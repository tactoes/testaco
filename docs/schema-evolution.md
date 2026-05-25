# Schema Evolution Guide

When your database schema changes, your test datasets must change with it. Testaco provides Gradle tasks that automate this — updating both the schema file and every dataset file in one step.

This guide walks through the common scenarios: adding tables, removing tables, adding columns, and removing columns.

## How It Works

Testaco validates your schema file (`testaco-schema.json`) against the live database at startup. If they don't match, your tests fail immediately with a clear diff:

```
SchemaValidationException: Database schema does not match testaco-schema.json

  public.users: missing columns: [phone]
  public.users.email: expected text(nullable), found varchar(255)(not null)
```

This means you can't just change your database — you must also update your schema file and datasets. The Gradle tasks handle both sides for you.

## The Workflow

Every schema change follows the same pattern:

1. **Change the database** (migration, manual DDL, etc.)
2. **Run the matching Gradle task** to update schema + datasets
3. **Review the changes** in your diff tool
4. **Commit everything together**

```
Database migration   →   Gradle task   →   git diff   →   git commit
(adds phone column)      (updates         (review       (schema +
                          schema +         changes)       datasets +
                          datasets)                       migration)
```

---

## Adding a Column

Your DBA adds a `phone` column to the `users` table. Now `testaco-schema.json` is out of date and your tests won't start.

### Nullable column

```bash
./gradlew testacoAddColumn --table=users --column=phone --type=text
```

This does two things:

1. **Updates `testaco-schema.json`** — adds the column definition:
   ```json
   "phone": { "type": "text", "nullable": true }
   ```

2. **Updates every dataset file** that contains a `users` table — adds `"phone": null` to each row:
   ```json
   // Before
   { "id": 1, "name": "Alice" }

   // After
   { "id": 1, "name": "Alice", "phone": null }
   ```

Since the column is nullable, `null` is the safe default for existing datasets.

### Non-null column with a default

If the column is `NOT NULL`, you need a default value for existing dataset rows:

```bash
./gradlew testacoAddColumn \
  --table=users \
  --column=status \
  --type=text \
  --nullable=false \
  --default=active
```

Now every existing row gets `"status": "active"` instead of `null`:

```json
// Before
{ "id": 1, "name": "Alice" }

// After
{ "id": 1, "name": "Alice", "status": "active" }
```

### After running the task

Review the changes. Some tests may need test-specific values instead of the default:

```bash
git diff
```

You'll see every affected file. Most rows are fine with the default. For tests that specifically exercise the new column, edit those dataset files manually to set meaningful values.

Then commit everything together:

```bash
git add -A
git commit -m "schema: add phone column to users"
```

---

## Removing a Column

The `phone` column is being dropped. Run:

```bash
./gradlew testacoRemoveColumn --table=users --column=phone
```

This:

1. **Removes the column from `testaco-schema.json`**
2. **Removes `"phone"` from every row** in every dataset file that contains `users`

```json
// Before
{ "id": 1, "name": "Alice", "phone": "555-1234" }

// After
{ "id": 1, "name": "Alice" }
```

No manual cleanup needed — the task handles all files.

---

## Adding a Table

A new `addresses` table is introduced:

```bash
./gradlew testacoAddTable \
  --table=addresses \
  --columns=id:bigint,user_id:bigint,street:text,city:text
```

This **only updates `testaco-schema.json`** — it adds the table definition but does not touch dataset files. This is intentional: new tables start empty in existing tests, and you add data only in the tests that need it.

After running the task, create dataset files for tests that use the new table:

```json
// src/test/resources/com/example/AddressServiceTest/creates_address/setup.json
{
  "addresses": [
    { "id": 1, "user_id": 1, "street": "123 Main St", "city": "Springfield" }
  ]
}
```

If existing tests load data into tables with foreign keys pointing to `addresses`, you may need to add `addresses` data to their setup files too — or use composable datasets to share reference data:

