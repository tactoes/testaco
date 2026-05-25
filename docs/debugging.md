# Debugging Failing Tests

When `assertMatches` fails, Testaco gives you two things: a structured error message in the test output, and an auto-dumped JSON file with the actual database state. This guide explains how to use both to diagnose and fix the problem.

## What Happens When a Test Fails

When `db.assertMatches("com/example/OrderServiceTest/creates_order/expected")` fails, Testaco:

1. **Prints a structured error message** showing exactly what's different
2. **Dumps the actual database state** to `build/test-results/testaco/<test-path>/actual.json`

The error message looks like this:

```
Dataset mismatch

Table 'public.orders':
  Row (PK: {id=1}):
    status: expected pending, got confirmed
    total: expected 29.99, got 39.99
  Missing rows: 1
  Extra rows: 2

Actual database state dumped to: build/test-results/testaco/com/example/OrderServiceTest/creates_order/actual.json
```

## Reading the Error Message

The error message has three sections per table:

### Column Mismatches

```
Row (PK: {id=1}):
  status: expected pending, got confirmed
  total: expected 29.99, got 39.99
```

A row was found by primary key, but some column values differ. The message shows the expected value (from your dataset file) and the actual value (from the database).

### Missing Rows

```
Missing rows: 1
```

Rows that exist in your expected dataset but not in the database. Your code under test didn't insert them, or deleted them when it shouldn't have.

### Extra Rows

```
Extra rows: 2
```

Rows that exist in the database but not in your expected dataset. Your code under test inserted rows you didn't anticipate, or your expected dataset is incomplete.

## Inspecting the Dump File

The error message tells you *what* is different. The dump file tells you *what the database actually contains*. Open it to see the full picture:

```bash
cat build/test-results/testaco/com/example/OrderServiceTest/creates_order/actual.json
```

The file has the same format as your dataset files:

```json
{
  "public.orders": [
    {
      "id": 1,
      "customer_id": 42,
      "status": "confirmed",
      "total": 39.99,
      "created_at": "2026-05-11T12:00:00Z"
    },
    {
      "id": 2,
      "customer_id": 42,
      "status": "pending",
      "total": 15.00,
      "created_at": "2026-05-11T12:00:01Z"
    },
    {
      "id": 3,
      "customer_id": 99,
      "status": "shipped",
      "total": 120.00,
      "created_at": "2026-05-11T12:00:02Z"
    }
  ]
}
```

## Comparing Expected vs Actual

The most effective debugging technique is a side-by-side diff of your expected dataset against the dump:

```bash
diff \
  src/test/resources/com/example/OrderServiceTest/creates_order/expected.json \
  build/test-results/testaco/com/example/OrderServiceTest/creates_order/actual.json
```

Or with a visual diff tool:

```bash
# VS Code
code --diff \
  src/test/resources/com/example/OrderServiceTest/creates_order/expected.json \
  build/test-results/testaco/com/example/OrderServiceTest/creates_order/actual.json

# IntelliJ (from command line)
idea diff \
  src/test/resources/com/example/OrderServiceTest/creates_order/expected.json \
  build/test-results/testaco/com/example/OrderServiceTest/creates_order/actual.json
```

The diff shows you exactly which rows and values are different, making it clear whether the problem is in your code or your expected data.

## Common Scenarios

### Wrong column value

**Error:**
```
Row (PK: {id=1}):
  status: expected pending, got confirmed
```

**What to check:**
- Is your code under test changing the status? Look at the business logic.
- Is your expected dataset wrong? Maybe the operation *should* set it to `confirmed`.
- Did the setup data already have the wrong value? Check `setup.json`.

**Fix:** Either fix your code or update `expected.json` with the correct value.

### Extra rows in the database

**Error:**
```
Extra rows: 2
```

**What to check:**
- Open the dump file to see what those extra rows look like.
- Did your code insert more rows than expected (e.g., audit log entries, history records)?
- Did the setup data include rows you forgot to account for in the expected data?

