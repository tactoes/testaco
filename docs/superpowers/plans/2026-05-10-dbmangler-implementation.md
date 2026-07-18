# DbMangler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Kotlin test data management library for PostgreSQL, with JSON datasets and a Gradle plugin for schema evolution.

**Architecture:** Two-module Gradle project. `dbmangler-core` is a standalone utility class that loads/asserts/dumps JSON datasets against a live PostgreSQL database. `dbmangler-gradle-plugin` provides tasks for adding/removing tables and columns across schema and dataset files.

**Tech Stack:** Kotlin, Gradle (Kotlin DSL), Jackson, PostgreSQL JDBC, Kotest 5, Testcontainers, Gradle Plugin API, Gradle TestKit.

---

## File Structure

### Root project
- `settings.gradle.kts` — includes both submodules
- `build.gradle.kts` — shared Kotlin/JVM config, dependency versions
- `gradle.properties` — project metadata

### `dbmangler-core`

```
dbmangler-core/
├── build.gradle.kts
└── src/
    ├── main/kotlin/com/dbmangler/
    │   ├── DbMangler.kt                    # Main entry point
    │   ├── config/
    │   │   └── DbManglerConfig.kt          # Config model + JSON parsing
    │   ├── schema/
    │   │   ├── Schema.kt                   # Schema data model
    │   │   ├── SchemaReader.kt             # Reads dbmangler-schema.json
    │   │   └── SchemaValidator.kt          # Validates schema↔DB and dataset↔schema
    │   ├── dataset/
    │   │   ├── DataSet.kt                  # Dataset data model
    │   │   ├── DataSetReader.kt            # Parses + merges JSON datasets
    │   │   └── DataSetWriter.kt            # Writes DB state to JSON
    │   ├── operation/
    │   │   ├── LoadOperation.kt            # CLEAN_INSERT / INSERT
    │   │   ├── CompareOperation.kt         # Asserts DB matches expected
    │   │   └── DumpOperation.kt            # Dumps DB to file
    │   ├── assertion/
    │   │   ├── ColumnMatcher.kt            # Matcher interface + implementations
    │   │   └── ComparisonResult.kt         # Diff representation
    │   └── util/
    │       ├── ForeignKeyResolver.kt       # Topological sort for FK deps
    │       └── PostgresIntrospector.kt     # Queries information_schema
    └── test/kotlin/com/dbmangler/
        ├── schema/
        │   ├── SchemaReaderTest.kt
        │   └── SchemaValidatorTest.kt
        ├── dataset/
        │   ├── DataSetReaderTest.kt
        │   └── DataSetWriterTest.kt
        ├── assertion/
        │   └── ColumnMatcherTest.kt
        ├── util/
        │   └── ForeignKeyResolverTest.kt
        └── integration/
            ├── LoadOperationTest.kt
            ├── CompareOperationTest.kt
            ├── DumpOperationTest.kt
            └── DbManglerTest.kt
```

### `dbmangler-gradle-plugin`

```
dbmangler-gradle-plugin/
├── build.gradle.kts
└── src/
    ├── main/kotlin/com/dbmangler/gradle/
    │   ├── DbManglerPlugin.kt
    │   ├── AddColumnTask.kt
    │   ├── RemoveColumnTask.kt
    │   ├── AddTableTask.kt
    │   └── RemoveTableTask.kt
    └── test/kotlin/com/dbmangler/gradle/
        └── functional/
            ├── AddColumnTaskTest.kt
            ├── RemoveColumnTaskTest.kt
            ├── AddTableTaskTest.kt
            └── RemoveTableTaskTest.kt
```

---

## Task 1: Gradle Project Scaffolding

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `dbmangler-core/build.gradle.kts`
- Create: `dbmangler-gradle-plugin/build.gradle.kts`

- [ ] **Step 1: Create root `settings.gradle.kts`**

```kotlin
rootProject.name = "dbmangler"

include("dbmangler-core")
include("dbmangler-gradle-plugin")
```

- [ ] **Step 2: Create root `build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm") version "1.9.22" apply false
}

allprojects {
    group = "com.dbmangler"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
```

- [ ] **Step 3: Create `gradle.properties`**

```properties
kotlin.code.style=official
org.gradle.parallel=true
```

- [ ] **Step 4: Create `dbmangler-core/build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")

    compileOnly("org.postgresql:postgresql:42.7.3")

    testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.postgresql:postgresql:42.7.3")
}
```

- [ ] **Step 5: Create `dbmangler-gradle-plugin/build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":dbmangler-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")

    testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
}

gradlePlugin {
    plugins {
        create("dbmangler") {
            id = "com.dbmangler"
            implementationClass = "com.dbmangler.gradle.DbManglerPlugin"
        }
    }
}
```

- [ ] **Step 6: Install Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.7`
Expected: `gradle/wrapper/` directory and `gradlew` script created.

- [ ] **Step 7: Verify project compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (with empty source sets)

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: scaffold Gradle multi-module project"
```

---

## Task 2: Schema Data Model

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/schema/Schema.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/schema/SchemaReaderTest.kt`
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/schema/SchemaReader.kt`

- [ ] **Step 1: Write the schema model**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/schema/Schema.kt`:

```kotlin
package com.dbmangler.schema

data class Schema(
    val schemas: List<String>,
    val tables: Map<String, TableDef>
)

data class TableDef(
    val columns: Map<String, ColumnDef>,
    val primaryKey: List<String> = emptyList(),
    val foreignKeys: List<ForeignKeyDef> = emptyList()
)

data class ColumnDef(
    val type: String,
    val nullable: Boolean
)

data class ForeignKeyDef(
    val columns: List<String>,
    val references: String,
    val referencedColumns: List<String>
)
```

- [ ] **Step 2: Write failing test for SchemaReader**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/schema/SchemaReaderTest.kt`:

```kotlin
package com.dbmangler.schema

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.assertions.throwables.shouldThrow

class SchemaReaderTest : FunSpec({

    test("reads a valid schema file from classpath") {
        val schema = SchemaReader.fromClasspath("test-schemas/valid-schema.json")

        schema.schemas shouldBe listOf("public")
        schema.tables shouldContainKey "public.users"

        val users = schema.tables["public.users"]!!
        users.columns["id"]!!.type shouldBe "bigint"
        users.columns["id"]!!.nullable shouldBe false
        users.columns["email"]!!.nullable shouldBe true
        users.primaryKey shouldBe listOf("id")
    }

    test("reads foreign keys") {
        val schema = SchemaReader.fromClasspath("test-schemas/valid-schema.json")

        val orders = schema.tables["public.orders"]!!
        orders.foreignKeys.size shouldBe 1
        orders.foreignKeys[0].columns shouldBe listOf("user_id")
        orders.foreignKeys[0].references shouldBe "public.users"
        orders.foreignKeys[0].referencedColumns shouldBe listOf("id")
    }

    test("throws on missing schema file") {
        shouldThrow<IllegalArgumentException> {
            SchemaReader.fromClasspath("nonexistent.json")
        }
    }
})
```

- [ ] **Step 3: Create test resource file**

Create `dbmangler-core/src/test/resources/test-schemas/valid-schema.json`:

```json
{
  "schemas": ["public"],
  "tables": {
    "public.users": {
      "columns": {
        "id": { "type": "bigint", "nullable": false },
        "name": { "type": "text", "nullable": false },
        "email": { "type": "text", "nullable": true }
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

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.schema.SchemaReaderTest" --info`
Expected: FAIL — `SchemaReader` class does not exist.

