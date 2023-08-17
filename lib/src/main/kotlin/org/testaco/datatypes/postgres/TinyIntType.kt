package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoIntegerType
import java.sql.PreparedStatement
import java.sql.Types

class TinyIntType : TestacoIntegerType {
  override fun store(value: Int?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TINYINT)
    } else {
      statement.setInt(columnIndex, value)
    }
  }
}