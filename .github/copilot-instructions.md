# Copilot instructions for PgFixtures repository

Purpose
- Help future Copilot sessions understand build, test, and repository-specific conventions for pgfixtures.

Build, test, and run
- Build everything: ./gradlew build
- Run all tests: ./gradlew test
- Run a single module tests: ./gradlew :testaco-core:test or ./gradlew :testaco-gradle-plugin:test
- Run a single test class (pattern): ./gradlew :testaco-core:test --tests "*OrderServiceTest"
- Run a single test method (fully-qualified): ./gradlew :testaco-core:test --tests "com.example.OrderServiceTest.createOrder"
- Gradle args: add --no-parallel if you need sequential test execution.

Notes about linting/formatting
- No dedicated linter configured. Use Kotlin compiler checks via ./gradlew build and your IDE.
- Kotlin style: kotlin.code.style=official (see gradle.properties)

High-level architecture
- Multi-module Gradle project (root: testaco)
  - testaco-core: the Kotlin library providing PgFixtures API, dataset matchers, validation and dump/assert logic.
  - testaco-gradle-plugin: Gradle plugin that provides schema/dataset management tasks (pgfixturesAddColumn, pgfixturesRemoveColumn, pgfixturesAddTable, pgfixturesRemoveTable).
- Tests use Kotest on JUnit platform and connect to PostgreSQL via PGSimpleDataSource.
- Test datasets and schema live under src/test/resources.
  - Schema: src/test/resources/testaco-schema.json (default)
  - Optional config: src/test/resources/testaco-config.json
  - Datasets: organized by test class/method, e.g. src/test/resources/com/example/OrderServiceTest/creates_order/{setup.json,expected.json}
- On PgFixtures instantiation the library validates schema ↔ live database bidirectionally.

Key conventions (repo-specific)
- Dataset layout: group datasets under src/test/resources by test class and test case name.
- Composable datasets: call db.load("shared/ref-data", "com/example/Test/setup") to merge sources.
- Special matchers in expected JSON:
  - ~now, ~now±PT10S for timestamps
  - ~ignore to skip columns
  - ~regex:<pattern> for pattern matching
- Default schema path: src/test/resources/testaco-schema.json unless overridden via PgFixtures constructor.
- Use provided Gradle tasks to change schema and update datasets (they modify schema + dataset JSONs consistently).
- Quote PostgreSQL types containing spaces (e.g., "timestamp with time zone") when passing via CLI.
- When adding/removing schema elements: commit schema and dataset changes together.
- Debugging failing assertions: inspect build/test-results/testaco/<test-path>/actual.json (auto-dumped) and use diff against expected JSON.

Environment & toolchain
- Kotlin: declared plugin kotlin("jvm") 2.1.20
- JVM target: Java 23 (jvmToolchain 25, jvmTarget 23)
- Requirements: Kotlin 2.1+, Java 23+, PostgreSQL 12+, Kotest 5+

Where to look next for context
- README.md for usage examples
- docs/usage.md, docs/gradle-tasks.md, docs/schema.md, docs/debugging.md for detailed behavior and examples

If you want edits
- Suggest any additional commands or repository-specific notes to include (CI targets, IDE tasks, or non-standard workflows).