- [ ] **Step 5: Implement SchemaReader**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/schema/SchemaReader.kt`:

```kotlin
package com.dbmangler.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object SchemaReader {

    private val mapper = ObjectMapper().registerKotlinModule()

    fun fromClasspath(resourcePath: String): Schema {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Schema file not found on classpath: $resourcePath")
        return parse(mapper.readTree(stream))
    }

    fun fromString(json: String): Schema {
        return parse(mapper.readTree(json))
    }

    private fun parse(root: JsonNode): Schema {
        val schemas = root["schemas"]?.map { it.asText() } ?: listOf("public")
        val tablesNode = root["tables"] ?: throw IllegalArgumentException("Schema file must contain 'tables'")

        val tables = mutableMapOf<String, TableDef>()
        tablesNode.fields().forEach { (tableName, tableNode) ->
            tables[tableName] = parseTable(tableNode)
        }

        return Schema(schemas = schemas, tables = tables)
    }

    private fun parseTable(node: JsonNode): TableDef {
        val columns = mutableMapOf<String, ColumnDef>()
        node["columns"]?.fields()?.forEach { (colName, colNode) ->
            columns[colName] = ColumnDef(
                type = colNode["type"].asText(),
                nullable = colNode["nullable"]?.asBoolean() ?: true
            )
        }

        val primaryKey = node["primaryKey"]?.map { it.asText() } ?: emptyList()

        val foreignKeys = node["foreignKeys"]?.map { fkNode ->
            ForeignKeyDef(
                columns = fkNode["columns"].map { it.asText() },
                references = fkNode["references"].asText(),
                referencedColumns = fkNode["referencedColumns"].map { it.asText() }
            )
        } ?: emptyList()

        return TableDef(columns = columns, primaryKey = primaryKey, foreignKeys = foreignKeys)
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.schema.SchemaReaderTest" --info`
Expected: PASS — all 3 tests green.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add schema model and reader"
```

---

## Task 3: Configuration Model

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/config/DbManglerConfig.kt`

- [ ] **Step 1: Implement DbManglerConfig**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/config/DbManglerConfig.kt`:

```kotlin
package com.dbmangler.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.Duration

data class DbManglerConfig(
    val schemas: List<String> = listOf("public"),
    val ignoredColumns: Map<String, List<String>> = emptyMap(),
    val defaultTimestampTolerance: Duration = Duration.ofSeconds(5),
    val loadStrategy: LoadStrategy = LoadStrategy.CLEAN_INSERT
) {
    fun isColumnIgnored(table: String, column: String): Boolean {
        val globalIgnored = ignoredColumns["*"] ?: emptyList()
        val tableIgnored = ignoredColumns[table] ?: emptyList()
        return column in globalIgnored || column in tableIgnored
    }

    companion object {
        private val mapper = ObjectMapper().registerKotlinModule()

        fun fromClasspath(resourcePath: String = "dbmangler-config.json"): DbManglerConfig {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
                ?: return DbManglerConfig()
            return fromJson(mapper.readTree(stream))
        }

        fun fromJson(node: JsonNode): DbManglerConfig {
            val schemas = node["schemas"]?.map { it.asText() } ?: listOf("public")
            val ignoredColumns = mutableMapOf<String, List<String>>()
            node["ignoredColumns"]?.fields()?.forEach { (table, cols) ->
                ignoredColumns[table] = cols.map { it.asText() }
            }
            val tolerance = node["defaultTimestampTolerance"]?.asText()?.let {
                Duration.parse(it)
            } ?: Duration.ofSeconds(5)
            val strategy = node["loadStrategy"]?.asText()?.let {
                LoadStrategy.valueOf(it)
            } ?: LoadStrategy.CLEAN_INSERT

            return DbManglerConfig(
                schemas = schemas,
                ignoredColumns = ignoredColumns,
                defaultTimestampTolerance = tolerance,
                loadStrategy = strategy
            )
        }
    }
}

enum class LoadStrategy {
    CLEAN_INSERT,
    INSERT
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :dbmangler-core:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add configuration model"
```

---

## Task 4: Dataset Model and Reader

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/dataset/DataSet.kt`
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/dataset/DataSetReader.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/dataset/DataSetReaderTest.kt`

- [ ] **Step 1: Implement DataSet model**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/dataset/DataSet.kt`:

```kotlin
package com.dbmangler.dataset

data class DataSet(
    val tables: Map<String, List<Map<String, Any?>>>
) {
    fun merge(other: DataSet): DataSet {
        val merged = tables.toMutableMap()
        for ((table, rows) in other.tables) {
            merged[table] = (merged[table] ?: emptyList()) + rows
        }
        return DataSet(merged)
    }

    companion object {
        fun empty() = DataSet(emptyMap())
    }
}
```

- [ ] **Step 2: Write failing test for DataSetReader**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/dataset/DataSetReaderTest.kt`:

```kotlin
package com.dbmangler.dataset

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.assertions.throwables.shouldThrow

class DataSetReaderTest : FunSpec({

    test("reads a dataset from classpath") {
        val ds = DataSetReader.fromClasspath("test-datasets/users")

        ds.tables.keys shouldBe setOf("users")
        ds.tables["users"]!! shouldHaveSize 2
        ds.tables["users"]!![0]["name"] shouldBe "Alice"
    }

    test("merges multiple datasets") {
        val ds = DataSetReader.fromClasspath(
            "test-datasets/users",
            "test-datasets/orders"
        )

        ds.tables.keys shouldBe setOf("users", "orders")
        ds.tables["users"]!! shouldHaveSize 2
        ds.tables["orders"]!! shouldHaveSize 1
    }

    test("merging appends rows for same table") {
        val ds = DataSetReader.fromClasspath(
            "test-datasets/users",
            "test-datasets/more-users"
        )

        ds.tables["users"]!! shouldHaveSize 3
    }

    test("throws on missing dataset file") {
        shouldThrow<IllegalArgumentException> {
            DataSetReader.fromClasspath("nonexistent")
        }
    }
})
```

- [ ] **Step 3: Create test resource files**

Create `dbmangler-core/src/test/resources/test-datasets/users.json`:

```json
{
  "users": [
    { "id": 1, "name": "Alice", "email": "alice@example.com" },
    { "id": 2, "name": "Bob", "email": null }
  ]
}
```

Create `dbmangler-core/src/test/resources/test-datasets/orders.json`:

```json
{
  "orders": [
    { "id": 10, "user_id": 1, "amount": 99.99 }
  ]
}
```

Create `dbmangler-core/src/test/resources/test-datasets/more-users.json`:

```json
{
  "users": [
    { "id": 3, "name": "Charlie", "email": "charlie@example.com" }
  ]
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.dataset.DataSetReaderTest" --info`
Expected: FAIL — `DataSetReader` class does not exist.

- [ ] **Step 5: Implement DataSetReader**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/dataset/DataSetReader.kt`:

```kotlin
package com.dbmangler.dataset

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object DataSetReader {

    private val mapper = ObjectMapper().registerKotlinModule()

    fun fromClasspath(vararg resourcePaths: String): DataSet {
        return resourcePaths.fold(DataSet.empty()) { acc, path ->
            acc.merge(readSingle(path))
        }
    }

    fun fromString(json: String): DataSet {
        return parseTables(json)
    }

    private fun readSingle(resourcePath: String): DataSet {
        val fullPath = if (resourcePath.endsWith(".json")) resourcePath else "$resourcePath.json"
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(fullPath)
            ?: throw IllegalArgumentException("Dataset file not found on classpath: $fullPath")
        return parseTables(stream.bufferedReader().readText())
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTables(json: String): DataSet {
        val root = mapper.readValue(json, Map::class.java) as Map<String, Any>
        val tables = mutableMapOf<String, List<Map<String, Any?>>>()
        for ((tableName, value) in root) {
            val rows = value as? List<Map<String, Any?>>
                ?: throw IllegalArgumentException("Table '$tableName' must be an array of objects")
            tables[tableName] = rows
        }
        return DataSet(tables)
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.dataset.DataSetReaderTest" --info`
Expected: PASS — all 4 tests green.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add dataset model and reader with composable merging"
```

---

## Task 5: Column Matchers

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/assertion/ColumnMatcher.kt`
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/assertion/ComparisonResult.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/assertion/ColumnMatcherTest.kt`

- [ ] **Step 1: Write failing test for matchers**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/assertion/ColumnMatcherTest.kt`:

```kotlin
package com.dbmangler.assertion

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ColumnMatcherTest : FunSpec({

    test("exact matcher matches equal values") {
        val matcher = ColumnMatcher.forExpected("hello", Duration.ofSeconds(5))
        matcher.matches("hello") shouldBe true
        matcher.matches("world") shouldBe false
    }

    test("exact matcher matches null") {
        val matcher = ColumnMatcher.forExpected(null, Duration.ofSeconds(5))
        matcher.matches(null) shouldBe true
        matcher.matches("nope") shouldBe false
    }

    test("~ignore always matches") {
        val matcher = ColumnMatcher.forExpected("~ignore", Duration.ofSeconds(5))
        matcher.matches("anything") shouldBe true
        matcher.matches(null) shouldBe true
    }

    test("~now matches timestamp near current time") {
        val matcher = ColumnMatcher.forExpected("~now", Duration.ofSeconds(5))
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        matcher.matches(now) shouldBe true
    }

    test("~now rejects timestamp far from current time") {
        val matcher = ColumnMatcher.forExpected("~now", Duration.ofSeconds(5))
        val old = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10)
        matcher.matches(old) shouldBe false
    }

    test("~now with custom tolerance") {
        val matcher = ColumnMatcher.forExpected("~now±PT30S", Duration.ofSeconds(5))
        val recent = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(20)
        matcher.matches(recent) shouldBe true
    }

    test("~regex matches pattern") {
        val matcher = ColumnMatcher.forExpected("~regex:[A-Z]{3}-\\d+", Duration.ofSeconds(5))
        matcher.matches("ABC-123") shouldBe true
        matcher.matches("abc-123") shouldBe false
    }

    test("numeric comparison handles BigDecimal vs Int") {
        val matcher = ColumnMatcher.forExpected(99.99, Duration.ofSeconds(5))
        matcher.matches(java.math.BigDecimal("99.99")) shouldBe true
    }
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.assertion.ColumnMatcherTest" --info`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement ComparisonResult**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/assertion/ComparisonResult.kt`:

```kotlin
package com.dbmangler.assertion

data class ComparisonResult(
    val matches: Boolean,
    val tableDiffs: Map<String, TableDiff> = emptyMap()
) {
    companion object {
        fun match() = ComparisonResult(matches = true)
    }
}

data class TableDiff(
    val missingRows: List<Map<String, Any?>> = emptyList(),
    val extraRows: List<Map<String, Any?>> = emptyList(),
    val columnDiffs: List<RowDiff> = emptyList()
)

data class RowDiff(
    val primaryKey: Map<String, Any?>,
    val columnMismatches: Map<String, ColumnMismatch>
)

data class ColumnMismatch(
    val expected: Any?,
    val actual: Any?,
    val message: String
)

class DataSetMismatchException(
    val result: ComparisonResult,
    val dumpPath: String? = null
) : RuntimeException(formatMessage(result, dumpPath)) {
    companion object {
        private fun formatMessage(result: ComparisonResult, dumpPath: String?): String {
            val sb = StringBuilder("Dataset mismatch\n\n")
            for ((table, diff) in result.tableDiffs) {
                sb.appendLine("Table '$table':")
                for (rowDiff in diff.columnDiffs) {
                    sb.appendLine("  Row (PK: ${rowDiff.primaryKey}):")
                    for ((col, mismatch) in rowDiff.columnMismatches) {
                        sb.appendLine("    $col: ${mismatch.message}")
                    }
                }
                if (diff.missingRows.isNotEmpty()) sb.appendLine("  Missing rows: ${diff.missingRows.size}")
                if (diff.extraRows.isNotEmpty()) sb.appendLine("  Extra rows: ${diff.extraRows.size}")
            }
            if (dumpPath != null) {
                sb.appendLine("\nActual database state dumped to: $dumpPath")
            }
            return sb.toString()
        }
    }
}
```

- [ ] **Step 4: Implement ColumnMatcher**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/assertion/ColumnMatcher.kt`:

```kotlin
package com.dbmangler.assertion

import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.Temporal

sealed class ColumnMatcher {

    abstract fun matches(actual: Any?): Boolean
    abstract fun describe(actual: Any?): String

    companion object {
        fun forExpected(expected: Any?, defaultTolerance: Duration): ColumnMatcher {
            if (expected is String) {
                if (expected == "~ignore") return IgnoreMatcher
                if (expected == "~now") return NowMatcher(defaultTolerance)
                if (expected.startsWith("~now±")) {
                    val tolerance = Duration.parse(expected.removePrefix("~now±"))
                    return NowMatcher(tolerance)
                }
                if (expected.startsWith("~regex:")) {
                    val pattern = expected.removePrefix("~regex:")
                    return RegexMatcher(Regex(pattern))
                }
            }
            return ExactMatcher(expected)
        }
    }
}

object IgnoreMatcher : ColumnMatcher() {
    override fun matches(actual: Any?) = true
    override fun describe(actual: Any?) = "ignored"
}

class NowMatcher(private val tolerance: Duration) : ColumnMatcher() {
    override fun matches(actual: Any?): Boolean {
        val timestamp = when (actual) {
            is OffsetDateTime -> actual
            is java.sql.Timestamp -> actual.toInstant().atOffset(ZoneOffset.UTC)
            is String -> OffsetDateTime.parse(actual)
            else -> return false
        }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val diff = Duration.between(timestamp, now).abs()
        return diff <= tolerance
    }

    override fun describe(actual: Any?) = "expected ~now (±${tolerance}), got $actual"
}

class RegexMatcher(private val pattern: Regex) : ColumnMatcher() {
    override fun matches(actual: Any?): Boolean {
        return actual?.toString()?.let { pattern.matches(it) } ?: false
    }

    override fun describe(actual: Any?) = "expected ~regex:${pattern.pattern}, got $actual"
}

class ExactMatcher(private val expected: Any?) : ColumnMatcher() {
    override fun matches(actual: Any?): Boolean {
        if (expected == null && actual == null) return true
        if (expected == null || actual == null) return false
        if (expected is Number && actual is Number) {
            return BigDecimal(expected.toString()).compareTo(BigDecimal(actual.toString())) == 0
        }
        return expected == actual
    }

    override fun describe(actual: Any?) = "expected $expected, got $actual"
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.assertion.ColumnMatcherTest" --info`
Expected: PASS — all 8 tests green.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add column matchers (exact, ~now, ~ignore, ~regex)"
```

---

## Task 6: FK Resolver (Topological Sort)

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/util/ForeignKeyResolver.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/util/ForeignKeyResolverTest.kt`

- [ ] **Step 1: Write failing test**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/util/ForeignKeyResolverTest.kt`:

```kotlin
package com.dbmangler.util

import com.dbmangler.schema.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainInOrder

class ForeignKeyResolverTest : FunSpec({

    test("resolves simple FK dependency order") {
        val schema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.orders" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "user_id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id"),
                    foreignKeys = listOf(ForeignKeyDef(listOf("user_id"), "public.users", listOf("id")))
                ),
                "public.users" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id")
                )
            )
        )

        val resolver = ForeignKeyResolver(schema)
        val insertOrder = resolver.insertOrder(listOf("public.users", "public.orders"))

        insertOrder shouldContainInOrder listOf("public.users", "public.orders")
    }

    test("detects cycles") {
        val schema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.a" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "b_id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id"),
                    foreignKeys = listOf(ForeignKeyDef(listOf("b_id"), "public.b", listOf("id")))
                ),
                "public.b" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "a_id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id"),
                    foreignKeys = listOf(ForeignKeyDef(listOf("a_id"), "public.a", listOf("id")))
                )
            )
        )

        val resolver = ForeignKeyResolver(schema)
        resolver.hasCycles(listOf("public.a", "public.b")) shouldBe true
    }

    test("truncate order is reverse of insert order") {
        val schema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.orders" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "user_id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id"),
                    foreignKeys = listOf(ForeignKeyDef(listOf("user_id"), "public.users", listOf("id")))
                ),
                "public.users" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id")
                )
            )
        )

        val resolver = ForeignKeyResolver(schema)
        val truncateOrder = resolver.truncateOrder(listOf("public.users", "public.orders"))

        truncateOrder shouldContainInOrder listOf("public.orders", "public.users")
    }
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.util.ForeignKeyResolverTest" --info`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement ForeignKeyResolver**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/util/ForeignKeyResolver.kt`:

