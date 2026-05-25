package org.testaco.gradle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class AddTableTask : DefaultTask() {
    private var tableValue: String = ""
    private var columnsValue: String = ""

    @Option(option = "table", description = "Table name")
    fun setTable(value: String) { tableValue = value }

    @Option(option = "columns", description = "Columns (name:type,...)")
    fun setColumns(value: String) { columnsValue = value }

    init { group = "testaco"; description = "Add a table to the schema file" }

    @TaskAction fun execute() {
        require(tableValue.isNotBlank() && columnsValue.isNotBlank())
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val schemaFile = File(project.file("src/test/resources"), "testaco-schema.json")
        require(schemaFile.exists())

        val root = mapper.readTree(schemaFile) as ObjectNode
        val tables = root["tables"] as ObjectNode
        val schemas = root["schemas"]?.map { it.asText() } ?: listOf("public")
        val qTable = if (tableValue.contains(".")) tableValue else "${schemas.first()}.$tableValue"

        val tNode = mapper.createObjectNode()
        val cNode = mapper.createObjectNode()
        for (spec in columnsValue.split(",")) {
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
