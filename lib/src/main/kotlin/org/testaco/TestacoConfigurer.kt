package org.testaco

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.testaco.datatypes.TestacoType

data class TestacoConfiguration(val databases: List<TestacoDatabase>, val datadir: String = "classpath:/db/testaco")

data class TestacoDatabase(
    val dataSource: String, // name of spring bean holding data source
    val schema: String, //name of schema in database
)

data class TestacoSchema(val tables: List<TestacoTable>, @JsonIgnore val filename: String?) {
    val referenceTableList: List<String> by lazy { tables.filter { it is Table }.map { it.tableName } }
    fun find(name: String): TestacoTable? = tables.find { it.tableName == name }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(*arrayOf( JsonSubTypes.Type(IgnoredTable::class), JsonSubTypes.Type(Table::class)))
sealed interface TestacoTable {
    fun pks(table: TTable): Set<String> {
        //TODO: Find PK columns algorithmically
        return table.rows.map { row -> row.columns.find { col -> col.name == "id" } }.map { "${it!!.value}" }.toSet()
    }

    val tableName: String
}

data class Table(override val tableName: String, val columns: List<TestacoType<*>>) : TestacoTable

data class IgnoredTable(override val tableName: String) : TestacoTable