```kotlin
package com.dbmangler.util

import com.dbmangler.schema.Schema

class ForeignKeyResolver(private val schema: Schema) {

    fun insertOrder(tables: List<String>): List<String> {
        val graph = buildDependencyGraph(tables)
        return topologicalSort(graph, tables)
    }

    fun truncateOrder(tables: List<String>): List<String> {
        return insertOrder(tables).reversed()
    }

    fun hasCycles(tables: List<String>): Boolean {
        val graph = buildDependencyGraph(tables)
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun dfs(node: String): Boolean {
            if (node in inStack) return true
            if (node in visited) return false
            visited.add(node)
            inStack.add(node)
            for (dep in graph[node] ?: emptyList()) {
                if (dfs(dep)) return true
            }
            inStack.remove(node)
            return false
        }

        return tables.any { dfs(it) }
    }

    private fun buildDependencyGraph(tables: List<String>): Map<String, List<String>> {
        val tableSet = tables.toSet()
        val graph = mutableMapOf<String, MutableList<String>>()
        for (table in tables) {
            graph[table] = mutableListOf()
            val tableDef = schema.tables[table] ?: continue
            for (fk in tableDef.foreignKeys) {
                if (fk.references in tableSet) {
                    graph[table]!!.add(fk.references)
                }
            }
        }
        return graph
    }

    private fun topologicalSort(graph: Map<String, List<String>>, tables: List<String>): List<String> {
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun visit(node: String) {
            if (node in visited) return
            if (node in inStack) return
            inStack.add(node)
            for (dep in graph[node] ?: emptyList()) {
                visit(dep)
            }
            inStack.remove(node)
            visited.add(node)
            result.add(node)
        }

        for (table in tables) {
            visit(table)
        }
        return result
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.util.ForeignKeyResolverTest" --info`
Expected: PASS — all 3 tests green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add foreign key resolver with topological sort"
```


---

## Task 7: PostgreSQL Introspector

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/util/PostgresIntrospector.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/integration/PostgresIntrospectorTest.kt`

- [ ] **Step 1: Write failing integration test**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/integration/PostgresIntrospectorTest.kt`:

```kotlin
package com.dbmangler.integration

import com.dbmangler.util.PostgresIntrospector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.maps.shouldContainKey
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class PostgresIntrospectorTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("""
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY,
                    name TEXT NOT NULL,
                    email TEXT
                );
                CREATE TABLE orders (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES users(id),
                    amount NUMERIC NOT NULL
                );
            """)
        }
    }

    afterSpec { postgres.stop() }

    test("introspects tables and columns") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val introspector = PostgresIntrospector(conn)
            val schema = introspector.introspect(listOf("public"))

            schema.tables shouldContainKey "public.users"
            schema.tables["public.users"]!!.columns["id"]!!.type shouldBe "bigint"
            schema.tables["public.users"]!!.columns["id"]!!.nullable shouldBe false
            schema.tables["public.users"]!!.columns["email"]!!.nullable shouldBe true
        }
    }

    test("introspects primary keys") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val introspector = PostgresIntrospector(conn)
            val schema = introspector.introspect(listOf("public"))

            schema.tables["public.users"]!!.primaryKey shouldBe listOf("id")
        }
    }

    test("introspects foreign keys") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val introspector = PostgresIntrospector(conn)
            val schema = introspector.introspect(listOf("public"))

            val orderFks = schema.tables["public.orders"]!!.foreignKeys
            orderFks.size shouldBe 1
            orderFks[0].columns shouldBe listOf("user_id")
            orderFks[0].references shouldBe "public.users"
        }
    }
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.PostgresIntrospectorTest" --info`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement PostgresIntrospector**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/util/PostgresIntrospector.kt`:

