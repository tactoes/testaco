# Schema Reference

This guide covers the PgFixtures schema file format, validation behavior, and error handling.

## Table of Contents

- [Schema File Format](#schema-file-format)
- [Validation Behavior](#validation-behavior)
- [Error Messages](#error-messages)
- [Bootstrapping from Existing Database](#bootstrapping-from-existing-database)

## Schema File Format

The schema file is a JSON file that defines the structure of your database tables. By default, it's located at `src/test/resources/testaco-schema.json`.

### Basic Structure

```json
{
  "table_name": {
    "column_name": "postgresql_type"
  }
}
```

### Example Schema

```json
{
  "users": {
    "id": "bigint",
    "email": "text",
    "role": "text",
    "created_at": "timestamp with time zone"
  },
  "orders": {
    "id": "bigint",
    "user_id": "bigint",
    "status": "text",
    "total": "numeric",
    "created_at": "timestamp with time zone"
  },
  "order_items": {
    "id": "bigint",
    "order_id": "bigint",
    "product_id": "bigint",
    "quantity": "integer",
    "price": "numeric"
  }
}
```

### Supported PostgreSQL Types

PgFixtures supports all PostgreSQL types. Common types include:

| Type | Description | Example Values |
|------|-------------|----------------|
| `text` | Variable-length text | `"hello"`, `""` |
| `varchar` | Variable-length text with limit | `"hello"` |
| `bigint` | 8-byte integer | `1`, `-1000`, `9223372036854775807` |
| `integer` | 4-byte integer | `1`, `-1000`, `2147483647` |
| `smallint` | 2-byte integer | `1`, `-1000`, `32767` |
| `numeric` | Exact numeric | `9.99`, `100.50` |
| `decimal` | Exact numeric (alias for numeric) | `9.99`, `100.50` |
| `boolean` | Boolean | `true`, `false` |
| `timestamp` | Timestamp without timezone | `"2024-01-01T00:00:00"` |
| `timestamp with time zone` | Timestamp with timezone | `"2024-01-01T00:00:00Z"` |
| `date` | Date | `"2024-01-01"` |
| `time` | Time | `"12:00:00"` |
| `jsonb` | Binary JSON | `{"key": "value"}` |
| `json` | Text JSON | `{"key": "value"}` |
| `uuid` | UUID | `"550e8400-e29b-41d4-a716-446655440000"` |

See [PostgreSQL Data Types](https://www.postgresql.org/docs/current/datatype.html) for the complete list.

### Multi-Schema Support

By default, PgFixtures uses the `public` schema. To work with multiple schemas, configure them in `testaco-config.json`:

```json
{
  "schemas": ["public", "audit", "reporting"]
}
```

Then reference tables in the schema file with or without the schema prefix:

```json
{
  "users": {
    "id": "bigint",
    "email": "text"
  },
  "audit.logs": {
    "id": "bigint",
    "action": "text",
    "created_at": "timestamp with time zone"
  }
}
```

If no schema prefix is specified, PgFixtures will search for the table in all configured schemas.

## Validation Behavior

PgFixtures performs **bidirectional validation** at construction time:

1. **Schema file → Live database:** Verifies that all tables and columns in the schema file exist in the database with matching types
2. **Live database → Schema file:** Verifies that all tables and columns in the database (for configured schemas) exist in the schema file

### When Validation Occurs

Validation happens when you create a `PgFixtures` instance:

```kotlin
val db = PgFixtures(dataSource) // Validation happens here
```

### What Gets Validated

#### 1. Tables

- All tables in the schema file must exist in the database
- All tables in the database (for configured schemas) must exist in the schema file

#### 2. Columns

- All columns in the schema file must exist in the corresponding database table
- All columns in the database table must exist in the schema file

#### 3. Types

- Column types in the schema file must match the database
- Type matching is exact (e.g., `text` != `varchar`, `bigint` != `integer`)

### Dataset Validation

Datasets are validated against the schema file on `load()` and `assertMatches()`:

- All tables in the dataset must exist in the schema file
- All columns in the dataset must exist in the schema file
- Column values must be compatible with the schema type (e.g., can't insert `"text"` into an `integer` column)

## Error Messages

PgFixtures provides detailed error messages with "did you mean?" suggestions for typos.

### Schema Validation Errors

#### Missing Table in Database

```
Schema validation failed: Table 'users' is defined in schema file but does not exist in database.

Did you mean one of these?
  - user_profiles (similarity: 0.75)
  - user_roles (similarity: 0.65)
```

#### Missing Column in Database

```
Schema validation failed: Column 'email' in table 'users' is defined in schema file but does not exist in database.

Did you mean one of these?
  - email_address (similarity: 0.80)
  - user_email (similarity: 0.70)
```

#### Type Mismatch

```
Schema validation failed: Column 'id' in table 'users' has type 'integer' in schema file but type 'bigint' in database.
```

#### Extra Table in Database

```
Schema validation failed: Table 'user_sessions' exists in database (schema 'public') but is not defined in schema file.
```

#### Extra Column in Database

```
Schema validation failed: Column 'phone' in table 'users' exists in database but is not defined in schema file.
```

### Dataset Validation Errors

#### Missing Table in Schema

```
Dataset validation failed: Table 'products' in dataset 'setup.json' is not defined in schema file.

Did you mean one of these?
  - product_categories (similarity: 0.75)
  - product_reviews (similarity: 0.70)
```

#### Missing Column in Schema

```
Dataset validation failed: Column 'name' in table 'users' in dataset 'setup.json' is not defined in schema file.

Did you mean one of these?
  - full_name (similarity: 0.80)
  - username (similarity: 0.70)
```

#### Type Mismatch

```
Dataset validation failed: Column 'id' in table 'users' in dataset 'setup.json' has value '1' (type: integer) but schema expects 'bigint'.
```

### Assertion Errors

When `assertMatches()` fails, PgFixtures:

1. Prints a detailed diff showing:
   - Missing rows (in expected but not actual)
   - Extra rows (in actual but not expected)
   - Mismatched values (different values for the same row)
2. Dumps the actual database state to `build/test-results/testaco/<test-path>/actual.json`

**Example:**

```
Assertion failed for table 'orders':

Missing rows (expected but not in actual):
  - {"id": 2, "user_id": 1, "status": "pending", "created_at": "2024-01-02T00:00:00Z"}

Extra rows (in actual but not expected):
  - {"id": 3, "user_id": 2, "status": "shipped", "created_at": "2024-01-03T00:00:00Z"}

Mismatched values:
  Row {"id": 1}:
    - status: expected "pending", actual "completed"

Actual database state dumped to: build/test-results/testaco/orders/actual.json
```

## Bootstrapping from Existing Database

If you have an existing database and want to create a schema file, you can use the `dump()` method to generate a schema from the live database.

### Step 1: Create a Temporary Script

Create a Kotlin script or test to dump the schema:

```kotlin
import org.testaco.PgFixtures
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.postgresql.ds.PGSimpleDataSource
import java.io.File

fun main() {
    val dataSource = PGSimpleDataSource().apply {
        serverNames = arrayOf("localhost")
        databaseName = "your_db"
        user = "your_user"
        password = "your_password"
    }

    // This will fail with a schema validation error, but will print the database schema
    try {
        val db = PgFixtures(dataSource, schemaResourcePath = "empty-schema.json")
    } catch (e: Exception) {
        println("Expected error (no schema file yet):")
        println(e.message)
    }

    // Alternatively, query the database directly to generate the schema
    val schema = mutableMapOf<String, MutableMap<String, String>>()
    
    dataSource.connection.use { conn ->
        val rs = conn.prepareStatement("""
            SELECT 
                table_name,
                column_name,
                data_type
            FROM information_schema.columns
            WHERE table_schema = 'public'
            ORDER BY table_name, ordinal_position
        """).executeQuery()
        
        while (rs.next()) {
            val table = rs.getString("table_name")
            val column = rs.getString("column_name")
            val type = rs.getString("data_type")
            
            schema.getOrPut(table) { mutableMapOf() }[column] = type
        }
    }
    
    val mapper = jacksonObjectMapper()
    val schemaJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    
    File("src/test/resources/testaco-schema.json").writeText(schemaJson)
    println("Schema written to src/test/resources/testaco-schema.json")
}
```

### Step 2: Manually Create Schema File

Alternatively, manually create the schema file by querying your database:

```sql
-- Get all tables and columns in the public schema
SELECT 
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_schema = 'public'
ORDER BY table_name, ordinal_position;
```

Then construct the JSON manually:

```json
{
  "table1": {
    "column1": "type1",
    "column2": "type2"
  },
  "table2": {
    "column1": "type1",
    "column2": "type2"
  }
}
```

### Step 3: Verify Schema

Create a `PgFixtures` instance to verify the schema:

```kotlin
val db = PgFixtures(dataSource)
// If no exception is thrown, schema is valid
```

### Step 4: Create Initial Datasets

Use `dump()` to create initial dataset files:

```kotlin
db.dump("initial-state")
```

This creates `src/test/resources/initial-state.json` with the current database state.

## Best Practices

### 1. Keep Schema File in Sync with Database

Always update the schema file when you modify the database schema:

```bash
# After running a migration
./gradlew testacoAddColumn --table=users --column=phone --type=text
```

### 2. Use Gradle Tasks for Schema Changes

Use the provided Gradle tasks to ensure schema and dataset files stay in sync:

```bash
./gradlew testacoAddColumn --table=users --column=phone --type=text
./gradlew testacoRemoveColumn --table=users --column=old_column
```

### 3. Validate Schema Early

Create the `PgFixtures` instance early in your test to catch schema mismatches before running test logic:

```kotlin
class UserServiceTest : FunSpec({
    val db = PgFixtures(dataSource) // Validates schema immediately
    
    test("creates user") {
        db.load("setup")
        // ... test logic ...
    }
})
```

### 4. Use Exact Type Matching

Ensure types in the schema file exactly match the database:

- Use `bigint` for `BIGINT`, not `integer`
- Use `text` for `TEXT`, not `varchar`
- Use `timestamp with time zone` for `TIMESTAMPTZ`, not `timestamp`

### 5. Handle Multi-Schema Databases

If your database uses multiple schemas, configure them explicitly:

```json
{
  "schemas": ["public", "audit", "reporting"]
}
```

And reference tables with schema prefixes if needed:

```json
{
  "users": {...},
  "audit.logs": {...}
}
```

## Troubleshooting

### Validation Fails on Startup

If validation fails when creating a `PgFixtures` instance:

1. Read the error message carefully (it shows exactly what's wrong)
2. Check if you have a typo (use the "did you mean?" suggestions)
3. Verify your database schema matches the schema file
4. Ensure all tables/columns exist in both the schema file and database

### Type Mismatch Errors

If you get type mismatch errors:

1. Check the actual type in the database:
   ```sql
   SELECT column_name, data_type 
   FROM information_schema.columns 
   WHERE table_name = 'your_table';
   ```
2. Update the schema file to match exactly

### Extra Tables/Columns in Database

If the database has tables/columns not in the schema file:

1. Add them to the schema file if they should be managed by PgFixtures
2. Exclude the schema if the tables shouldn't be managed:
   ```json
   {
     "schemas": ["public"]
   }
   ```

### Schema File Not Found

If you get a "schema file not found" error:

1. Ensure the file exists at `src/test/resources/testaco-schema.json`
2. If using a custom path, pass it to the constructor:
   ```kotlin
   val db = PgFixtures(dataSource, schemaResourcePath = "custom-schema.json")
   ```
