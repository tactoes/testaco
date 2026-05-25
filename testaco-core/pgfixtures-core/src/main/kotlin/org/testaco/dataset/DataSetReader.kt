package org.testaco.dataset

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