```kotlin
package com.dbmangler.util

import com.dbmangler.schema.*
import java.sql.Connection

class PostgresIntrospector(private val connection: Connection) {

    fun introspect(schemas: List<String>): Schema {
        val tables = mutableMapOf<String, TableDef>()

        for (schemaName in schemas) {
            val tableNames = getTableNames(schemaName)
            for (tableName in tableNames) {
                val qualifiedName = "$schemaName.$tableName"
                val columns = getColumns(schemaName, tableName)
                val primaryKey = getPrimaryKey(schemaName, tableName)
                val foreignKeys = getForeignKeys(schemaName, tableName)
                tables[qualifiedName] = TableDef(columns = columns, primaryKey = primaryKey, foreignKeys = foreignKeys)
            }
        }

        return Schema(schemas = schemas, tables = tables)
    }

    private fun getTableNames(schema: String): List<String> {
        val sql = """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = ? AND table_type = 'BASE TABLE'
            ORDER BY table_name
        """
        val result = mutableListOf<String>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, schema)
            stmt.executeQuery().use { rs ->
                while (rs.next()) result.add(rs.getString("table_name"))
            }
        }
        return result
    }

    private fun getColumns(schema: String, table: String): Map<String, ColumnDef> {
        val sql = """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_schema = ? AND table_name = ?
            ORDER BY ordinal_position
        """
        val columns = mutableMapOf<String, ColumnDef>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    columns[rs.getString("column_name")] = ColumnDef(
                        type = normalizeType(rs.getString("data_type")),
                        nullable = rs.getString("is_nullable") == "YES"
                    )
                }
            }
        }
        return columns
    }

    private fun getPrimaryKey(schema: String, table: String): List<String> {
        val sql = """
            SELECT kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = ? AND tc.table_name = ? AND tc.constraint_type = 'PRIMARY KEY'
            ORDER BY kcu.ordinal_position
        """
        val result = mutableListOf<String>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                while (rs.next()) result.add(rs.getString("column_name"))
            }
        }
        return result
    }

    private fun getForeignKeys(schema: String, table: String): List<ForeignKeyDef> {
        val sql = """
            SELECT kcu.column_name, ccu.table_schema AS ref_schema, ccu.table_name AS ref_table,
                   ccu.column_name AS ref_column, tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name AND tc.table_schema = ccu.table_schema
            WHERE tc.table_schema = ? AND tc.table_name = ? AND tc.constraint_type = 'FOREIGN KEY'
            ORDER BY tc.constraint_name, kcu.ordinal_position
        """
        val fkMap = mutableMapOf<String, Triple<MutableList<String>, String, MutableList<String>>>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val cn = rs.getString("constraint_name")
                    val entry = fkMap.getOrPut(cn) {
                        Triple(mutableListOf(), "${rs.getString("ref_schema")}.${rs.getString("ref_table")}", mutableListOf())
                    }
                    entry.first.add(rs.getString("column_name"))
                    entry.third.add(rs.getString("ref_column"))
                }
            }
        }
        return fkMap.values.map { (cols, ref, refCols) ->
            ForeignKeyDef(columns = cols, references = ref, referencedColumns = refCols)
        }
    }

    private fun normalizeType(pgType: String): String = when (pgType) {
        "character varying" -> "varchar"
        "timestamp without time zone" -> "timestamp"
        else -> pgType
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.PostgresIntrospectorTest" --info`
Expected: PASS — all 3 tests green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add PostgreSQL introspector"
```

---

## Task 8: Schema Validator

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/schema/SchemaValidator.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/schema/SchemaValidatorTest.kt`

- [ ] **Step 1: Write failing test**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/schema/SchemaValidatorTest.kt`:

```kotlin
package com.dbmangler.schema

import com.dbmangler.config.DbManglerConfig
import com.dbmangler.dataset.DataSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain

class SchemaValidatorTest : FunSpec({

    val schema = Schema(
        schemas = listOf("public"),
        tables = mapOf(
            "public.users" to TableDef(
                columns = mapOf(
                    "id" to ColumnDef("bigint", false),
                    "name" to ColumnDef("text", false),
                    "email" to ColumnDef("text", true)
                ),
                primaryKey = listOf("id")
            )
        )
    )
    val config = DbManglerConfig()

    test("identical schemas pass validation") {
        SchemaValidator.validateSchemaMatch(schema, schema.copy(), config)
    }

    test("missing table in live DB is reported") {
        val liveSchema = Schema(schemas = listOf("public"), tables = emptyMap())
        val ex = shouldThrow<SchemaValidationException> {
            SchemaValidator.validateSchemaMatch(schema, liveSchema, config)
        }
        ex.message shouldContain "Missing tables"
    }

    test("column type mismatch is reported") {
        val liveSchema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.users" to TableDef(
                    columns = mapOf(
                        "id" to ColumnDef("integer", false),
                        "name" to ColumnDef("text", false),
                        "email" to ColumnDef("text", true)
                    ),
                    primaryKey = listOf("id")
                )
            )
        )
        val ex = shouldThrow<SchemaValidationException> {
            SchemaValidator.validateSchemaMatch(schema, liveSchema, config)
        }
        ex.message shouldContain "bigint"
    }

    test("dataset referencing unknown column fails") {
        val dataset = DataSet(mapOf("public.users" to listOf(mapOf("id" to 1, "bogus" to "x"))))
        val ex = shouldThrow<DataSetValidationException> {
            SchemaValidator.validateDataSetAgainstSchema(dataset, schema, config)
        }
        ex.message shouldContain "bogus"
    }

    test("dataset referencing unknown table fails") {
        val dataset = DataSet(mapOf("public.nope" to listOf(mapOf("id" to 1))))
        val ex = shouldThrow<DataSetValidationException> {
            SchemaValidator.validateDataSetAgainstSchema(dataset, schema, config)
        }
        ex.message shouldContain "nope"
    }

    test("ignored columns are excluded from validation") {
        val configWithIgnored = DbManglerConfig(ignoredColumns = mapOf("public.users" to listOf("email")))
        val liveSchema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.users" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "name" to ColumnDef("text", false)),
                    primaryKey = listOf("id")
                )
            )
        )
        // Should not throw — email mismatch is ignored
        SchemaValidator.validateSchemaMatch(schema, liveSchema, configWithIgnored)
    }
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.schema.SchemaValidatorTest" --info`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement SchemaValidator**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/schema/SchemaValidator.kt`:

```kotlin
package com.dbmangler.schema

import com.dbmangler.config.DbManglerConfig
import com.dbmangler.dataset.DataSet

class SchemaValidationException(message: String) : RuntimeException(message)
class DataSetValidationException(message: String) : RuntimeException(message)

object SchemaValidator {

    fun validateSchemaMatch(expected: Schema, live: Schema, config: DbManglerConfig) {
        val errors = mutableListOf<String>()
        val expectedTables = expected.tables.keys
        val liveTables = live.tables.keys

        val missing = expectedTables - liveTables
        val extra = liveTables - expectedTables
        if (missing.isNotEmpty()) errors.add("Missing tables: $missing")
        if (extra.isNotEmpty()) errors.add("Extra tables: $extra")

        for (tableName in expectedTables.intersect(liveTables)) {
            val et = expected.tables[tableName]!!
            val lt = live.tables[tableName]!!
            val eCols = et.columns.filterKeys { !config.isColumnIgnored(tableName, it) }
            val lCols = lt.columns.filterKeys { !config.isColumnIgnored(tableName, it) }

            val missingCols = eCols.keys - lCols.keys
            val extraCols = lCols.keys - eCols.keys
            if (missingCols.isNotEmpty()) errors.add("$tableName: missing columns: $missingCols")
            if (extraCols.isNotEmpty()) errors.add("$tableName: extra columns: $extraCols")

            for (col in eCols.keys.intersect(lCols.keys)) {
                val ec = eCols[col]!!; val lc = lCols[col]!!
                if (ec.type != lc.type)
                    errors.add("$tableName.$col: expected ${ec.type}, found ${lc.type}")
                if (ec.nullable != lc.nullable)
                    errors.add("$tableName.$col: expected nullable=${ec.nullable}, found nullable=${lc.nullable}")
            }
        }

        if (errors.isNotEmpty())
            throw SchemaValidationException("Database schema does not match dbmangler-schema.json\n\n  ${errors.joinToString("\n  ")}")
    }

    fun validateDataSetAgainstSchema(dataSet: DataSet, schema: Schema, config: DbManglerConfig) {
        val errors = mutableListOf<String>()
        for ((tableName, rows) in dataSet.tables) {
            val tableDef = schema.tables[tableName]
            if (tableDef == null) { errors.add("Dataset references unknown table '$tableName'"); continue }
            val validCols = tableDef.columns.keys.filterNot { config.isColumnIgnored(tableName, it) }.toSet()
            for (row in rows) {
                for (col in row.keys) {
                    if (config.isColumnIgnored(tableName, col)) continue
                    if (col !in validCols) {
                        val suggestion = validCols.minByOrNull { levenshtein(it, col) }
                        val hint = if (suggestion != null) " Did you mean '$tableName.$suggestion'?" else ""
                        errors.add("Dataset references unknown column '$tableName.$col'.$hint")
                    }
                }
            }
        }
        if (errors.isNotEmpty()) throw DataSetValidationException(errors.joinToString("\n"))
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length)
            dp[i][j] = minOf(dp[i-1][j]+1, dp[i][j-1]+1, dp[i-1][j-1] + if (a[i-1]==b[j-1]) 0 else 1)
        return dp[a.length][b.length]
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.schema.SchemaValidatorTest" --info`
Expected: PASS — all 6 tests green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add schema validator with bidirectional validation"
```

---

## Task 9: DataSet Writer

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/dataset/DataSetWriter.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/dataset/DataSetWriterTest.kt`

- [ ] **Step 1: Write failing test**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/dataset/DataSetWriterTest.kt`:

```kotlin
package com.dbmangler.dataset

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

