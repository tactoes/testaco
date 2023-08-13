package org.testaco

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class TestacoConfiguration(val databases: List<TestacoDatabase>, val datadir: String = "classpath:/db/testaco")

data class TestacoDatabase(
    val dataSource: String, // name of spring bean holding data source
    val schema: String, //name of schema in database
)

data class TestacoSchema(val tables: List<TestacoTable>, @JsonIgnore val filename: String?)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes(*arrayOf( JsonSubTypes.Type(IgnoredTable::class), JsonSubTypes.Type(Table::class)))
interface TestacoTable {
    val tableName: String
}

data class Table(override val tableName: String, val columns: List<TestacoColumn>) : TestacoTable

data class TestacoColumn(val name: String)

data class IgnoredTable(override val tableName: String) : TestacoTable