package org.testaco.datatypes

import java.sql.PreparedStatement
import java.sql.ResultSet

data class NullType(override val name: String) : TestacoType<String>(name) {
  override fun read(columnName: String, rs: ResultSet): String? {
    throw IllegalStateException("Don't know how to handle null types. Please raise a request with your actual use case.")
  }

  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    throw IllegalStateException("Don't know how to handle null types. Please raise a request with your actual use case.")
  }
}