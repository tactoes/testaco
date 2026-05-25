/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.testaco.schema

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
