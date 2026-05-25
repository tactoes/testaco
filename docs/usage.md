# Usage Guide

This guide covers the complete Testaco workflow with detailed examples.

## Table of Contents

- [Initialization](#initialization)
- [Loading Data](#loading-data)
- [Asserting Data](#asserting-data)
- [Dumping Data](#dumping-data)
- [Special Matchers](#special-matchers)
- [Ignored Columns](#ignored-columns)
- [Load Strategies](#load-strategies)
- [Complete Test Examples](#complete-test-examples)

## Initialization

### Basic Initialization

```kotlin
import org.testaco.Testaco
import org.postgresql.ds.PGSimpleDataSource

val dataSource = PGSimpleDataSource().apply {
    serverNames = arrayOf("localhost")
    databaseName = "test_db"
    user = "test_user"
    password = "test_pass"
}

val db = Testaco(dataSource)
```

### With Configuration

```kotlin
import org.testaco.Testaco
import org.testaco.TestacoConfig
import org.testaco.LoadStrategy
import java.time.Duration

val config = TestacoConfig(
    schemas = listOf("public", "audit"),
    ignoredColumns = mapOf(
        "*" to listOf("updated_at", "version"),
        "users" to listOf("password_hash", "last_login")
    ),
    defaultTimestampTolerance = Duration.ofSeconds(10),
    loadStrategy = LoadStrategy.CLEAN_INSERT
)

val db = Testaco(dataSource, config)
```

### Custom Schema Path

```kotlin
val db = Testaco(
    dataSource = dataSource,
    config = config,
    schemaResourcePath = "custom-schema.json"
)
```

## Loading Data

### Single Dataset

```kotlin
db.load("com/example/OrderServiceTest/setup")
```

This loads `src/test/resources/com/example/OrderServiceTest/setup.json`.

### Composable Datasets

Load multiple datasets in sequence. Later datasets override earlier ones for the same table rows:

```kotlin
// Load shared reference data first, then test-specific data
db.load(
    "shared/reference-data",
    "com/example/OrderServiceTest/creates_order/setup"
)
```

**Example:**

**`shared/reference-data.json`:**
```json
{
  "users": [
    {"id": 1, "email": "admin@example.com", "role": "admin"},
    {"id": 2, "email": "user@example.com", "role": "user"}
  ],
  "products": [
    {"id": 100, "name": "Widget", "price": 9.99}
  ]
}
```

**`creates_order/setup.json`:**
```json
{
  "orders": [
    {"id": 1, "user_id": 1, "status": "pending"}
  ]
}
```

The final database state includes users, products, and orders.

### Load Strategy Override

Override the default load strategy per call:

```kotlin
import org.testaco.LoadStrategy

// Truncate tables before inserting (default)
db.load("setup", strategy = LoadStrategy.CLEAN_INSERT)

// Insert without truncating (useful for additive tests)
db.load("additional-data", strategy = LoadStrategy.INSERT)
```

## Asserting Data

### Basic Assertion

```kotlin
db.assertMatches("com/example/OrderServiceTest/expected")
```

Compares the current database state against the expected dataset. If there's a mismatch, Testaco:
1. Fails the test with a detailed diff
2. Dumps the actual database state to `build/test-results/testaco/<test-path>/actual.json`

### Assertion Behavior

- Tables not listed in the expected dataset are ignored
- Columns configured as ignored (via `ignoredColumns`) are not compared
- Row order doesn't matter (comparison is set-based)
- All special matchers are evaluated

## Dumping Data

### Dump to File

```kotlin
// Dump current database state to a file
db.dump("debug/actual-state")
```

This creates `src/test/resources/debug/actual-state.json` with the current database state.

### Dump to Memory

```kotlin
// Get current database state as a map
val state: Map<String, List<Map<String, Any?>>> = db.dump()

println(state["users"])
// [{"id": 1, "email": "test@example.com", "created_at": "2024-01-01T00:00:00Z"}]
```

Useful for debugging or programmatic assertions.

## Special Matchers

Testaco provides special matchers for flexible assertions in expected datasets.

### ~now

Matches timestamps within the default tolerance (5 seconds by default):

```json
{
  "orders": [
    {"id": 1, "created_at": "~now"}
  ]
}
```

**Use case:** Verifying that a timestamp was set to the current time.

### ~now±PT10S

Matches timestamps within a specific tolerance (ISO-8601 duration):

```json
{
  "orders": [
    {"id": 1, "created_at": "~now±PT10S"}
  ]
}
```

**Examples:**
- `~now±PT5S` — 5 seconds
- `~now±PT1M` — 1 minute
- `~now±PT30S` — 30 seconds

### ~ignore

Ignores the column value in assertions:

```json
{
  "users": [
    {"id": 1, "email": "test@example.com", "password_hash": "~ignore"}
  ]
}
```

**Use case:** When a column value is unpredictable or not relevant to the test.

### ~regex:pattern

Matches the value against a regex pattern:

```json
{
  "orders": [
    {"id": 1, "order_number": "~regex:^ORD-\\d{6}$"}
  ]
}
```

**Examples:**
- `~regex:^[A-Z]{3}-\\d{3}$` — Matches "ABC-123"
- `~regex:\\d{4}-\\d{2}-\\d{2}` — Matches date format "2024-01-15"
- `~regex:^\\w+@\\w+\\.\\w+$` — Matches email format

**Note:** Backslashes must be escaped in JSON (`\\d`, `\\w`, etc.).

## Ignored Columns

### Per-Table Ignored Columns

Ignore specific columns for specific tables:

```kotlin
val config = TestacoConfig(
    ignoredColumns = mapOf(
        "users" to listOf("password_hash", "last_login"),
        "orders" to listOf("updated_at")
    )
)
```

Or in `testaco-config.json`:

```json
{
  "ignoredColumns": {
    "users": ["password_hash", "last_login"],
    "orders": ["updated_at"]
  }
}
```

### Wildcard Ignored Columns

Ignore columns across all tables:

```kotlin
val config = TestacoConfig(
    ignoredColumns = mapOf(
        "*" to listOf("updated_at", "version")
    )
)
```

Or in `testaco-config.json`:

```json
{
  "ignoredColumns": {
    "*": ["updated_at", "version"]
  }
}
```

### Combining Per-Table and Wildcard

```json
{
  "ignoredColumns": {
    "*": ["updated_at", "version"],
    "users": ["password_hash", "last_login"]
  }
}
```

This ignores `updated_at` and `version` on all tables, plus `password_hash` and `last_login` on the `users` table.

## Load Strategies

### CLEAN_INSERT (Default)

Truncates tables before inserting data. Ensures a clean slate for each test.

```kotlin
db.load("setup", strategy = LoadStrategy.CLEAN_INSERT)
```

**Behavior:**
1. Truncate all tables in the dataset
2. Insert rows from the dataset

**Use case:** Most tests (default behavior).

### INSERT

Inserts data without truncating. Useful for additive tests.

```kotlin
db.load("setup", strategy = LoadStrategy.INSERT)
```

**Behavior:**
1. Insert rows from the dataset
2. Fails if rows with duplicate primary keys exist

**Use case:** Adding data to an existing dataset without clearing it.

### Example: Combining Strategies

```kotlin
// Setup: Load base data with CLEAN_INSERT
db.load("shared/base-data", strategy = LoadStrategy.CLEAN_INSERT)

// Add additional data without clearing
db.load("test-specific/extra-data", strategy = LoadStrategy.INSERT)
```

## Complete Test Examples

### Example 1: Basic CRUD Test

```kotlin
import org.testaco.Testaco
import io.kotest.core.spec.style.FunSpec
import org.postgresql.ds.PGSimpleDataSource

class UserServiceTest : FunSpec({
    val dataSource = PGSimpleDataSource().apply {
        serverNames = arrayOf("localhost")
        databaseName = "test_db"
        user = "test_user"
        password = "test_pass"
    }

    val db = Testaco(dataSource)

    test("creates user successfully") {
        db.load("com/example/UserServiceTest/creates_user/setup")

        val service = UserService(dataSource)
        val userId = service.createUser(email = "newuser@example.com", role = "user")

        db.assertMatches("com/example/UserServiceTest/creates_user/expected")
    }

    test("updates user email") {
        db.load("com/example/UserServiceTest/updates_email/setup")

        val service = UserService(dataSource)
        service.updateEmail(userId = 1, newEmail = "updated@example.com")

        db.assertMatches("com/example/UserServiceTest/updates_email/expected")
    }

    test("deletes user") {
        db.load("com/example/UserServiceTest/deletes_user/setup")

        val service = UserService(dataSource)
        service.deleteUser(userId = 1)

        db.assertMatches("com/example/UserServiceTest/deletes_user/expected")
    }
})
```

**`setup.json`:**
```json
{
  "users": [
    {"id": 1, "email": "test@example.com", "role": "user", "created_at": "2024-01-01T00:00:00Z"}
  ]
}
```

**`expected.json`:**
```json
{
  "users": []
}
```

### Example 2: Composable Datasets

```kotlin
class OrderServiceTest : FunSpec({
    val db = Testaco(dataSource)

    test("processes order with multiple items") {
        db.load(
            "shared/users",           // Load users
            "shared/products",        // Load products
            "OrderServiceTest/setup"  // Load test-specific orders
        )

        val service = OrderService(dataSource)
        service.processOrder(orderId = 1)

        db.assertMatches("OrderServiceTest/expected")
    }
})
```

### Example 3: Using Special Matchers

```kotlin
class NotificationServiceTest : FunSpec({
    val db = Testaco(dataSource)

    test("sends notification") {
        db.load("NotificationServiceTest/setup")

        val service = NotificationService(dataSource)
        service.sendNotification(userId = 1, message = "Hello!")

        db.assertMatches("NotificationServiceTest/expected")
    }
})
```

**`expected.json`:**
```json
{
  "notifications": [
    {
      "id": 1,
      "user_id": 1,
      "message": "Hello!",
      "status": "sent",
      "sent_at": "~now±PT10S",
      "notification_id": "~regex:^[a-f0-9-]{36}$"
    }
  ]
}
```

### Example 4: Ignored Columns

```kotlin
class AuditServiceTest : FunSpec({
    val config = TestacoConfig(
        ignoredColumns = mapOf(
            "*" to listOf("updated_at"),
            "users" to listOf("last_login")
        )
    )
    val db = Testaco(dataSource, config)

    test("updates user profile") {
        db.load("AuditServiceTest/setup")

        val service = UserService(dataSource)
        service.updateProfile(userId = 1, name = "New Name")

        db.assertMatches("AuditServiceTest/expected")
    }
})
```

**`expected.json`:**
```json
{
  "users": [
    {
      "id": 1,
      "email": "test@example.com",
      "name": "New Name",
      "created_at": "~ignore"
    }
  ]
}
```

Note: `updated_at` and `last_login` are automatically ignored due to configuration.

### Example 5: Dump on Failure

When an assertion fails, Testaco automatically dumps the actual database state:

```kotlin
test("creates order") {
    db.load("OrderServiceTest/setup")

    val service = OrderService(dataSource)
    service.createOrder(userId = 1)

    db.assertMatches("OrderServiceTest/expected")
    // If this fails, actual state is dumped to:
    // build/test-results/testaco/OrderServiceTest/actual.json
}
```

You can then compare the expected and actual files to understand the mismatch:

```bash
diff src/test/resources/OrderServiceTest/expected.json \
     build/test-results/testaco/OrderServiceTest/actual.json
```

### Example 6: INSERT Load Strategy

```kotlin
class EventLogTest : FunSpec({
    val db = Testaco(dataSource)

    test("appends event log entries") {
        // Load initial events with CLEAN_INSERT
        db.load("EventLogTest/initial-events", strategy = LoadStrategy.CLEAN_INSERT)

        // Add more events without clearing
        db.load("EventLogTest/additional-events", strategy = LoadStrategy.INSERT)

        val service = EventLogService(dataSource)
        service.logEvent("user_login", userId = 1)

        db.assertMatches("EventLogTest/expected")
    }
})
```

## Best Practices

### 1. Organize Datasets by Test Class and Method

```
src/test/resources/
└── com/example/OrderServiceTest/
    ├── creates_order/
    │   ├── setup.json
    │   └── expected.json
    ├── cancels_order/
    │   ├── setup.json
    │   └── expected.json
    └── processes_order/
        ├── setup.json
        └── expected.json
```

### 2. Use Composable Datasets for Shared Data

Create shared datasets for reference data:

```
src/test/resources/
├── shared/
│   ├── users.json
│   ├── products.json
│   └── categories.json
└── com/example/OrderServiceTest/
    └── creates_order/
        ├── setup.json
        └── expected.json
```

### 3. Use Special Matchers for Non-Deterministic Values

- Use `~now` for timestamps
- Use `~regex:` for generated IDs
- Use `~ignore` for irrelevant columns

### 4. Configure Ignored Columns Globally

Use wildcards for common columns like `updated_at`:

```json
{
  "ignoredColumns": {
    "*": ["updated_at", "version"]
  }
}
```

### 5. Use Dump for Debugging

When a test fails, examine the dumped actual state:

```bash
cat build/test-results/testaco/<test-path>/actual.json
```

Or dump explicitly during development:

```kotlin
test("debugging test") {
    db.load("setup")
    
    // ... test logic ...
    
    db.dump("debug/actual-state")
}
```
