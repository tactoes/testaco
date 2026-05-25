package org.testaco.gradle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class RemoveTableTask : DefaultTask() {
    private var tableValue: String = ""

    @Option(option = "table", description = "Table name")
    fun setTable(value: String) { tableValue = value }

    init { group = "testaco"; description = "Remove a table from schema and dataset files" }

    @TaskAction fun execute() {
        require(tableValue.isNotBlank())
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val resDir = project.file("src/test/resources")
        val schemaFile = File(resDir, "testaco-schema.json")
        require(schemaFile.exists())

        val root = mapper.readTree(schemaFile) as ObjectNode
        val tables = root["tables"] as ObjectNode
        val schemas = root["schemas"]?.map { it.asText() } ?: listOf("public")
        val qTable = if (tableValue.contains(".")) tableValue else {
            schemas.map { "$it.$tableValue" }.firstOrNull { tables.has(it) } ?: "${schemas.first()}.$tableValue"
        }

        tables.remove(qTable)
        mapper.writeValue(schemaFile, root)
        logger.lifecycle("Removed table '$qTable' from schema")

        resDir.walkTopDown().filter { it.isFile && it.extension == "json" && it.name != "testaco-schema.json" && it.name != "testaco-config.json" }.forEach { file ->
            try {
                val node = mapper.readTree(file) as? ObjectNode ?: return@forEach
                if (node.has(tableValue)) { node.remove(tableValue); mapper.writeValue(file, node); logger.lifecycle("Removed '$tableValue' from ${file.relativeTo(resDir)}") }
            } catch (_: Exception) {}
        }
    }
}
