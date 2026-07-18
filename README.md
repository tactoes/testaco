# Testaco

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![GitHub Actions](https://github.com/tactoes/testaco/actions/workflows/tests.yml/badge.svg)](https://github.com/tactoes/testaco/actions)
[![Maven Central](https://img.shields.io/maven-central/v/org.testaco/testaco-core.svg)](https://search.maven.org/search?q=g:org.testaco)

Testaco is a Kotlin reimplementation of DbUnit and an old DbUnit wrapper I made in 2008-ish, designed for modern test workflows. It uses JSON datasets for test 
data management and focuses exclusively on PostgreSQL, providing bidirectional schema validation, composable datasets, and powerful assertion matchers.

The  project started as a trial to see how far I could push AI to reimplement old code. The end result looks Ok, although I have not gotten
around to use it in a professional project or read through all the code yet.

## Why?

Tests have a regrettable side effect, and that is a kind of stiffening of the boundaries that are used for testing. A project that only uses unit tests
will have a harder time making refactoring changes across existing unit boundaries because the tests will not be as useful for refactoring
(See "refactoring", Fowler). The boundaries they are written against will change, which will reduce or remove the utility of the tests
for refactoring.

There are pre-existing interfaces that are more flexible in a typical application. The database layer has well-defined ways of mutating the
database schema (["Database refactoring", Ambler et al](https://databaserefactoring.com/)). Well-designed applications are the sole
user of its database, which cuts down on the need for versioning, and tools like [Flyway](https://github.com/flyway/flyway) handles
evolution of database schemas well. I prefer having at least some tests that pull up the whole application, interacting directly with
the database, and a nice side-effect of these kinds of tests is that refactoring becomes a test-supported activity.

Tests do need to load, dump and compare the data in the database, and this is where Testaco comes in. There are scripts within Testaco
that allows evolving database data sets automatically from the command line or an AI tool. The workflow would be
- create Flyway-based mutation of database schema
- migrate database data sets using Testaco.
- run tests. If you have a clean design and have followed the database refactoring rulebook you will have no failing tests.

Testaco is written to be as test-library agnostic as possible, although I personally use kotest and testcontainers in the 
projects I am currently working on. The library is meant to be used as a lockdown of functionality, causing tests to fail 
when something changes. It is up to the developer to use tools like "diff" to figure out what changed, and approve the 
changes by updating the reference files, or fixing the bug.

Give it a spin, and tell me how it worked out for you? Positive feedback is also welcome through the "praise" github issues
label?

## Features

- **JSON-based datasets** — Human-readable, version-control friendly test data
- **Bidirectional schema validation** — Validates schema file against live DB and datasets against schema file
- **Composable datasets** — Load multiple datasets in sequence with automatic merging
- **Powerful matchers** — `~now`, `~now±PT10S`, `~ignore`, `~regex:pattern` for flexible assertions
- **Gradle tasks** — Add/remove tables and columns from schema and dataset files
- **Auto-dump on failure** — Automatically dumps actual DB state to `build/test-results/testaco/<test-path>/actual.json` on assertion mismatch

## Quick Start

### 1. Add Dependency

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("org.testaco:testaco-core:0.1.0-SNAPSHOT")
}

plugins {
    id("org.testaco.gradle-plugin") version "0.1.0-SNAPSHOT"
}
```

### 2. Create Schema File

Create `src/test/resources/testaco-schema.json`:

```json
{
  "users": {
    "id": "bigint",
    "email": "text",
    "created_at": "timestamp with time zone"
  },
  "orders": {
    "id": "bigint",
    "user_id": "bigint",
    "status": "text",
    "created_at": "timestamp with time zone"
  }
}
```

### 3. Write Your First Test

```kotlin
import org.testaco.Testaco
import org.testaco.TestacoConfig
import io.kotest.core.spec.style.FunSpec
import org.postgresql.ds.PGSimpleDataSource

class OrderServiceTest : FunSpec({
    val dataSource = PGSimpleDataSource().apply {
        serverNames = arrayOf("localhost")
        databaseName = "test_db"
        user = "test_user"
        password = "test_pass"
    }

    val db = Testaco(dataSource)

    test("creates order successfully") {
        db.load("com/example/OrderServiceTest/creates_order/setup")

        // Your test logic here
        val service = OrderService(dataSource)
        service.createOrder(userId = 1, items = listOf("item1"))

        db.assertMatches("com/example/OrderServiceTest/creates_order/expected")
    }
})
```

### 4. Create Dataset Files

**`src/test/resources/com/example/OrderServiceTest/creates_order/setup.json`:**

```json
{
  "users": [
    {"id": 1, "email": "test@example.com", "created_at": "2024-01-01T00:00:00Z"}
  ],
  "orders": []
}
```

**`src/test/resources/com/example/OrderServiceTest/creates_order/expected.json`:**

```json
{
  "users": [
    {"id": 1, "email": "test@example.com", "created_at": "~ignore"}
  ],
  "orders": [
    {"id": 1, "user_id": 1, "status": "pending", "created_at": "~now±PT10S"}
  ]
}
```

## Configuration

Create an optional `src/test/resources/testaco-config.json`:

```json
{
  "schemas": ["public"],
  "ignoredColumns": {
    "*": ["updated_at"],
    "users": ["password_hash"]
  },
  "defaultTimestampTolerance": "PT5S",
  "loadStrategy": "CLEAN_INSERT"
}
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `schemas` | `List<String>` | `["public"]` | PostgreSQL schemas to manage |
| `ignoredColumns` | `Map<String, List<String>>` | `{}` | Columns to ignore during assertions. Use `"*"` for all tables |
| `defaultTimestampTolerance` | `Duration` | `PT5S` | Default tolerance for `~now` matcher |
| `loadStrategy` | `LoadStrategy` | `CLEAN_INSERT` | Load strategy: `CLEAN_INSERT` (truncate then insert) or `INSERT` (insert only) |

## API Reference

### Testaco

```kotlin
class Testaco(
    dataSource: DataSource,
    config: TestacoConfig? = null,
    schemaResourcePath: String = "testaco-schema.json"
)

// Load datasets into database
fun load(vararg resourcePaths: String, strategy: LoadStrategy = config.loadStrategy)

// Assert database matches expected dataset
fun assertMatches(resourcePath: String)

// Dump database to file
fun dump(resourcePath: String)

// Dump database to in-memory map
fun dump(): Map<String, List<Map<String, Any?>>>
```

### Special Matchers

- `"~now"` — Matches timestamp within default tolerance (5 seconds)
- `"~now±PT10S"` — Matches timestamp within specified tolerance (10 seconds)
- `"~ignore"` — Ignores the column value in assertions
- `"~regex:^[A-Z]{3}-\\d{3}$"` — Matches value against regex pattern

## Gradle Tasks

```bash
# Add column to schema and all dataset files
./gradlew testacoAddColumn --table=users --column=phone --type=text

# Add non-null column with default value
./gradlew testacoAddColumn --table=users --column=status --type=text --default=active

# Remove column from schema and all dataset files
./gradlew testacoRemoveColumn --table=users --column=phone

# Add table to schema
./gradlew testacoAddTable --table=addresses --columns=id:bigint,street:text,city:text

# Remove table from schema and all dataset files
./gradlew testacoRemoveTable --table=addresses
```

## Documentation

- [Usage Guide](docs/usage.md) — Detailed usage examples and patterns
- [Gradle Tasks](docs/gradle-tasks.md) — Complete reference for schema management tasks
- [Schema Reference](docs/schema.md) — Schema file format and validation
- [Schema Evolution](docs/schema-evolution.md) — Maintaining datasets as your schema changes
- [Debugging Failing Tests](docs/debugging.md) — Inspecting dumped vs expected datasets

## Requirements

- Kotlin 2.1+
- PostgreSQL 12+
- Kotest 5+
- Java 23+

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on:
- Opening issues and pull requests
- Code style and quality expectations
- Our review and merging process
- Releases and changelog management

For security concerns, see [SECURITY.md](SECURITY.md).

---
License

Copyright (c) 2026 Geir Hedemark

Licensed under the Apache License, Version 2.0. See LICENSE for details.
