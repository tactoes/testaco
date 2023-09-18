package org.testaco.datatypes

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types.*

/**
 * Core interface, describing how a json type is mapped to a database type
 *
 * JT: Json type
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
sealed class TestacoType<JT>(open val name: String) {
  val type = this::class.java.simpleName
  /**
   * Read a column from the database, return a potentially null string value
   */
  abstract fun read(columnName: String, rs: ResultSet): JT?

  /**
   * Take a json value, store it in the database
   */
  abstract fun store(value: JT?, columnIndex: Int, statement: PreparedStatement)

  override fun toString(): String {
    return name
  }
}

object TestacoConverter {
  fun createDataType(sqlType: Int, sqlTypeName: String, columnName: String): TestacoType<*> {
    return if (sqlType == OTHER) {
      when (sqlTypeName) {
        "uuid" -> UuidType(columnName)
        "interval" -> IntervalType(columnName)
        "inet" -> InetType(columnName)
        "geometry" -> GeometryType(columnName)
        "citext" -> CitextType(columnName)
        else ->
          throw RuntimeException("Testaco does not support user data types out of the box.")
      }
    } else {
      types(sqlType, columnName) ?: throw IllegalStateException("No type found for sql type $sqlType")
    }
  }
  private fun types(sqlType: Int, columnName: String): TestacoType<*>? =
    when(sqlType) {
      BIT ->  BitType(columnName)
      TINYINT ->  IntType(columnName)
      SMALLINT ->  IntType(columnName)
      INTEGER ->  IntType(columnName)
      BIGINT ->  LongType(columnName)
      FLOAT ->  FloatType(columnName)
      REAL ->  FloatType(columnName)
      DOUBLE ->  DoubleType(columnName)
      NUMERIC ->  BigDecimalType(columnName)
      DECIMAL ->  BigDecimalType(columnName)
      CHAR ->  StringType(columnName)
      VARCHAR ->  StringType(columnName)
      LONGVARCHAR ->  StringType(columnName)
      DATE ->  DateType(columnName)
      TIME ->  TimeType(columnName)
      TIMESTAMP ->  TimestampType(columnName)
      BINARY ->  BinaryType(columnName)
      VARBINARY ->  BinaryType(columnName)
      LONGVARBINARY ->  BinaryType(columnName)
      NULL ->  NullType(columnName)
      JAVA_OBJECT ->  JavaObjectType(columnName)
      BLOB ->  BlobType(columnName)
      CLOB ->  ClobType(columnName)
      BOOLEAN ->  BooleanType(columnName)
      NCHAR ->  StringType(columnName)
      NVARCHAR ->  StringType(columnName)
      LONGNVARCHAR ->  StringType(columnName)
      TIME_WITH_TIMEZONE ->  TimeType(columnName)
      TIMESTAMP_WITH_TIMEZONE ->  TimestampType(columnName)
      else -> null
    }
}

abstract class TestacoBooleanType(name: String) : TestacoType<Boolean?>(name) {
  override fun read(columnName: String, rs: ResultSet): Boolean? {
    val b = rs.getBoolean(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      b
    }
  }
}

