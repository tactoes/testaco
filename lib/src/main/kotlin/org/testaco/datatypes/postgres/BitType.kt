package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoBooleanType
import java.sql.PreparedStatement
import java.sql.Types

class BitType : TestacoBooleanType {
  override fun store(value: Boolean?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.BIT)
    } else {
      statement.setBoolean(columnIndex, value)
    }
  }
}