class DataSetWriterTest : FunSpec({

    test("writes dataset to JSON string") {
        val ds = DataSet(mapOf("users" to listOf(mapOf("id" to 1, "name" to "Alice"))))
        val json = DataSetWriter.toJson(ds)
        json shouldContain "\"users\""
        json shouldContain "\"Alice\""
    }

    test("writes dataset to file and reads back") {
        val ds = DataSet(mapOf("users" to listOf(mapOf("id" to 1, "name" to "Bob"))))
        val tempFile = File.createTempFile("dbmangler-test", ".json")
        tempFile.deleteOnExit()
        DataSetWriter.toFile(ds, tempFile.toPath())
        val readBack = DataSetReader.fromString(tempFile.readText())
        readBack.tables["users"]!![0]["name"] shouldBe "Bob"
    }
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.dataset.DataSetWriterTest" --info`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement DataSetWriter**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/dataset/DataSetWriter.kt`:

```kotlin
package com.dbmangler.dataset

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Files
import java.nio.file.Path

object DataSetWriter {

    private val mapper = ObjectMapper().registerKotlinModule().enable(SerializationFeature.INDENT_OUTPUT)

    fun toJson(dataSet: DataSet): String = mapper.writeValueAsString(dataSet.tables)

    fun toFile(dataSet: DataSet, path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(path, toJson(dataSet))
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.dataset.DataSetWriterTest" --info`
Expected: PASS — all 2 tests green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add dataset writer for JSON serialization"
```

---

## Task 10: Load Operation

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/operation/LoadOperation.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/integration/LoadOperationTest.kt`

- [ ] **Step 1: Write failing integration test**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/integration/LoadOperationTest.kt`:

```kotlin
package com.dbmangler.integration

import com.dbmangler.config.DbManglerConfig
import com.dbmangler.config.LoadStrategy
import com.dbmangler.dataset.DataSet
import com.dbmangler.operation.LoadOperation
import com.dbmangler.schema.*
import com.dbmangler.util.ForeignKeyResolver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class LoadOperationTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("""
                CREATE TABLE users (id BIGINT PRIMARY KEY, name TEXT NOT NULL, email TEXT);
                CREATE TABLE orders (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), amount NUMERIC NOT NULL);
            """)
        }
    }
    afterSpec { postgres.stop() }

    val schema = Schema(
        schemas = listOf("public"),
        tables = mapOf(
            "public.users" to TableDef(
                columns = mapOf("id" to ColumnDef("bigint", false), "name" to ColumnDef("text", false), "email" to ColumnDef("text", true)),
                primaryKey = listOf("id")
            ),
            "public.orders" to TableDef(
                columns = mapOf("id" to ColumnDef("bigint", false), "user_id" to ColumnDef("bigint", false), "amount" to ColumnDef("numeric", false)),
                primaryKey = listOf("id"),
                foreignKeys = listOf(ForeignKeyDef(listOf("user_id"), "public.users", listOf("id")))
            )
        )
    )

    test("CLEAN_INSERT truncates and inserts") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("INSERT INTO users (id, name) VALUES (99, 'OldUser') ON CONFLICT DO NOTHING")
            val dataset = DataSet(mapOf(
                "public.users" to listOf(mapOf("id" to 1, "name" to "Alice", "email" to "alice@example.com")),
                "public.orders" to listOf(mapOf("id" to 10, "user_id" to 1, "amount" to 99.99))
            ))
            LoadOperation.execute(conn, dataset, schema, ForeignKeyResolver(schema), DbManglerConfig(), LoadStrategy.CLEAN_INSERT)

            val rs = conn.createStatement().executeQuery("SELECT count(*) FROM users")
            rs.next(); rs.getInt(1) shouldBe 1
        }
    }

    test("INSERT adds without truncating") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("TRUNCATE orders, users CASCADE")
            conn.createStatement().execute("INSERT INTO users (id, name) VALUES (50, 'Existing')")
            val dataset = DataSet(mapOf("public.users" to listOf(mapOf("id" to 1, "name" to "Alice"))))
            LoadOperation.execute(conn, dataset, schema, ForeignKeyResolver(schema), DbManglerConfig(), LoadStrategy.INSERT)

            val rs = conn.createStatement().executeQuery("SELECT count(*) FROM users")
            rs.next(); rs.getInt(1) shouldBe 2
        }
    }
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.LoadOperationTest" --info`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement LoadOperation**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/operation/LoadOperation.kt`:

```kotlin
package com.dbmangler.operation

import com.dbmangler.config.DbManglerConfig
import com.dbmangler.config.LoadStrategy
import com.dbmangler.dataset.DataSet
import com.dbmangler.schema.Schema
import com.dbmangler.util.ForeignKeyResolver
import java.sql.Connection

object LoadOperation {

    fun execute(
        connection: Connection, dataSet: DataSet, schema: Schema,
        fkResolver: ForeignKeyResolver, config: DbManglerConfig, strategy: LoadStrategy
    ) {
        val tables = dataSet.tables.keys.toList()
        val hasCycles = fkResolver.hasCycles(tables)

        connection.autoCommit = false
        try {
            if (hasCycles) connection.createStatement().execute("SET CONSTRAINTS ALL DEFERRED")

            if (strategy == LoadStrategy.CLEAN_INSERT) {
                for (table in fkResolver.truncateOrder(tables)) {
                    val (s, t) = table.split(".", limit = 2)
                    connection.createStatement().execute("TRUNCATE \"$s\".\"$t\" CASCADE")
                }
            }

            for (table in fkResolver.insertOrder(tables)) {
                val rows = dataSet.tables[table] ?: continue
                if (rows.isEmpty()) continue
                val (s, t) = table.split(".", limit = 2)

                for (row in rows) {
                    val filtered = row.filterKeys { !config.isColumnIgnored(table, it) }
                    val cols = filtered.keys.toList()
                    val placeholders = cols.joinToString(", ") { "?" }
                    val colNames = cols.joinToString(", ") { "\"$it\"" }
                    val sql = "INSERT INTO \"$s\".\"$t\" ($colNames) VALUES ($placeholders)"

                    connection.prepareStatement(sql).use { stmt ->
                        cols.forEachIndexed { i, col ->
                            val v = filtered[col]
                            if (v == null) stmt.setNull(i + 1, java.sql.Types.NULL) else stmt.setObject(i + 1, v)
                        }
                        stmt.executeUpdate()
                    }
                }
            }
            connection.commit()
        } catch (e: Exception) {
            connection.rollback(); throw e
        } finally {
            connection.autoCommit = true
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.LoadOperationTest" --info`
Expected: PASS — all 2 tests green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add load operation with CLEAN_INSERT and INSERT strategies"
```

---

## Task 11: Compare Operation

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/operation/CompareOperation.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/integration/CompareOperationTest.kt`

- [ ] **Step 1: Write failing integration test**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/integration/CompareOperationTest.kt`:

```kotlin
package com.dbmangler.integration

import com.dbmangler.config.DbManglerConfig
import com.dbmangler.dataset.DataSet
import com.dbmangler.operation.CompareOperation
import com.dbmangler.schema.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class CompareOperationTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name TEXT NOT NULL, email TEXT);")
            conn.createStatement().execute("INSERT INTO users VALUES (1, 'Alice', 'alice@example.com')")
            conn.createStatement().execute("INSERT INTO users VALUES (2, 'Bob', null)")
        }
    }
    afterSpec { postgres.stop() }

    val schema = Schema(
        schemas = listOf("public"),
        tables = mapOf("public.users" to TableDef(
            columns = mapOf("id" to ColumnDef("bigint", false), "name" to ColumnDef("text", false), "email" to ColumnDef("text", true)),
            primaryKey = listOf("id")
        ))
    )

    test("matching data returns success") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val expected = DataSet(mapOf("public.users" to listOf(
                mapOf("id" to 1, "name" to "Alice", "email" to "alice@example.com"),
                mapOf("id" to 2, "name" to "Bob", "email" to null)
            )))
            CompareOperation.execute(conn, expected, schema, DbManglerConfig()).matches shouldBe true
        }
    }

    test("mismatched data returns failure") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val expected = DataSet(mapOf("public.users" to listOf(
                mapOf("id" to 1, "name" to "WRONG", "email" to "alice@example.com"),
                mapOf("id" to 2, "name" to "Bob", "email" to null)
            )))
            val result = CompareOperation.execute(conn, expected, schema, DbManglerConfig())
            result.matches shouldBe false
            result.tableDiffs["public.users"]!!.columnDiffs.isNotEmpty() shouldBe true
        }
    }

    test("~ignore matcher skips column") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val expected = DataSet(mapOf("public.users" to listOf(
                mapOf("id" to 1, "name" to "Alice", "email" to "~ignore"),
                mapOf("id" to 2, "name" to "Bob", "email" to "~ignore")
            )))
            CompareOperation.execute(conn, expected, schema, DbManglerConfig()).matches shouldBe true
        }
    }
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.CompareOperationTest" --info`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement CompareOperation**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/operation/CompareOperation.kt`:

