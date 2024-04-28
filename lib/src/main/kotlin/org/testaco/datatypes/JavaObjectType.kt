package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet

data class JavaObjectType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): String? {
    throw IllegalStateException("Don't know how to handle java object type. Please raise a request with your actual use case.")
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    throw IllegalStateException("Don't know how to handle java object type. Please raise a request with your actual use case.")
  }
}