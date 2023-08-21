package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class BigDecimalType : TestacoType<BigDecimal> {
  override fun read(columnName: String, rs: ResultSet): BigDecimal? {
    val i = rs.getBigDecimal(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      i
    }
  }

  override fun store(value: BigDecimal?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.NUMERIC)
    } else {
      statement.setBigDecimal(columnIndex, value)
    }
  }
}