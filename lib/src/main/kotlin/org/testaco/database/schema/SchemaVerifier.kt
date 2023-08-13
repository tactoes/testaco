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
          is Table -> verifyColumns(tableName, databaseMetaData, referenceSchema)
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

  private fun verifyColumns(
    tableName: String,
    databaseMetaData: DatabaseMetaData,
    referenceSchema: TestacoSchema
  ) {
    val columns = getColumns(databaseMetaData, tableName).toSet()
    val table: TestacoTable? = referenceSchema
      .tables.find { it.tableName == tableName }
    val referenceColumns = (table as? Table?)
      ?.columns?.toSet()
      ?: fail("Concrete table configuration $tableName not found in reference data")
    if (columns.toSet() != referenceColumns.toSet()) {
      val missingInReference = columns.minus(referenceColumns)
      val missingInDb = referenceColumns.minus(columns)
      fail(
        """Table $tableName has extra columns $missingInReference in the database, 
or has extra $missingInDb columns in the testaco configuration.
A suitable table definition should be
${tableExample(tableName, databaseMetaData)}"""
      )
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
    val columns = getColumns(metadata, tableName)
    System.out.println("columns: $columns $tableName")
    return mapper.writeValueAsString(Table(tableName, columns))
  }

  private fun getColumns(metadata: DatabaseMetaData, tableName: String): List<TestacoColumn> {
    return metadata.getColumns(null, null, tableName, null).use { result ->
      generateSequence {
        if (result.next()) {
          val columnName = result.getString("COLUMN_NAME")
          TestacoColumn(columnName)
        } else {
          null
        }
      }.toList()
    }
  }
}
