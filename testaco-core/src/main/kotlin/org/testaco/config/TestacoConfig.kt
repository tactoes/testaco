/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.testaco.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.Duration

data class TestacoConfig(
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

        fun fromClasspath(resourcePath: String = "testaco-config.json"): TestacoConfig {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
                ?: return TestacoConfig()
            return fromJson(mapper.readTree(stream))
        }

        fun fromJson(node: JsonNode): TestacoConfig {
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

            return TestacoConfig(
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
