/*
 * Copyright 2026 Geir Hedemark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testaco.gradle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Initializes a new Testaco project structure with example files.
 *
 * Usage: ./gradlew testacoInit
 *
 * Creates:
 * - src/test/resources/testaco-schema.json (example schema)
 * - src/test/resources/testaco-config.json (example config)
 * - src/test/resources/example/setup.json (example dataset)
 * - src/test/resources/example/expected.json (example expected dataset)
 */
open class TestacoInitTask : DefaultTask() {
    init { group = "testaco"; description = "Initialize a new Testaco project with example files" }

    @TaskAction fun execute() {
        val resDir = project.file("src/test/resources")
        resDir.mkdirs()

        val schemaFile = File(resDir, "testaco-schema.json")
        if (!schemaFile.exists()) {
            val schema = createExampleSchema()
            ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(schemaFile, schema)
            logger.lifecycle("Created example schema: ${schemaFile.absolutePath}")
        } else {
            logger.lifecycle("Schema already exists: ${schemaFile.absolutePath}")
        }

        val configFile = File(resDir, "testaco-config.json")
        if (!configFile.exists()) {
            val config = createExampleConfig()
            ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(configFile, config)
            logger.lifecycle("Created example config: ${configFile.absolutePath}")
        }

        val exampleDir = File(resDir, "example")
        exampleDir.mkdirs()

        val setupFile = File(exampleDir, "setup.json")
        if (!setupFile.exists()) {
            val setup = createExampleSetup()
            ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(setupFile, setup)
            logger.lifecycle("Created example setup dataset: ${setupFile.absolutePath}")
        }

        val expectedFile = File(exampleDir, "expected.json")
        if (!expectedFile.exists()) {
            val expected = createExampleExpected()
            ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(expectedFile, expected)
            logger.lifecycle("Created example expected dataset: ${expectedFile.absolutePath}")
        }

        logger.lifecycle("")
        logger.lifecycle("Testaco project initialized! Next steps:")
        logger.lifecycle("1. Update src/test/resources/testaco-schema.json with your database schema")
        logger.lifecycle("2. Create test datasets under src/test/resources/com/example/YourTest/")
        logger.lifecycle("3. See https://github.com/tactoes/testaco/blob/main/README.md for examples")
    }

    private fun createExampleSchema(): ObjectNode {
        val mapper = ObjectMapper()
        val root = mapper.createObjectNode()
        root.putArray("schemas").add("public")

        val tables = mapper.createObjectNode()
        val users = mapper.createObjectNode()
        val usersColumns = mapper.createObjectNode()
        usersColumns.set<ObjectNode>("id", mapper.createObjectNode().put("type", "bigint").put("nullable", false))
        usersColumns.set<ObjectNode>("name", mapper.createObjectNode().put("type", "text").put("nullable", false))
        users.set<ObjectNode>("columns", usersColumns)
        users.putArray("primaryKey").add("id")

        tables.set<ObjectNode>("public.users", users)
        root.set<ObjectNode>("tables", tables)
        return root
    }

    private fun createExampleConfig(): ObjectNode {
        val mapper = ObjectMapper()
        val config = mapper.createObjectNode()
        config.put("loadStrategy", "CLEAN_INSERT")
        config.put("assertionTolerance", "PT1S")
        config.set<ObjectNode>("ignoredColumns", mapper.createObjectNode())
        return config
    }

    private fun createExampleSetup(): ObjectNode {
        val mapper = ObjectMapper()
        val root = mapper.createObjectNode()
        val users = root.putArray("users")
        val user1 = mapper.createObjectNode()
        user1.put("id", 1)
        user1.put("name", "Alice")
        users.add(user1)
        return root
    }

    private fun createExampleExpected(): ObjectNode {
        val mapper = ObjectMapper()
        val root = mapper.createObjectNode()
        val users = root.putArray("users")
        val user1 = mapper.createObjectNode()
        user1.put("id", 1)
        user1.put("name", "Alice")
        users.add(user1)
        return root
    }
}
