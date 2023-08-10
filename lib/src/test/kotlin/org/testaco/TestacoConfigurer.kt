package org.testaco

data class TestacoConfiguration(val databases: List<TestacoDatabase>, val datadir: String = "classpath:/db/testaco")

data class TestacoDatabase(
    val dataSource: String, // name of spring bean holding data source
    val schema: String, //name of schema in database
    val tableConfig: List<TestacoTableConfig>,
)

data class TestacoSchema(val tables: List<TestacoTable>)

data class TestacoTable(val name: String, val columns: List<TestacoColumn>)

data class TestacoColumn(val name: String)

interface TestacoTableConfig {
    val tableName: String
}

data class IgnoredTable(override val tableName: String) : TestacoTableConfig