package org.testaco.datatypes

import org.testaco.ext.postgresql.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Core interface, describing how a json type is mapped to a database type
 *
 * JT: Json type
 */
sealed interface TestacoConverter<JT> {
  /**
   * Read a column from the database, return a potentially null string value
   */
  fun read(columnName: String, rs: ResultSet): JT?

  /**
   * Take a json value, store it in the database
   */
  fun store(value: JT, columnIndex: Int, statement: PreparedStatement)

  companion object {
    override fun createDataType(sqlType: Int, sqlTypeName: String): TestacoConverter<*> {
      if (sqlType == Types.OTHER) {
        // Treat Postgresql UUID types as VARCHARS
        if ("uuid" == sqlTypeName) {
          return UuidType()
        } else if ("interval" == sqlTypeName) {
          return IntervalType()
        } else if ("inet" == sqlTypeName) {
          return InetType()
        } else if ("geometry" == sqlTypeName) {
          return GeometryType()
        } else if ("citext" == sqlTypeName) {
          return CitextType()
        } else {
          throw RuntimeException("Testaco does not support user data types out of the box. Please implement your own data type factory")
        }
      } else if (sqlType == Types.BIGINT && "oid" == sqlTypeName) {
        return PostgreSQLOidDataType()
      }
      return super.createDataType(sqlType, sqlTypeName)
    }
  }
}

interface TestacoStringConverter: TestacoConverter<String>
interface TestacoNumberConverter: TestacoConverter<Number>
interface TestacoBooleanConverter: TestacoConverter<Boolean>

