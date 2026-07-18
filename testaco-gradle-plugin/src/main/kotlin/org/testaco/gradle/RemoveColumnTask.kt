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
import org.gradle.api.tasks.options.Option
import java.io.File

open class RemoveColumnTask : DefaultTask() {
    private var tableValue: String = ""
    private var columnValue: String = ""

    @Option(option = "table", description = "Table name")
    fun setTable(value: String) { tableValue = value }

    @Option(option = "column", description = "Column name")
    fun setColumn(value: String) { columnValue = value }

    init { group = "testaco"; description = "Remove a column from schema and all dataset files" }

    @TaskAction fun execute() {
        InputValidation.validateTableName(tableValue)
        InputValidation.validateColumnName(columnValue)

        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val resDir = project.file("src/test/resources")
        val schemaFile = File(resDir, "testaco-schema.json")
        require(schemaFile.exists()) { "Schema file not found: ${schemaFile.absolutePath}" }

        val root = mapper.readTree(schemaFile) as ObjectNode
        val qTable = resolveTable(tableValue, root)
        ((root["tables"] as ObjectNode)[qTable] as ObjectNode)["columns"].let { (it as ObjectNode).remove(columnValue) }
        mapper.writeValue(schemaFile, root)
        logger.lifecycle("Removed column '$columnValue' from table '$qTable' in schema")

        resDir.walkTopDown().filter { it.isFile && it.extension == "json" && it.name != "testaco-schema.json" && it.name != "testaco-config.json" }.forEach { file ->
            try {
                val node = mapper.readTree(file); if (!node.isObject) return@forEach
                val tbl = node[tableValue] ?: return@forEach; if (!tbl.isArray) return@forEach
                var mod = false
                for (row in tbl) { if (row is ObjectNode && row.has(columnValue)) { row.remove(columnValue); mod = true } }
                if (mod) { mapper.writeValue(file, node); logger.lifecycle("Updated ${file.relativeTo(resDir)}") }
            } catch (e: Exception) {
                logger.warn("Failed to update dataset file ${file.relativeTo(resDir)}: ${e.message}")
            }
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
