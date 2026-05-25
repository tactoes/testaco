package org.testaco.gradle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class AddColumnTask : DefaultTask() {
    private var tableValue: String = ""
    private var columnValue: String = ""
    private var typeValue: String = ""
    private var nullableValue: String = "true"
    private var defaultValue: String? = null

    @Option(option = "table", description = "Table name")
    fun setTable(value: String) { tableValue = value }

    @Option(option = "column", description = "Column name")
    fun setColumn(value: String) { columnValue = value }

    @Option(option = "type", description = "Column type")
    fun setType(value: String) { typeValue = value }

    @Option(option = "nullable", description = "Nullable (true/false)")
    fun setNullable(value: String) { nullableValue = value }

    @Option(option = "default", description = "Default value for datasets")
    fun setDefault(value: String) { defaultValue = value }

    init { group = "testaco"; description = "Add a column to schema and all dataset files" }

    @TaskAction fun execute() {
        require(tableValue.isNotBlank() && columnValue.isNotBlank() && typeValue.isNotBlank())
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val resDir = project.file("src/test/resources")
        val schemaFile = File(resDir, "testaco-schema.json")
        require(schemaFile.exists()) { "Schema file not found: ${schemaFile.absolutePath}" }

        val root = mapper.readTree(schemaFile) as ObjectNode
        val tables = root["tables"] as ObjectNode
        val qTable = resolveTable(tableValue, root)
        val cols = (tables[qTable] as? ObjectNode ?: error("Table '$qTable' not found"))["columns"] as ObjectNode
        val colDef = mapper.createObjectNode(); colDef.put("type", typeValue); colDef.put("nullable", nullableValue.toBoolean())
        cols.set<ObjectNode>(columnValue, colDef)
        mapper.writeValue(schemaFile, root)
        logger.lifecycle("Added column '$columnValue' to table '$qTable' in schema")

        val defVal: Any? = if (nullableValue.toBoolean()) null else defaultValue
        resDir.walkTopDown().filter { it.isFile && it.extension == "json" && it.name != "testaco-schema.json" && it.name != "testaco-config.json" }.forEach { file ->
            try {
                val node = mapper.readTree(file); if (!node.isObject) return@forEach
                val tbl = node[tableValue] ?: return@forEach; if (!tbl.isArray) return@forEach
                var mod = false
                for (row in tbl) { if (row is ObjectNode && !row.has(columnValue)) {
                    if (defVal == null) row.putNull(columnValue) else row.put(columnValue, defVal.toString()); mod = true
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