```kotlin
db.load("shared/addresses", "com/example/SomeTest/test_name/setup")
```

---

## Removing a Table

The `addresses` table is being dropped:

```bash
./gradlew testacoRemoveTable --table=addresses
```

This:

1. **Removes the table from `testaco-schema.json`**
2. **Removes the `"addresses"` key** from every dataset file that references it

```json
// Before
{
  "users": [{ "id": 1, "name": "Alice" }],
  "addresses": [{ "id": 1, "user_id": 1, "street": "123 Main St" }]
}

// After
{
  "users": [{ "id": 1, "name": "Alice" }]
}
```

Dataset files that become empty (only contained the removed table) will still be valid JSON — `{}` — but you should delete them manually if they're no longer needed.

---

## Handling Multiple Changes at Once

Database migrations often involve several changes. Run the tasks in sequence:

```bash
# A migration that restructures user contact info
./gradlew testacoAddTable --table=contacts --columns=id:bigint,user_id:bigint,type:text,value:text
./gradlew testacoRemoveColumn --table=users --column=phone
./gradlew testacoRemoveColumn --table=users --column=email
./gradlew testacoAddColumn --table=users --column=primary_contact_id --type=bigint --nullable=true
```

Review the full diff, update test-specific values, and commit as a single change:

```bash
git diff
# ... review changes, edit test-specific datasets as needed ...
git add -A
git commit -m "schema: migrate contact info to contacts table"
```

---

## Renaming a Column

There is no dedicated rename task. A rename is an add + remove:

```bash
./gradlew testacoAddColumn --table=users --column=full_name --type=text --nullable=false --default=""
./gradlew testacoRemoveColumn --table=users --column=name
```

After running both tasks, you'll need to manually update dataset files to copy the old values into the new column name. The `--default=""` gives you a placeholder; replace it with the actual values in each dataset file.

---

## Changing a Column Type

There is no dedicated type-change task. Update the schema file manually:

1. Edit `testaco-schema.json` — change the column's `"type"` value
2. Review dataset files — ensure values are compatible with the new type
3. Commit

For example, changing `phone` from `text` to `varchar`:

```json
// Before
"phone": { "type": "text", "nullable": true }

// After
"phone": { "type": "varchar", "nullable": true }
```

Dataset values don't need to change for most type promotions (text → varchar, integer → bigint). For narrowing changes (text → integer), review each value.

---

## Ignored Columns and Evolution

If you add a column that should never appear in test datasets (audit columns, auto-generated timestamps), add it to `testaco-config.json` instead of updating every dataset:

```json
{
  "ignoredColumns": {
    "public.users": ["created_at", "updated_at"],
    "*": ["audit_modified_by"]
  }
}
```

Ignored columns are:
- **Excluded from schema validation** — mismatches won't fail startup
- **Excluded from dataset validation** — you don't need to include them in JSON files
- **Excluded from comparisons** — `assertMatches` won't check their values
- **Excluded from dumps** — `dump()` won't include them

So when you add a column like `updated_at`:

```bash
# Add to schema (for completeness)
./gradlew testacoAddColumn --table=users --column=updated_at --type="timestamp with time zone"

# Then add to ignored columns in config — no dataset changes needed
```

---

## Tips

- **Commit migrations and dataset changes together.** They're logically one change.
- **Use `git diff` after every Gradle task.** The tasks are mechanical — review their output before committing.
- **Use `--default` for non-null columns.** Forgetting it means every dataset row gets `null`, which will fail against a `NOT NULL` constraint.
- **Use composable datasets for shared reference data.** When a new table is needed by many tests, create a shared dataset file and load it with `db.load("shared/reference-data", "test-specific/setup")`.
- **Use ignored columns for audit fields.** Don't clutter every dataset file with `created_at` and `updated_at`.
- **Run your tests after schema changes.** The Gradle tasks update files mechanically. If a test's logic depends on the changed column, the test may need manual attention beyond what the task provides.