**Fix:** Either add the extra rows to `expected.json`, fix your code, or use `~ignore` matchers if the rows have unpredictable values.

### Missing rows

**Error:**
```
Missing rows: 1
```

**What to check:**
- Did your code fail to insert a row? Check for swallowed exceptions.
- Did your code delete a row it shouldn't have?
- Is the expected dataset expecting rows that your code doesn't create?

**Fix:** Fix your code to produce the expected rows, or remove them from `expected.json`.

### Timestamp too far from now

**Error:**
```
Row (PK: {id=1}):
  created_at: expected ~now (±PT5S), got 2026-05-11T11:59:50Z
```

**What to check:**
- Is the tolerance too tight? On a slow CI server, 5 seconds may not be enough.
- Is the timestamp being set to the wrong time (e.g., using a cached time instead of `now()`)?

**Fix:** Increase the tolerance (`~now±PT30S`) or fix the timestamp logic. You can also set the default tolerance in `testaco-config.json`:

```json
{
  "defaultTimestampTolerance": "PT30S"
}
```

### Null vs non-null mismatch

**Error:**
```
Row (PK: {id=1}):
  phone: expected null, got 555-1234
```

**What to check:**
- Did your code set a value that you expected to stay null?
- Was the setup data different from what you assumed?

### Type mismatch (common gotcha)

**Error:**
```
Row (PK: {id=1}):
  amount: expected 100, got 100.00
```

Testaco handles numeric comparisons via `BigDecimal`, so `100` and `100.00` are considered equal. If you see this kind of error, the types may be fundamentally different (string vs number). Check that your JSON dataset uses the right JSON type:

```json
// Wrong — amount is a string
{ "amount": "100" }

// Right — amount is a number
{ "amount": 100 }
```

## Using Dump for Development

When writing a new test, you may not know what the expected output should be. Use `dump` to capture it:

```kotlin
"creates an order" {
    db.load("com/example/OrderServiceTest/creates_order/setup")

    orderService.createOrder(customerId = 42, amount = 29.99)

    // During development: dump to see the actual state
    db.dump("com/example/OrderServiceTest/creates_order/expected")

    // Later: replace with assertion
    // db.assertMatches("com/example/OrderServiceTest/creates_order/expected")
}
```

Run the test once with `dump`, inspect the generated file, clean up any columns you want to ignore or match with `~now`, then switch to `assertMatches`.

## Using In-Memory Dump for Programmatic Debugging

For more complex debugging, dump to memory and inspect programmatically:

```kotlin
"debugging a complex failure" {
    db.load("com/example/ComplexTest/setup")

    complexService.process()

    val state = db.dump()
    // state is Map<String, List<Map<String, Any?>>>

    // Check specific table
    val orders = state["public.orders"] ?: emptyList()
    println("Order count: ${orders.size}")
    orders.forEach { row ->
        println("  id=${row["id"]}, status=${row["status"]}, total=${row["total"]}")
    }

    db.assertMatches("com/example/ComplexTest/expected")
}
```

## Tips

- **Always check the dump file first.** The error message tells you what's wrong, but the dump file tells you what's actually there. The difference helps you decide whether to fix the code or the expected data.
- **Use `diff` tools.** Side-by-side comparison is far more effective than reading the dump manually, especially for tables with many columns.
- **Use `~ignore` for unpredictable columns.** If a column has values you can't predict (UUIDs, random tokens), use `"~ignore"` in the expected dataset instead of trying to match them.
- **Use `~now` for timestamps.** Don't hardcode timestamp values. Use `"~now"` or `"~now±PT30S"` to assert that a timestamp is close to the current time.
- **Use `~regex:` for pattern matching.** For values that follow a pattern but aren't exact (e.g., `"~regex:ORD-\\d+"`).
- **Check your setup data.** Many assertion failures are caused by the setup data being wrong, not the code under test. Review `setup.json` alongside `expected.json`.
- **Each failing test gets its own dump file.** The dump path is derived from the resource path, so multiple failing tests don't overwrite each other. After a test run, check `build/test-results/testaco/` to see all failures.
