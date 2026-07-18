# DbMangler — Design Specification

A Kotlin reimplementation of DbUnit for Kotest, using JSON datasets and PostgreSQL only.

## Overview

DbMangler is a test data management library for JVM projects using PostgreSQL. It replaces XML-based DbUnit with a modern, Kotlin-idiomatic approach using JSON datasets. The library is a standalone utility class (no framework coupling) that handles loading test data, asserting database state, and dumping actual state on failure. A companion Gradle plugin provides tasks for schema evolution across dataset files.

## Architecture

Two Gradle modules:

- **`dbmangler-core`** — the runtime library (loaded in tests)
- **`dbmangler-gradle-plugin`** — Gradle tasks for schema/dataset file management

### Module: `dbmangler-core`

```
src/main/kotlin/com/dbmangler/
├── DbMangler.kt              # Main entry point (standalone utility)
├── config/
│   └── DbManglerConfig.kt    # Configuration (ignored columns, schemas, tolerances)
├── schema/
│   ├── Schema.kt             # Schema model (tables, columns, types)
│   ├── SchemaReader.kt       # Reads dbmangler-schema.json from resources
│   └── SchemaValidator.kt    # Validates schema file ↔ live DB, datasets ↔ schema
├── dataset/
│   ├── DataSet.kt            # Dataset model
│   ├── DataSetReader.kt      # Parses JSON dataset files
│   └── DataSetWriter.kt      # Dumps DB state to JSON
├── operation/
│   ├── LoadOperation.kt      # CLEAN_INSERT / INSERT
│   ├── CompareOperation.kt   # Assert DB matches expected dataset
│   └── DumpOperation.kt      # Dump current DB to file
├── assertion/
│   ├── ColumnMatcher.kt      # Exact, near-now, regex, ignore matchers
│   └── ComparisonResult.kt   # Diff representation
└── util/
    ├── ForeignKeyResolver.kt # Topological sort for FK dependencies
    └── PostgresIntrospector.kt # Queries information_schema
```

### Module: `dbmangler-gradle-plugin`

```
src/main/kotlin/com/dbmangler/gradle/
├── DbManglerPlugin.kt
├── AddColumnTask.kt
├── RemoveColumnTask.kt
├── AddTableTask.kt
└── RemoveTableTask.kt
```

## Consumer Project File Layout

Test resources in the consuming project:

```
src/test/resources/
├── dbmangler-schema.json             # Schema definition
├── dbmangler-config.json             # Global config (ignored columns, etc.)
└── com/example/
    └── OrderServiceTest/
        ├── creates_order/
        │   ├── setup.json            # Loaded before test
        │   └── expected.json         # Compared after test
        └── cancels_order/
            ├── setup.json
            └── expected.json
```

Dataset paths in code are relative to `src/test/resources/` and omit the `.json` extension.

## JSON Dataset Format

Object-per-table format. Each top-level key is a fully-qualified table name (or unqualified, defaulting to the first configured schema). Values are arrays of row objects:

```json
{
  "users": [
    { "id": 1, "name": "Alice", "email": "alice@example.com" },
    { "id": 2, "name": "Bob", "email": null }
  ],
  "orders": [
    { "id": 10, "user_id": 1, "amount": 99.99 }
  ]
}
```

### Special Value Matchers (in expected datasets only)

| Matcher | Meaning |
|---------|---------|
| `"~now"` | Value is within `defaultTimestampTolerance` (5s) of current time |
| `"~now±PT10S"` | Value is within custom tolerance of current time |
| `"~ignore"` | Skip this column for this row only |
| `"~regex:pattern"` | Value matches the given regex |

Exact values are used for all other comparisons.

## Schema File

`dbmangler-schema.json` captures the known database structure:

```json
{
  "schemas": ["public"],
  "tables": {
    "public.users": {
      "columns": {
        "id": { "type": "bigint", "nullable": false },
        "name": { "type": "text", "nullable": false },
        "email": { "type": "text", "nullable": true },
        "created_at": { "type": "timestamp with time zone", "nullable": false }
      },
      "primaryKey": ["id"]
    },
    "public.orders": {
      "columns": {
        "id": { "type": "bigint", "nullable": false },
        "user_id": { "type": "bigint", "nullable": false },
        "amount": { "type": "numeric", "nullable": false }
      },
      "primaryKey": ["id"],
      "foreignKeys": [
        { "columns": ["user_id"], "references": "public.users", "referencedColumns": ["id"] }
      ]
    }
  }
}
```

## Schema Validation

Validation runs at `DbMangler` construction time:

1. **Schema file ↔ Live DB:** Introspect `information_schema` for configured schemas. Compare tables, columns, types, nullability, primary keys, foreign keys. Fail with a detailed diff on mismatch.
2. **Dataset files ↔ Schema file:** Every column referenced in dataset JSON must exist in the schema. Every table in a dataset must exist in the schema. Extra DB columns not in the dataset are allowed (you only assert what you declare).

Ignored columns (from config) are excluded from both validations.

## Configuration

### File-based (`dbmangler-config.json`)

```json
{
  "schemas": ["public"],
  "ignoredColumns": {
    "public.users": ["created_at", "updated_at"],
    "*": ["audit_modified_by"]
  },
  "defaultTimestampTolerance": "PT5S",
  "loadStrategy": "CLEAN_INSERT"
}
```

### Programmatic (takes precedence over file-based)

```kotlin
val db = DbMangler(
    dataSource = testDataSource,
    config = DbManglerConfig(
        schemas = listOf("public"),
        ignoredColumns = mapOf("*" to listOf("audit_modified_by")),
        defaultTimestampTolerance = Duration.ofSeconds(5)
    )
)
```