```kotlin
package com.dbmangler.operation

import com.dbmangler.assertion.*
import com.dbmangler.config.DbManglerConfig
import com.dbmangler.dataset.DataSet
import com.dbmangler.schema.Schema
import java.sql.Connection

object CompareOperation {

    fun execute(connection: Connection, expected: DataSet, schema: Schema, config: DbManglerConfig): ComparisonResult {
        val tableDiffs = mutableMapOf<String, TableDiff>()
        var allMatch = true

        for ((tableName, expectedRows) in expected.tables) {
            val tableDef = schema.tables[tableName] ?: continue
            val pk = tableDef.primaryKey
            val (s, t) = tableName.split(".", limit = 2)

            val orderBy = if (pk.isNotEmpty()) pk.joinToString(", ") { "\"$it\"" } else "1"

            val actualRows = mutableListOf<Map<String, Any?>>()
            connection.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM \"$s\".\"$t\" ORDER BY $orderBy").use { rs ->
                    val meta = rs.metaData
                    while (rs.next()) {
                        val row = mutableMapOf<String, Any?>()
                        for (i in 1..meta.columnCount) {
                            val col = meta.getColumnName(i)
                            if (!config.isColumnIgnored(tableName, col)) row[col] = rs.getObject(i)
                        }
                        actualRows.add(row)
                    }
                }
            }

            val filtered = expectedRows.map { it.filterKeys { k -> !config.isColumnIgnored(tableName, k) } }
            val diff = compareRows(filtered, actualRows, pk, config)
            if (diff != null) { tableDiffs[tableName] = diff; allMatch = false }
        }

        return if (allMatch) ComparisonResult.match() else ComparisonResult(false, tableDiffs)
    }

    private fun compareRows(expected: List<Map<String, Any?>>, actual: List<Map<String, Any?>>,
                            pk: List<String>, config: DbManglerConfig): TableDiff? {
        val missing = mutableListOf<Map<String, Any?>>()
        val extra = mutableListOf<Map<String, Any?>>()
        val diffs = mutableListOf<RowDiff>()

        if (pk.isEmpty()) {
            for (i in expected.indices) {
                if (i >= actual.size) { missing.add(expected[i]); continue }
                compareRow(expected[i], actual[i], pk, config)?.let { diffs.add(it) }
            }
            if (actual.size > expected.size) extra.addAll(actual.drop(expected.size))
        } else {
            val actualByPk = actual.associateBy { row -> pk.map { row[it] } }
            val expectedByPk = expected.associateBy { row -> pk.map { row[it] } }

            for ((k, v) in expectedByPk) {
                val act = actualByPk[k]
                if (act == null) missing.add(v) else compareRow(v, act, pk, config)?.let { diffs.add(it) }
            }
            for ((k, v) in actualByPk) { if (k !in expectedByPk) extra.add(v) }
        }

        return if (missing.isEmpty() && extra.isEmpty() && diffs.isEmpty()) null
        else TableDiff(missing, extra, diffs)
    }

    private fun compareRow(expected: Map<String, Any?>, actual: Map<String, Any?>,
                           pk: List<String>, config: DbManglerConfig): RowDiff? {
        val mismatches = mutableMapOf<String, ColumnMismatch>()
        for ((col, expVal) in expected) {
            val matcher = ColumnMatcher.forExpected(expVal, config.defaultTimestampTolerance)
            val actVal = actual[col]
            if (!matcher.matches(actVal))
                mismatches[col] = ColumnMismatch(expected = expVal, actual = actVal, message = matcher.describe(actVal))
        }
        return if (mismatches.isEmpty()) null
        else RowDiff(primaryKey = pk.associateWith { actual[it] }, columnMismatches = mismatches)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.CompareOperationTest" --info`
Expected: PASS — all 3 tests green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add compare operation with column matchers"
```

---

## Task 12: Dump Operation

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/operation/DumpOperation.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/integration/DumpOperationTest.kt`

- [ ] **Step 1: Write failing integration test**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/integration/DumpOperationTest.kt`:

```kotlin
package com.dbmangler.integration

import com.dbmangler.config.DbManglerConfig
import com.dbmangler.operation.DumpOperation
import com.dbmangler.schema.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.maps.shouldContainKey
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class DumpOperationTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name TEXT NOT NULL);")
            conn.createStatement().execute("INSERT INTO users VALUES (1, 'Alice')")
            conn.createStatement().execute("INSERT INTO users VALUES (2, 'Bob')")
        }
    }
    afterSpec { postgres.stop() }

    val schema = Schema(
        schemas = listOf("public"),
        tables = mapOf("public.users" to TableDef(
            columns = mapOf("id" to ColumnDef("bigint", false), "name" to ColumnDef("text", false)),
            primaryKey = listOf("id")
        ))
    )

    test("dumps database to in-memory dataset") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val result = DumpOperation.toDataSet(conn, schema, DbManglerConfig())
            result.tables shouldContainKey "public.users"
            result.tables["public.users"]!!.size shouldBe 2
        }
    }
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.DumpOperationTest" --info`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement DumpOperation**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/operation/DumpOperation.kt`:

```kotlin
package com.dbmangler.operation

import com.dbmangler.config.DbManglerConfig
import com.dbmangler.dataset.DataSet
import com.dbmangler.dataset.DataSetWriter
import com.dbmangler.schema.Schema
import java.nio.file.Path
import java.sql.Connection

object DumpOperation {

    fun toDataSet(connection: Connection, schema: Schema, config: DbManglerConfig): DataSet {
        val tables = mutableMapOf<String, List<Map<String, Any?>>>()

        for ((tableName, tableDef) in schema.tables) {
            val (s, t) = tableName.split(".", limit = 2)
            val pk = tableDef.primaryKey
            val orderBy = if (pk.isNotEmpty()) pk.joinToString(", ") { "\"$it\"" } else "1"

            val rows = mutableListOf<Map<String, Any?>>()
            connection.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM \"$s\".\"$t\" ORDER BY $orderBy").use { rs ->
                    val meta = rs.metaData
                    while (rs.next()) {
                        val row = mutableMapOf<String, Any?>()
                        for (i in 1..meta.columnCount) {
                            val col = meta.getColumnName(i)
                            if (!config.isColumnIgnored(tableName, col)) row[col] = rs.getObject(i)
                        }
                        rows.add(row)
                    }
                }
            }
            tables[tableName] = rows
        }
        return DataSet(tables)
    }

    fun toFile(connection: Connection, schema: Schema, config: DbManglerConfig, path: Path) {
        DataSetWriter.toFile(toDataSet(connection, schema, config), path)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.DumpOperationTest" --info`
Expected: PASS — 1 test green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add dump operation"
```

---

## Task 13: DbMangler Main Class

**Files:**
- Create: `dbmangler-core/src/main/kotlin/com/dbmangler/DbMangler.kt`
- Create: `dbmangler-core/src/test/kotlin/com/dbmangler/integration/DbManglerTest.kt`

- [ ] **Step 1: Create test resource files for integration test**

Create `dbmangler-core/src/test/resources/test-datasets/expected-after-load.json`:

```json
{
  "public.users": [
    { "id": 1, "name": "Alice", "email": "alice@example.com" },
    { "id": 2, "name": "Bob", "email": null }
  ],
  "public.orders": [
    { "id": 10, "user_id": 1, "amount": 99.99 }
  ]
}
```

Create `dbmangler-core/src/test/resources/test-datasets/expected-mismatch.json`:

```json
{
  "public.users": [
    { "id": 1, "name": "WRONG_NAME", "email": "alice@example.com" },
    { "id": 2, "name": "Bob", "email": null }
  ]
}
```

- [ ] **Step 2: Write failing integration test**

Create `dbmangler-core/src/test/kotlin/com/dbmangler/integration/DbManglerTest.kt`:

```kotlin
package com.dbmangler.integration

import com.dbmangler.DbMangler
import com.dbmangler.assertion.DataSetMismatchException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.assertions.throwables.shouldThrow
import org.testcontainers.containers.PostgreSQLContainer
import org.postgresql.ds.PGSimpleDataSource

class DbManglerTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        PGSimpleDataSource().apply {
            setURL(postgres.jdbcUrl); user = postgres.username; password = postgres.password
        }.connection.use { conn ->
            conn.createStatement().execute("""
                CREATE TABLE users (id BIGINT PRIMARY KEY, name TEXT NOT NULL, email TEXT);
                CREATE TABLE orders (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), amount NUMERIC NOT NULL);
            """)
        }
    }
    afterSpec { postgres.stop() }

    fun ds() = PGSimpleDataSource().apply {
        setURL(postgres.jdbcUrl); user = postgres.username; password = postgres.password
    }

    test("load and assertMatches round-trip") {
        val db = DbMangler(dataSource = ds(), schemaResourcePath = "test-schemas/valid-schema.json")
        db.load("test-datasets/users", "test-datasets/orders")
        db.assertMatches("test-datasets/expected-after-load")
    }

    test("dump returns in-memory dataset") {
        val db = DbMangler(dataSource = ds(), schemaResourcePath = "test-schemas/valid-schema.json")
        db.load("test-datasets/users")
        val result = db.dump()
        result shouldContainKey "public.users"
        result["public.users"]!!.size shouldBe 2
    }

    test("assertMatches throws on mismatch") {
        val db = DbMangler(dataSource = ds(), schemaResourcePath = "test-schemas/valid-schema.json")
        db.load("test-datasets/users")
        shouldThrow<DataSetMismatchException> {
            db.assertMatches("test-datasets/expected-mismatch")
        }
    }
})
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.DbManglerTest" --info`
Expected: FAIL — `DbMangler` class not found.

- [ ] **Step 4: Implement DbMangler**

Create `dbmangler-core/src/main/kotlin/com/dbmangler/DbMangler.kt`:

