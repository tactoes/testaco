package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet

open class JavaObjectType : TestacoType<String> {
  override fun read(columnName: String, rs: ResultSet): String? {
    throw IllegalStateException("Don't know how to handle java object type. Please raise a request with your actual use case.")
  }

  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    throw IllegalStateException("Don't know how to handle java object type. Please raise a request with your actual use case.")
  }
}