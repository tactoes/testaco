package org.testaco.datatypes

import org.testaco.datatypes.postgres.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types.*

/**
 * Core interface, describing how a json type is mapped to a database type
 *
 * JT: Json type
 */
interface TestacoType<JT> {
  /**
   * Read a column from the database, return a potentially null string value
   */
  fun read(columnName: String, rs: ResultSet): JT?

  /**
   * Take a json value, store it in the database
   */
  fun store(value: JT?, columnIndex: Int, statement: PreparedStatement)
}

object TestacoConverter {
  fun createDataType(sqlType: Int, sqlTypeName: String): TestacoType<*> {
    if (sqlType == OTHER) {
      return when (sqlTypeName) {
        "uuid" -> { UuidType() }
        "interval" -> { IntervalType() }
        "inet" -> { InetType() }
        "geometry" -> { GeometryType() }
        "citext" -> { CitextType() }
        else -> {
          throw RuntimeException("Testaco does not support user data types out of the box.")
        }
      }
    } else {
      return types[sqlType] ?: throw IllegalStateException("No type found for sql type $sqlType")
    }
  }
  private val types : Map<Int, TestacoType<*>> = mapOf(
    BIT to BitType(),
    TINYINT to IntType(),
    SMALLINT to IntType(),
    INTEGER to IntType(),
    BIGINT to LongType(),
    FLOAT to FloatType(),
    REAL to FloatType(),
    DOUBLE to DoubleType(),
    NUMERIC to BigDecimalType(),
    DECIMAL to BigDecimalType(),
    CHAR to StringType(),
    VARCHAR to StringType(),
    LONGVARCHAR to StringType(),
    DATE to DateType(),
    TIME to TimeType(),
    TIMESTAMP to TimestampType(),
    BINARY to BinaryType(),
    VARBINARY to BinaryType(),
    LONGVARBINARY to BinaryType(),
    NULL to NullType(),
    JAVA_OBJECT to JavaObjectType(),
    BLOB to BlobType(),
    CLOB to ClobType(),
    BOOLEAN to BooleanType(),
    NCHAR to StringType(),
    NVARCHAR to StringType(),
    LONGNVARCHAR to StringType(),
    TIME_WITH_TIMEZONE to TimeType(),
    TIMESTAMP_WITH_TIMEZONE to TimestampType(),
  )
}

interface TestacoBooleanType: TestacoType<Boolean?> {
  override fun read(columnName: String, rs: ResultSet): Boolean? {
    val b = rs.getBoolean(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      b
    }
  }
}

