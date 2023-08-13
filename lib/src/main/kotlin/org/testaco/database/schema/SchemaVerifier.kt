package org.testaco.database.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.sql.DatabaseMetaData
import org.junit.jupiter.api.fail
import org.testaco.*

object SchemaVerifier {
  private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

  fun verifySchema(databaseMetaData: DatabaseMetaData, referenceSchema: TestacoSchema) {
    databaseMetaData.getTables(null, null, null, arrayOf<String>("TABLE")).use { resultSet ->
      while (resultSet.next()) {
        val tableName: String =
          resultSet.getString("TABLE_NAME")
            ?: fail("Don't know how to cope if metadata does not return tablename, panic!")
        val schema: String? = resultSet.getString("TABLE_SCHEM")

        val referenceTable: TestacoTable? =
          referenceSchema.tables.find { it.tableName == tableName }
        when (referenceTable) {
          is IgnoredTable -> {} // Do nothing, we need to ignore this
          is Table -> println("Table: $tableName $schema")
          else -> {
            fail(
              """
                                |Reference schema ${referenceSchema.filename} does not contain a definition for table $tableName
                                |Options for declaration are 
                                |${examples(tableName, databaseMetaData)}
                                |"""
                .trimMargin()
            )
          }
        }
      }
    }
  }

  private fun examples(tableName: String, metadata: DatabaseMetaData): String {
    return """
            ${ignoredExample(tableName)}
            ${tableExample(tableName, metadata)}
            """
      .trimIndent()
  }

  private fun ignoredExample(tableName: String): String =
    mapper.writeValueAsString(IgnoredTable(tableName))
  private fun tableExample(tableName: String, metadata: DatabaseMetaData): String {
    val columns: List<TestacoColumn> =
      metadata.getColumns(null, null, tableName, null).use { result ->
        generateSequence {
            if (result.next()) {
              val columnName = result.getString("COLUMN_NAME")
              TestacoColumn(columnName)
            } else {
              null
            }
          }
          .toList()
      }
    return mapper.writeValueAsString(Table(tableName, columns))
  }
}
