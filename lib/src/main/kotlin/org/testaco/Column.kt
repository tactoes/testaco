package org.testaco

import com.fasterxml.jackson.databind.JsonNode

interface TestacoSpecial

/**
 * The value is stored in serialized JSON format
 */
data class Column(val name: String, val value: JsonNode?)
data class Row(val columns: List<Column>)
data class TTable(val name: String, val rows: List<Row>)
data class DataSet(val tables: List<TTable>)