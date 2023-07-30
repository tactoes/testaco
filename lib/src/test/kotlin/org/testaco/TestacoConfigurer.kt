package org.testaco

data class TestacoConfiguration(val databases: List<TestacoDatabase>, val datadir: String = "classpath:/db/testaco")

data class TestacoDatabase(
    val dataSource: String, // name of spring bean holding data source
    val schema: String, //name of schema in database
)

data class TestacoSchema(val tables: List<TestacoTable>)

data class TestacoTable(val name: String, val columns: List<TestacoColumn>)

data class TestacoColumn(val name: String)