```kotlin
package com.dbmangler

import com.dbmangler.assertion.DataSetMismatchException
import com.dbmangler.config.DbManglerConfig
import com.dbmangler.config.LoadStrategy
import com.dbmangler.dataset.DataSetReader
import com.dbmangler.operation.CompareOperation
import com.dbmangler.operation.DumpOperation
import com.dbmangler.operation.LoadOperation
import com.dbmangler.schema.SchemaReader
import com.dbmangler.schema.SchemaValidator
import com.dbmangler.util.ForeignKeyResolver
import com.dbmangler.util.PostgresIntrospector
import java.nio.file.Path
import javax.sql.DataSource

class DbMangler(
    private val dataSource: DataSource,
    config: DbManglerConfig? = null,
    schemaResourcePath: String = "dbmangler-schema.json"
) {
    private val config: DbManglerConfig = config ?: DbManglerConfig.fromClasspath()
    private val schema = SchemaReader.fromClasspath(schemaResourcePath)
    private val fkResolver = ForeignKeyResolver(schema)

    init {
        dataSource.connection.use { conn ->
            val live = PostgresIntrospector(conn).introspect(this.config.schemas)
            SchemaValidator.validateSchemaMatch(schema, live, this.config)
        }
    }

    fun load(vararg resourcePaths: String, strategy: LoadStrategy = config.loadStrategy) {
        val dataSet = DataSetReader.fromClasspath(*resourcePaths)
        SchemaValidator.validateDataSetAgainstSchema(dataSet, schema, config)
        dataSource.connection.use { conn ->
            LoadOperation.execute(conn, dataSet, schema, fkResolver, config, strategy)
        }
    }

    fun assertMatches(resourcePath: String) {
        val expected = DataSetReader.fromClasspath(resourcePath)
        SchemaValidator.validateDataSetAgainstSchema(expected, schema, config)
        dataSource.connection.use { conn ->
            val result = CompareOperation.execute(conn, expected, schema, config)
            if (!result.matches) {
                val dumpPath = try {
                    val p = Path.of("build", "test-results", "dbmangler-actual.json")
                    DumpOperation.toFile(conn, schema, config, p); p.toString()
                } catch (_: Exception) { null }
                throw DataSetMismatchException(result, dumpPath)
            }
        }
    }

    fun dump(resourcePath: String) {
        dataSource.connection.use { conn ->
            DumpOperation.toFile(conn, schema, config, Path.of(resourcePath))
        }
    }

    fun dump(): Map<String, List<Map<String, Any?>>> {
        dataSource.connection.use { conn ->
            return DumpOperation.toDataSet(conn, schema, config).tables
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :dbmangler-core:test --tests "com.dbmangler.integration.DbManglerTest" --info`
Expected: PASS — all 3 tests green.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add DbMangler main class with load/assert/dump"
```

---

## Task 14: Gradle Plugin — AddColumn and RemoveColumn Tasks

**Files:**
- Create: `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/DbManglerPlugin.kt`
- Create: `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/AddColumnTask.kt`
- Create: `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/RemoveColumnTask.kt`
- Create: `dbmangler-gradle-plugin/src/test/kotlin/com/dbmangler/gradle/functional/AddColumnTaskTest.kt`
- Create: `dbmangler-gradle-plugin/src/test/kotlin/com/dbmangler/gradle/functional/RemoveColumnTaskTest.kt`

- [ ] **Step 1: Implement DbManglerPlugin**

Create `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/DbManglerPlugin.kt`:

```kotlin
package com.dbmangler.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class DbManglerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("dbmanglerAddColumn", AddColumnTask::class.java)
        project.tasks.register("dbmanglerRemoveColumn", RemoveColumnTask::class.java)
        project.tasks.register("dbmanglerAddTable", AddTableTask::class.java)
        project.tasks.register("dbmanglerRemoveTable", RemoveTableTask::class.java)
    }
}
```

- [ ] **Step 2: Implement AddColumnTask**

Create `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/AddColumnTask.kt`:

```kotlin
package com.dbmangler.gradle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class AddColumnTask : DefaultTask() {
    @get:Option(option = "table", description = "Table name") var table: String = ""
    @get:Option(option = "column", description = "Column name") var column: String = ""
    @get:Option(option = "type", description = "Column type") var type: String = ""
    @get:Option(option = "nullable", description = "Nullable (true/false)") var nullable: String = "true"
    @get:Option(option = "default", description = "Default value for datasets") var default: String? = null

    init { group = "dbmangler"; description = "Add a column to schema and all dataset files" }

    @TaskAction fun execute() {
        require(table.isNotBlank() && column.isNotBlank() && type.isNotBlank())
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val resDir = project.file("src/test/resources")
        val schemaFile = File(resDir, "dbmangler-schema.json")
        require(schemaFile.exists()) { "Schema file not found: ${schemaFile.absolutePath}" }

        val root = mapper.readTree(schemaFile) as ObjectNode
        val tables = root["tables"] as ObjectNode
        val qTable = resolveTable(table, root)
        val cols = (tables[qTable] as? ObjectNode ?: error("Table '$qTable' not found"))["columns"] as ObjectNode
        val colDef = mapper.createObjectNode(); colDef.put("type", type); colDef.put("nullable", nullable.toBoolean())
        cols.set<ObjectNode>(column, colDef)
        mapper.writeValue(schemaFile, root)
        logger.lifecycle("Added column '$column' to table '$qTable' in schema")

        val defVal: Any? = if (nullable.toBoolean()) null else default
        resDir.walkTopDown().filter { it.isFile && it.extension == "json" && it.name != "dbmangler-schema.json" && it.name != "dbmangler-config.json" }.forEach { file ->
            try {
                val node = mapper.readTree(file); if (!node.isObject) return@forEach
                val tbl = node[table] ?: return@forEach; if (!tbl.isArray) return@forEach
                var mod = false
                for (row in tbl) { if (row is ObjectNode && !row.has(column)) {
                    if (defVal == null) row.putNull(column) else row.put(column, defVal.toString()); mod = true
                }}
                if (mod) { mapper.writeValue(file, node); logger.lifecycle("Updated ${file.relativeTo(resDir)}") }
            } catch (_: Exception) {}
        }
    }

    private fun resolveTable(t: String, root: ObjectNode): String {
        if (t.contains(".")) return t
        val tbls = root["tables"] as ObjectNode
        val schemas = root["schemas"]?.map { it.asText() } ?: listOf("public")
        for (s in schemas) { if (tbls.has("$s.$t")) return "$s.$t" }
        return "${schemas.first()}.$t"
    }
}
```

- [ ] **Step 3: Implement RemoveColumnTask**

Create `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/RemoveColumnTask.kt`:

```kotlin
package com.dbmangler.gradle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class RemoveColumnTask : DefaultTask() {
    @get:Option(option = "table", description = "Table name") var table: String = ""
    @get:Option(option = "column", description = "Column name") var column: String = ""

    init { group = "dbmangler"; description = "Remove a column from schema and all dataset files" }

    @TaskAction fun execute() {
        require(table.isNotBlank() && column.isNotBlank())
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val resDir = project.file("src/test/resources")
        val schemaFile = File(resDir, "dbmangler-schema.json")
        require(schemaFile.exists())

        val root = mapper.readTree(schemaFile) as ObjectNode
        val qTable = resolveTable(table, root)
        ((root["tables"] as ObjectNode)[qTable] as ObjectNode)["columns"].let { (it as ObjectNode).remove(column) }
        mapper.writeValue(schemaFile, root)
        logger.lifecycle("Removed column '$column' from table '$qTable' in schema")

        resDir.walkTopDown().filter { it.isFile && it.extension == "json" && it.name != "dbmangler-schema.json" && it.name != "dbmangler-config.json" }.forEach { file ->
            try {
                val node = mapper.readTree(file); if (!node.isObject) return@forEach
                val tbl = node[table] ?: return@forEach; if (!tbl.isArray) return@forEach
                var mod = false
                for (row in tbl) { if (row is ObjectNode && row.has(column)) { row.remove(column); mod = true } }
                if (mod) { mapper.writeValue(file, node); logger.lifecycle("Updated ${file.relativeTo(resDir)}") }
            } catch (_: Exception) {}
        }
    }

    private fun resolveTable(t: String, root: ObjectNode): String {
        if (t.contains(".")) return t
        val tbls = root["tables"] as ObjectNode
        val schemas = root["schemas"]?.map { it.asText() } ?: listOf("public")
        for (s in schemas) { if (tbls.has("$s.$t")) return "$s.$t" }
        return "${schemas.first()}.$t"
    }
}
```

- [ ] **Step 4: Write functional test helpers and tests**

Create `dbmangler-gradle-plugin/src/test/kotlin/com/dbmangler/gradle/functional/TestHelpers.kt`:

```kotlin
package com.dbmangler.gradle.functional

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

fun createTempDir(prefix: String): File {
    val dir = kotlin.io.path.createTempDirectory(prefix).toFile()
    dir.deleteOnExit()
    return dir
}

