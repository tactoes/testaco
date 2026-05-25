# Gradle Tasks Reference

PgFixtures provides four Gradle tasks for managing schema and dataset files. These tasks modify your schema file and optionally update all dataset files to maintain consistency.

## Table of Contents

- [testacoAddColumn](#testacoaddcolumn)
- [testacoRemoveColumn](#testacoremovecolumn)
- [testacoAddTable](#testacoaddtable)
- [testacoRemoveTable](#testacoremovemtable)

## testacoAddColumn

Adds a column to a table in the schema file and all dataset files.

### Usage

```bash
./gradlew testacoAddColumn \
  --table=<table_name> \
  --column=<column_name> \
  --type=<postgres_type> \
  [--nullable=true|false] \
  [--default=<value>]
```

### Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `--table` | Yes | - | Name of the table to add the column to |
| `--column` | Yes | - | Name of the column to add |
| `--type` | Yes | - | PostgreSQL type (e.g., `text`, `bigint`, `boolean`) |
| `--nullable` | No | `true` | Whether the column is nullable |
| `--default` | No | - | Default value for the column (required if `--nullable=false`) |

### Examples

#### Add Nullable Column

```bash
./gradlew testacoAddColumn \
  --table=users \
  --column=phone \
  --type=text
```

**Before (`testaco-schema.json`):**
```json
{
  "users": {
    "id": "bigint",
    "email": "text"
  }
}
```

**After (`testaco-schema.json`):**
```json
{
  "users": {
    "id": "bigint",
    "email": "text",
    "phone": "text"
  }
}
```

**Dataset files updated:**
All dataset files containing the `users` table will have `"phone": null` added to each row.

#### Add Non-Nullable Column with Default

```bash
./gradlew testacoAddColumn \
  --table=users \
  --column=status \
  --type=text \
  --nullable=false \
  --default=active
```

**After (`testaco-schema.json`):**
```json
{
  "users": {
    "id": "bigint",
    "email": "text",
    "phone": "text",
    "status": "text"
  }
}
```

**Dataset files updated:**
All dataset files containing the `users` table will have `"status": "active"` added to each row.

#### Add Timestamp Column

```bash
./gradlew testacoAddColumn \
  --table=orders \
  --column=shipped_at \
  --type="timestamp with time zone" \
  --nullable=true
```

**Note:** Use quotes for types with spaces.

### What Gets Modified

1. **Schema file** (`src/test/resources/testaco-schema.json`) — Column added to table definition
2. **Dataset files** — All JSON files in `src/test/resources/**/*.json` containing the table:
   - Nullable columns: `"column_name": null` added to each row
   - Non-nullable columns: `"column_name": <default_value>` added to each row

### Error Handling

- **Table not found:** Task fails if the table doesn't exist in the schema file
- **Column already exists:** Task fails if the column already exists
- **Missing default:** Task fails if `--nullable=false` and no `--default` provided

## testacoRemoveColumn

Removes a column from a table in the schema file and all dataset files.

### Usage

```bash
./gradlew testacoRemoveColumn \
  --table=<table_name> \
  --column=<column_name>
```

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--table` | Yes | Name of the table to remove the column from |
| `--column` | Yes | Name of the column to remove |

### Examples

#### Remove Column

```bash
./gradlew testacoRemoveColumn \
  --table=users \
  --column=phone
```

**Before (`testaco-schema.json`):**
```json
{
  "users": {
    "id": "bigint",
    "email": "text",
    "phone": "text"
  }
}
```

**After (`testaco-schema.json`):**
```json
{
  "users": {
    "id": "bigint",
    "email": "text"
  }
}
```

**Dataset files updated:**
All dataset files containing the `users` table will have the `phone` field removed from each row.

### What Gets Modified

1. **Schema file** — Column removed from table definition
2. **Dataset files** — The column removed from all rows in all dataset files

### Error Handling

- **Table not found:** Task fails if the table doesn't exist in the schema file
- **Column not found:** Task fails if the column doesn't exist in the table

## testacoAddTable

Adds a new table to the schema file.

### Usage

```bash
./gradlew testacoAddTable \
  --table=<table_name> \
  --columns=<col1>:<type1>,<col2>:<type2>,...
```

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--table` | Yes | Name of the table to add |
| `--columns` | Yes | Comma-separated list of columns in `name:type` format |

### Examples

#### Add Simple Table

```bash
./gradlew testacoAddTable \
  --table=addresses \
  --columns=id:bigint,street:text,city:text
```

**After (`testaco-schema.json`):**
```json
{
  "users": {
    "id": "bigint",
    "email": "text"
  },
  "addresses": {
    "id": "bigint",
    "street": "text",
    "city": "text"
  }
}
```

#### Add Table with Complex Types

```bash
./gradlew testacoAddTable \
  --table=audit_logs \
  --columns="id:bigint,user_id:bigint,action:text,created_at:timestamp with time zone"
```

**Note:** Use quotes when column types contain spaces.

#### Add Table with Multiple Columns

```bash
./gradlew testacoAddTable \
  --table=products \
  --columns=id:bigint,name:text,description:text,price:numeric,in_stock:boolean,created_at:"timestamp with time zone"
```

### What Gets Modified

1. **Schema file** — New table added with specified columns
2. **Dataset files** — Not modified (table doesn't exist in any datasets yet)

### Error Handling

- **Table already exists:** Task fails if the table already exists in the schema file
- **Invalid column format:** Task fails if columns are not in `name:type` format

## testacoRemoveTable

Removes a table from the schema file and all dataset files.

### Usage

```bash
./gradlew testacoRemoveTable \
  --table=<table_name>
```

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--table` | Yes | Name of the table to remove |

### Examples

#### Remove Table

```bash
./gradlew testacoRemoveTable \
  --table=addresses
```

**Before (`testaco-schema.json`):**
```json
{
  "users": {
    "id": "bigint",
    "email": "text"
  },
  "addresses": {
    "id": "bigint",
    "street": "text",
    "city": "text"
  }
}
```

**After (`testaco-schema.json`):**
```json
{
  "users": {
    "id": "bigint",
    "email": "text"
  }
}
```

**Dataset files updated:**
All dataset files will have the `addresses` table removed.

### What Gets Modified

1. **Schema file** — Table removed from schema
2. **Dataset files** — Table removed from all dataset files

### Error Handling

- **Table not found:** Task fails if the table doesn't exist in the schema file

## Common Workflows

### Adding a New Feature with New Columns

```bash
# Add column to store user's timezone
./gradlew testacoAddColumn \
  --table=users \
  --column=timezone \
  --type=text \
  --nullable=false \
  --default=UTC

# Run tests to verify datasets are valid
./gradlew test
```

### Removing Deprecated Columns

```bash
# Remove deprecated column
./gradlew testacoRemoveColumn \
  --table=users \
  --column=legacy_id

# Run tests to verify datasets are valid
./gradlew test
```

### Adding a New Table

```bash
# Add new table
./gradlew testacoAddTable \
  --table=sessions \
  --columns=id:bigint,user_id:bigint,token:text,expires_at:"timestamp with time zone"

# Manually create dataset files for tests that need this table
mkdir -p src/test/resources/com/example/SessionServiceTest
echo '{"sessions": []}' > src/test/resources/com/example/SessionServiceTest/setup.json
```

### Removing an Unused Table

```bash
# Remove table no longer in use
./gradlew testacoRemoveTable \
  --table=deprecated_logs

# Run tests to verify no tests were using this table
./gradlew test
```

## Best Practices

### 1. Run Tests After Schema Changes

Always run tests after modifying the schema to ensure datasets are still valid:

```bash
./gradlew testacoAddColumn --table=users --column=phone --type=text
./gradlew test
```

### 2. Use Nullable Columns When Possible

Nullable columns are easier to add because you don't need to decide on a default value:

```bash
# Preferred: Nullable column
./gradlew testacoAddColumn --table=users --column=phone --type=text

# Avoid if possible: Non-nullable column with arbitrary default
./gradlew testacoAddColumn --table=users --column=phone --type=text --nullable=false --default=""
```

### 3. Use Meaningful Defaults for Non-Nullable Columns

When adding non-nullable columns, choose defaults that make sense:

```bash
# Good: Meaningful default
./gradlew testacoAddColumn --table=users --column=status --type=text --nullable=false --default=active

# Bad: Arbitrary default
./gradlew testacoAddColumn --table=users --column=status --type=text --nullable=false --default=unknown
```

### 4. Quote Types with Spaces

PostgreSQL types with spaces must be quoted:

```bash
./gradlew testacoAddColumn \
  --table=orders \
  --column=created_at \
  --type="timestamp with time zone"
```

### 5. Review Dataset Changes

After running a task, review the changes to ensure datasets look correct:

```bash
git diff src/test/resources
```

### 6. Commit Schema and Dataset Changes Together

Schema and dataset changes should be committed together to maintain consistency:

```bash
./gradlew testacoAddColumn --table=users --column=phone --type=text
git add -A
git commit -m "feat: add phone column to users table"
```

## Troubleshooting

### Task Not Found

If the task is not found, ensure the plugin is applied in `build.gradle.kts`:

```kotlin
plugins {
    id("org.testaco.gradle-plugin") version "0.1.0-SNAPSHOT"
}
```

### Schema File Not Found

The tasks expect the schema file at `src/test/resources/testaco-schema.json`. If you use a custom path, you may need to update the plugin configuration (see plugin documentation).

### Dataset Files Not Updated

Ensure dataset files are valid JSON and follow the PgFixtures format:

```json
{
  "table_name": [
    {"column1": "value1", "column2": "value2"}
  ]
}
```

### Invalid Type

Ensure you use valid PostgreSQL types:
- `text`, `varchar`
- `bigint`, `integer`, `smallint`
- `numeric`, `decimal`
- `boolean`
- `timestamp`, `timestamp with time zone`
- `date`, `time`
- `jsonb`, `json`
- etc.

See [PostgreSQL Data Types](https://www.postgresql.org/docs/current/datatype.html) for the full list.
