package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BigIntegerNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ShortNode
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class BigDecimalType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<BigDecimal>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): BigDecimal? {
    val i = rs.getBigDecimal(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      i
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.NUMERIC)
    } else {
      when (value) {
        is BigIntegerNode -> statement.setBigDecimal(columnIndex, value.bigIntegerValue().toBigDecimal())
        is LongNode -> statement.setBigDecimal(columnIndex, value.longValue().toBigDecimal())
        is IntNode -> statement.setBigDecimal(columnIndex, value.intValue().toBigDecimal())
        is ShortNode -> statement.setBigDecimal(columnIndex, value.shortValue().toInt().toBigDecimal())
        else -> throw IllegalStateException("Big decimal database types require json data sets to store values as Short, Int, Long, or BigIntegerNodes")
      }
    }
  }
}