fun setupGradleProject(dir: File) {
    File(dir, "settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
    File(dir, "build.gradle.kts").writeText("plugins { id(\"com.dbmangler\") }")
}

fun runGradle(projectDir: File, vararg args: String): BuildResult {
    return GradleRunner.create().withProjectDir(projectDir).withArguments(*args).withPluginClasspath().build()
}
```

Create `dbmangler-gradle-plugin/src/test/kotlin/com/dbmangler/gradle/functional/AddColumnTaskTest.kt`:

```kotlin
package com.dbmangler.gradle.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import java.io.File

class AddColumnTaskTest : FunSpec({
    test("adds column to schema and dataset files") {
        val dir = createTempDir("dbmangler-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "dbmangler-schema.json").writeText("""{"schemas":["public"],"tables":{"public.users":{"columns":{"id":{"type":"bigint","nullable":false}},"primaryKey":["id"]}}}""")
        val ds = File(res, "test/setup.json"); ds.parentFile.mkdirs()
        ds.writeText("""{"users":[{"id":1}]}""")

        val result = runGradle(dir, "dbmanglerAddColumn", "--table=users", "--column=email", "--type=text")
        result.output shouldContain "Added column"
        File(res, "dbmangler-schema.json").readText() shouldContain "\"email\""
    }

    test("adds column with default value for non-null") {
        val dir = createTempDir("dbmangler-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "dbmangler-schema.json").writeText("""{"schemas":["public"],"tables":{"public.users":{"columns":{"id":{"type":"bigint","nullable":false}},"primaryKey":["id"]}}}""")
        val ds = File(res, "test/setup.json"); ds.parentFile.mkdirs()
        ds.writeText("""{"users":[{"id":1}]}""")

        runGradle(dir, "dbmanglerAddColumn", "--table=users", "--column=status", "--type=text", "--nullable=false", "--default=active")
        ds.readText() shouldContain "\"active\""
    }
})
```

Create `dbmangler-gradle-plugin/src/test/kotlin/com/dbmangler/gradle/functional/RemoveColumnTaskTest.kt`:

```kotlin
package com.dbmangler.gradle.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class RemoveColumnTaskTest : FunSpec({
    test("removes column from schema and datasets") {
        val dir = createTempDir("dbmangler-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "dbmangler-schema.json").writeText("""{"schemas":["public"],"tables":{"public.users":{"columns":{"id":{"type":"bigint","nullable":false},"phone":{"type":"text","nullable":true}},"primaryKey":["id"]}}}""")
        val ds = File(res, "test/setup.json"); ds.parentFile.mkdirs()
        ds.writeText("""{"users":[{"id":1,"phone":"555-1234"}]}""")

        runGradle(dir, "dbmanglerRemoveColumn", "--table=users", "--column=phone")
        File(res, "dbmangler-schema.json").readText() shouldNotContain "\"phone\""
        ds.readText() shouldNotContain "\"phone\""
    }
})
```

- [ ] **Step 5: Add TestKit dependency to `dbmangler-gradle-plugin/build.gradle.kts`**

Ensure `testImplementation(gradleTestKit())` is in dependencies.

- [ ] **Step 6: Run tests**

Run: `./gradlew :dbmangler-gradle-plugin:test --info`
Expected: PASS — all 3 tests green.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add Gradle plugin with AddColumn and RemoveColumn tasks"
```

---

## Task 15: Gradle Plugin — AddTable and RemoveTable Tasks

**Files:**
- Create: `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/AddTableTask.kt`
- Create: `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/RemoveTableTask.kt`
- Create: `dbmangler-gradle-plugin/src/test/kotlin/com/dbmangler/gradle/functional/AddTableTaskTest.kt`
- Create: `dbmangler-gradle-plugin/src/test/kotlin/com/dbmangler/gradle/functional/RemoveTableTaskTest.kt`

- [ ] **Step 1: Implement AddTableTask**

Create `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/AddTableTask.kt`:

```kotlin
package com.dbmangler.gradle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class AddTableTask : DefaultTask() {
    @get:Option(option = "table", description = "Table name") var table: String = ""
    @get:Option(option = "columns", description = "Columns (name:type,...)") var columns: String = ""

    init { group = "dbmangler"; description = "Add a table to the schema file" }

    @TaskAction fun execute() {
        require(table.isNotBlank() && columns.isNotBlank())
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val schemaFile = File(project.file("src/test/resources"), "dbmangler-schema.json")
        require(schemaFile.exists())

        val root = mapper.readTree(schemaFile) as ObjectNode
        val tables = root["tables"] as ObjectNode
        val schemas = root["schemas"]?.map { it.asText() } ?: listOf("public")
        val qTable = if (table.contains(".")) table else "${schemas.first()}.$table"

        val tNode = mapper.createObjectNode()
        val cNode = mapper.createObjectNode()
        for (spec in columns.split(",")) {
            val (name, type) = spec.trim().split(":", limit = 2)
            val col = mapper.createObjectNode(); col.put("type", type); col.put("nullable", true)
            cNode.set<ObjectNode>(name, col)
        }
        tNode.set<ObjectNode>("columns", cNode); tNode.putArray("primaryKey")
        tables.set<ObjectNode>(qTable, tNode)

        mapper.writeValue(schemaFile, root)
        logger.lifecycle("Added table '$qTable' to schema")
    }
}
```

- [ ] **Step 2: Implement RemoveTableTask**

Create `dbmangler-gradle-plugin/src/main/kotlin/com/dbmangler/gradle/RemoveTableTask.kt`:

```kotlin
package com.dbmangler.gradle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class RemoveTableTask : DefaultTask() {
    @get:Option(option = "table", description = "Table name") var table: String = ""

    init { group = "dbmangler"; description = "Remove a table from schema and dataset files" }

    @TaskAction fun execute() {
        require(table.isNotBlank())
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val resDir = project.file("src/test/resources")
        val schemaFile = File(resDir, "dbmangler-schema.json")
        require(schemaFile.exists())

        val root = mapper.readTree(schemaFile) as ObjectNode
        val tables = root["tables"] as ObjectNode
        val schemas = root["schemas"]?.map { it.asText() } ?: listOf("public")
        val qTable = if (table.contains(".")) table else {
            schemas.map { "$it.$table" }.firstOrNull { tables.has(it) } ?: "${schemas.first()}.$table"
        }

        tables.remove(qTable)
        mapper.writeValue(schemaFile, root)
        logger.lifecycle("Removed table '$qTable' from schema")

        resDir.walkTopDown().filter { it.isFile && it.extension == "json" && it.name != "dbmangler-schema.json" && it.name != "dbmangler-config.json" }.forEach { file ->
            try {
                val node = mapper.readTree(file) as? ObjectNode ?: return@forEach
                if (node.has(table)) { node.remove(table); mapper.writeValue(file, node); logger.lifecycle("Removed '$table' from ${file.relativeTo(resDir)}") }
            } catch (_: Exception) {}
        }
    }
}
```

- [ ] **Step 3: Write functional tests**

Create `dbmangler-gradle-plugin/src/test/kotlin/com/dbmangler/gradle/functional/AddTableTaskTest.kt`:

```kotlin
package com.dbmangler.gradle.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import java.io.File

class AddTableTaskTest : FunSpec({
    test("adds table to schema") {
        val dir = createTempDir("dbmangler-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "dbmangler-schema.json").writeText("""{"schemas":["public"],"tables":{}}""")

        runGradle(dir, "dbmanglerAddTable", "--table=addresses", "--columns=id:bigint,street:text")
        File(res, "dbmangler-schema.json").readText() shouldContain "public.addresses"
    }
})
```

Create `dbmangler-gradle-plugin/src/test/kotlin/com/dbmangler/gradle/functional/RemoveTableTaskTest.kt`:

```kotlin
package com.dbmangler.gradle.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class RemoveTableTaskTest : FunSpec({
    test("removes table from schema and datasets") {
        val dir = createTempDir("dbmangler-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "dbmangler-schema.json").writeText("""{"schemas":["public"],"tables":{"public.addresses":{"columns":{"id":{"type":"bigint","nullable":false}},"primaryKey":["id"]}}}""")
        val ds = File(res, "test/setup.json"); ds.parentFile.mkdirs()
        ds.writeText("""{"addresses":[{"id":1}]}""")

        runGradle(dir, "dbmanglerRemoveTable", "--table=addresses")
        File(res, "dbmangler-schema.json").readText() shouldNotContain "addresses"
        ds.readText() shouldNotContain "addresses"
    }
})
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :dbmangler-gradle-plugin:test --info`
Expected: PASS — all tests green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Gradle AddTable and RemoveTable tasks"
```

---

## Task 16: Documentation

**Files:**
- Create: `README.md`
- Create: `docs/usage.md`
- Create: `docs/gradle-tasks.md`
- Create: `docs/schema.md`

- [ ] **Step 1: Write README.md**

Create `README.md` with: project overview, quick start (add dependency, create schema file, write first test), configuration reference table, links to detailed docs.

Include a complete test example:

```kotlin
class OrderServiceTest : FunSpec({
    val db = DbMangler(dataSource = testDataSource)

    test("creates order") {
        db.load("shared/reference-data", "com/example/OrderServiceTest/creates_order/setup")
        // ... exercise the system under test ...
        db.assertMatches("com/example/OrderServiceTest/creates_order/expected")
    }
})
```

- [ ] **Step 2: Write docs/usage.md**

Detailed usage guide including: initialization, `load()` with composable datasets, `assertMatches()`, `dump()`, special matchers (`~now`, `~now±PT10S`, `~ignore`, `~regex:pattern`), ignored columns, load strategies (CLEAN_INSERT, INSERT).

- [ ] **Step 3: Write docs/gradle-tasks.md**

Reference for all four tasks with full examples: `dbmanglerAddColumn` (with `--default`), `dbmanglerRemoveColumn`, `dbmanglerAddTable`, `dbmanglerRemoveTable`.

- [ ] **Step 4: Write docs/schema.md**

Schema file format, validation behavior, error messages, bootstrapping from existing DB.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "docs: add README, usage guide, gradle tasks reference, and schema docs"
```

---

## Task 17: Final Verification

- [ ] **Step 1: Run all tests**

Run: `./gradlew test --info`
Expected: All tests pass.

- [ ] **Step 2: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Tag release**

```bash
git tag -a v0.1.0-SNAPSHOT -m "Initial implementation"
```
