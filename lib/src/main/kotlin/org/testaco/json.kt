package org.testaco

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

interface TestacoSpecial

/**
 * The value is stored in serialized JSON format
 */

data class Column(val name: String, val value: JsonNode?)
@JvmInline value class Pk(val values: List<Column>) {
  override fun toString(): String {
    return values.map { it.value.toString() }.joinToString(",")
  }
}
data class Row(val columns: Set<Column>) {
  fun pk(): Pk = Pk(columns.filter{ it.name == "id"})
}

data class TTable(val name: String, val rows: Set<Row>)
data class DataSet(val tables: List<TTable>) {
  fun orderedByDataSchema(tableOrder: List<String>): DataSet {
    val tablesByName: Map<String, TTable> = tables.associateBy { it.name }
    return DataSet(tableOrder.mapNotNull { tablesByName.getValue(it) })
  }
}

class DataSetSerializer : JsonSerializer<DataSet>() {
  override fun serialize(value: DataSet?, gen: JsonGenerator?, serializers: SerializerProvider?) {
    if (value == null) return
    if (gen == null) return
    gen.writeStartObject()
    value.tables.forEach { table ->
      gen.writeArrayFieldStart(table.name)
      table.rows.forEach { row ->
        gen.writeStartObject()
        row.columns.map { column ->
          gen.writeObjectField(column.name, column.value) }
        gen.writeEndObject()
      }
      gen.writeEndArray()
    }
    gen.writeEndObject()
  }
}