### Configuration Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `schemas` | `List<String>` | `["public"]` | PostgreSQL schemas to manage |
| `ignoredColumns` | `Map<String, List<String>>` | `{}` | Columns to ignore globally. `"*"` applies to all tables. |
| `defaultTimestampTolerance` | ISO 8601 duration | `PT5S` | Default tolerance for `~now` assertions |
| `loadStrategy` | `CLEAN_INSERT` or `INSERT` | `CLEAN_INSERT` | Default load strategy |

## Core API

```kotlin
class DbMangler(dataSource: DataSource, config: DbManglerConfig = DbManglerConfig()) {

    // Construction validates schema file ↔ live DB.
    // Throws SchemaValidationException with detailed diff on mismatch.

    fun load(vararg resourcePaths: String, strategy: LoadStrategy = config.loadStrategy)
    // Loads one or more datasets, merging them in order (later datasets add to / override earlier ones).
    // Resolves FK order (with deferred constraints for cycles),
    // truncates tables (CLEAN_INSERT) or skips (INSERT), inserts rows.
    // Example: db.load("shared/reference-data", "com/example/OrderServiceTest/creates_order/setup")
    // This allows composing semi-static reference data with test-specific data.

    fun assertMatches(resourcePath: String)
    // Compares live DB state against expected dataset (only tables present in the expected file).
    // On failure: dumps actual state, throws DataSetMismatchException with diff + dump path.

    fun dump(resourcePath: String)
    // Dumps current DB state for ALL tables defined in the schema file to a JSON file at the given path.

    fun dump(): Map<String, List<Map<String, Any?>>>
    // Returns current DB state as in-memory structure.
}
```

### Load Operation

1. Parse all dataset JSON files and merge them: for each table, concatenate rows from all datasets in order
2. Validate columns against schema (fail fast on unknown column)
3. Resolve FK dependency order via topological sort
4. For cycles: `SET CONSTRAINTS ALL DEFERRED`, insert, then commit
5. CLEAN_INSERT: `TRUNCATE ... CASCADE` in reverse FK order, then insert in FK order
6. INSERT: insert in FK order, fail on conflicts

### Compare Operation

1. Parse expected dataset JSON
2. For each table, `SELECT *` ordered by PK
3. Match rows using column matchers (`~now`, `~ignore`, `~regex:...`, exact)
4. On mismatch: auto-dump actual state to `build/test-results/<testclass>/<testname>/dbmangler-actual.json`, throw `DataSetMismatchException`

### Dump Operation

1. For each table in schema (respecting ignored columns), `SELECT *` ordered by PK
2. Write object-per-table JSON to the specified path

## Gradle Plugin Tasks

### `dbmanglerAddColumn`

```
./gradlew dbmanglerAddColumn --table=users --column=phone --type=text --nullable=true
./gradlew dbmanglerAddColumn --table=users --column=status --type=text --nullable=false --default=active
```

- Adds column to `dbmangler-schema.json`
- Adds `"phone": null` (or the specified `--default` value) to every row in every dataset file containing the `users` table

### `dbmanglerRemoveColumn`

```
./gradlew dbmanglerRemoveColumn --table=users --column=phone
```

- Removes column from `dbmangler-schema.json`
- Removes `phone` key from every row in every dataset file containing `users`

### `dbmanglerAddTable`

```
./gradlew dbmanglerAddTable --table=addresses --columns='id:bigint,user_id:bigint,street:text'
```

- Adds table definition to `dbmangler-schema.json`
- Does not modify dataset files

### `dbmanglerRemoveTable`

```
./gradlew dbmanglerRemoveTable --table=addresses
```

- Removes table from `dbmangler-schema.json`
- Removes `addresses` key from every dataset file that references it

All tasks operate within a configurable resource directory (default `src/test/resources`).

## Error Handling

### Schema mismatch at startup

```
SchemaValidationException: Database schema does not match dbmangler-schema.json

  Missing tables: [public.audit_log]
  Extra tables: [public.temp_migration]
  Column mismatches:
    public.users.email: expected text(nullable), found varchar(255)(not null)
```

### Dataset mismatch on assertion

```
DataSetMismatchException: Dataset mismatch for table 'public.orders'

  Row 1 (PK: id=10):
    amount: expected 99.99, got 100.00
    created_at: expected ~now (±5s), got 2026-05-10T17:03:00Z (12s off)

  Actual database state dumped to: build/test-results/OrderServiceTest/creates_order/dbmangler-actual.json
```

### Dataset validation (fail-fast)

```
DataSetValidationException: Dataset 'setup.json' references unknown column 'users.phon'
  Did you mean 'users.phone'? Run: ./gradlew dbmanglerAddColumn --table=users --column=phon --type=text
```

## Documentation

The project includes:

- **`README.md`** — project overview, quick start, installation, configuration reference
- **`docs/usage.md`** — detailed usage guide with test examples, special matchers, ignored columns
- **`docs/gradle-tasks.md`** — reference for all four Gradle tasks
- **`docs/schema.md`** — schema file format, validation behavior, bootstrapping from existing DB

## Testing Strategy (for DbMangler itself)

- **Integration tests:** Testcontainers PostgreSQL — spin up real Postgres, run load/compare/dump, verify behavior
- **Unit tests:** Schema parsing, dataset parsing, FK resolution, column matchers
- **Gradle plugin tests:** Gradle TestKit functional tests running actual Gradle builds

## Dependencies

### `dbmangler-core`

- Kotlin stdlib
- Jackson (JSON parsing)
- PostgreSQL JDBC driver (runtime/provided)
- Kotest (test scope only — for DbMangler's own tests)
- Testcontainers PostgreSQL (test scope)

### `dbmangler-gradle-plugin`

- Gradle Plugin API
- `dbmangler-core` (for schema/dataset parsing)
- Gradle TestKit (test scope)
