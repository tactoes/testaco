package org.testaco

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.testaco.datatypes.TestacoConverter

data class TestacoConfiguration(val databases: List<TestacoDatabase>, val datadir: String = "classpath:/db/testaco")

data class TestacoDatabase(
    val dataSource: String, // name of spring bean holding data source
    val schema: String, //name of schema in database
)

data class TestacoSchema(val tables: List<TestacoTable>, @JsonIgnore val filename: String?) {
    val referenceTableList: List<String> by lazy { tables.filter { it is Table }.map { it.tableName } }
    fun find(name: String): TestacoTable? = tables.find { it.tableName == name }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes(*arrayOf( JsonSubTypes.Type(IgnoredTable::class), JsonSubTypes.Type(Table::class)))
sealed interface TestacoTable {
    val tableName: String
}

data class Table(override val tableName: String, val columns: List<TestacoColumn>) : TestacoTable

data class TestacoColumn(val name: String, val converter: TestacoConverter<*>) {
    override fun toString(): String {
        return name
    }
}

data class IgnoredTable(override val tableName: String) : TestacoTable