package org.testaco.datatypes

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.NullNode
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types.*

/**
 * Core interface, describing how a json type is mapped to a database type
 *
 * JT: Json type
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
sealed class TestacoType<JT>(open val name: String, open val sqlType: Int, open val sqlTypeName: String) {
  val type = this::class.java.simpleName
  /**
   * Read a column from the database, return a potentially null string value
   */
  abstract fun read(columnName: String, rs: ResultSet): JsonNode

  /**
   * Take a json value, store it in the database
   */
  abstract fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement)

  override fun toString(): String {
    return name
  }
}

object TestacoConverter {
  fun createDataType(sqlType: Int, sqlTypeName: String, columnName: String): TestacoType<*> {
    return if (sqlType == OTHER) {
      when (sqlTypeName) {
        "uuid" -> UuidType(columnName, sqlType, sqlTypeName)
        "interval" -> IntervalType(columnName, sqlType, sqlTypeName)
        "inet" -> InetType(columnName, sqlType, sqlTypeName)
        "geometry" -> GeometryType(columnName, sqlType, sqlTypeName)
        "citext" -> CitextType(columnName, sqlType, sqlTypeName)
        else ->
          throw RuntimeException("Testaco does not support user data types out of the box.")
      }
    } else {
      types(sqlType, sqlTypeName, columnName) ?: throw IllegalStateException("No type found for sql type $sqlType")
    }
  }
  private fun types(sqlType: Int, sqlTypeName: String, columnName: String): TestacoType<*>? =
    when(sqlType) {
      BIT ->  BitType(columnName, sqlType, sqlTypeName)
      TINYINT ->  IntType(columnName, sqlType, sqlTypeName)
      SMALLINT ->  IntType(columnName, sqlType, sqlTypeName)
      INTEGER ->  IntType(columnName, sqlType, sqlTypeName)
      BIGINT ->  LongType(columnName, sqlType, sqlTypeName)
      FLOAT ->  FloatType(columnName, sqlType, sqlTypeName)
      REAL ->  FloatType(columnName, sqlType, sqlTypeName)
      DOUBLE ->  DoubleType(columnName, sqlType, sqlTypeName)
      NUMERIC ->  BigDecimalType(columnName, sqlType, sqlTypeName)
      DECIMAL ->  BigDecimalType(columnName, sqlType, sqlTypeName)
      CHAR ->  StringType(columnName, sqlType, sqlTypeName)
      VARCHAR ->  StringType(columnName, sqlType, sqlTypeName)
      LONGVARCHAR ->  StringType(columnName, sqlType, sqlTypeName)
      DATE ->  DateType(columnName, sqlType, sqlTypeName)
      TIME ->  TimeType(columnName, sqlType, sqlTypeName)
      TIMESTAMP ->  TimestampType(columnName, sqlType, sqlTypeName)
      BINARY ->  BinaryType(columnName, sqlType, sqlTypeName)
      VARBINARY ->  BinaryType(columnName, sqlType, sqlTypeName)
      LONGVARBINARY ->  BinaryType(columnName, sqlType, sqlTypeName)
      NULL ->  NullType(columnName, sqlType, sqlTypeName)
      JAVA_OBJECT ->  JavaObjectType(columnName, sqlType, sqlTypeName)
      BLOB ->  BlobType(columnName, sqlType, sqlTypeName)
      CLOB ->  ClobType(columnName, sqlType, sqlTypeName)
      BOOLEAN ->  BooleanType(columnName, sqlType, sqlTypeName)
      NCHAR ->  StringType(columnName, sqlType, sqlTypeName)
      NVARCHAR ->  StringType(columnName, sqlType, sqlTypeName)
      LONGNVARCHAR ->  StringType(columnName, sqlType, sqlTypeName)
      TIME_WITH_TIMEZONE ->  TimeType(columnName, sqlType, sqlTypeName)
      TIMESTAMP_WITH_TIMEZONE ->  TimestampType(columnName, sqlType, sqlTypeName)
      else -> null
    }
}

abstract class TestacoBooleanType(name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<Boolean?>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val b = rs.getBoolean(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      BooleanNode.valueOf(b)
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, sqlType)
    } else {
      when (value) {
        is BooleanNode -> statement.setBoolean(columnIndex, value.booleanValue())
        else -> error("Boolean database types require json data sets to store values as BooleanNodes")
      }
    }
  }
}

