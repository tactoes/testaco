package org.testaco.datatypes

import java.sql.ResultSet

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
  fun store(value: JT, columnName: String, rs: ResultSet)
}

interface TestacoStringConverter: TestacoConverter<String>
interface TestacoNumberConverter: TestacoConverter<Number>
interface TestacoBooleanConverter: TestacoConverter<Boolean>
