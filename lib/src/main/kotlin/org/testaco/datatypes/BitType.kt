package org.testaco.datatypes

import org.testaco.datatypes.TestacoBooleanType
import java.sql.PreparedStatement
import java.sql.Types

data class BitType(override val name: String) : TestacoBooleanType(name) {
  override fun store(value: Boolean?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.BIT)
    } else {
      statement.setBoolean(columnIndex, value)
    }
  }
}