package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import java.sql.PreparedStatement
import java.sql.ResultSet

data class NullType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): String? {
    throw IllegalStateException("Don't know how to handle null types. Please raise a request with your actual use case.")
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    throw IllegalStateException("Don't know how to handle null types. Please raise a request with your actual use case.")
  }
}