package org.testaco.database.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import org.junit.jupiter.api.fail
import org.testaco.*

const val NOT_DEFERRABLE = 7.toShort()
const val INITIALLY_DEFERRED = 4.toShort()

object SchemaVerifier {
  private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

  fun verifySchema(databaseMetaData: DatabaseMetaData, referenceSchema: TestacoSchema) {
    val tables = databaseMetaData.getTables(null, null, null, arrayOf<String>("TABLE")).use { resultSet ->
      generateSequence {
        if (resultSet.next()) {
          resultSet.getString("TABLE_NAME")
        } else {
          null
        }
      }.toList()
    }
    tables.forEach { tableName ->
      when (referenceSchema.find(tableName)) {
        is IgnoredTable -> {} // Do nothing, we need to ignore this
        is Table -> {
          verifyColumns(tableName, databaseMetaData, referenceSchema)
          verifyTableOrderComplatibleWithForeignKeys(
            referenceSchema.referenceTableList,
            databaseMetaData,
            tableName,
            referenceSchema.filename
          )
        }
        null -> {
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
    val extraTablesInReferenceSchema = referenceSchema.tables.map { it.tableName }.minus(tables)
    if (extraTablesInReferenceSchema.isNotEmpty()) {
      fail("""Reference schema contains definitions for tables $extraTablesInReferenceSchema. 
        |These can not be found in the database, and should be deleted.""".trimMargin())
    }
  }

  private fun verifyTableOrderComplatibleWithForeignKeys(
    referenceTableList: List<String>,
    databaseMetaData: DatabaseMetaData,
    tableName: String,
    filename: String?
  ) {
    databaseMetaData
      .getImportedKeys(null, null, tableName)
      .use { rs: ResultSet ->
        while (rs.next()) {
          val table = rs.getString("PKTABLE_NAME")
          val column = rs.getString("PKCOLUMN_NAME")
          val nondeferrable: Boolean = rs.getShort("DEFERRABILITY") == NOT_DEFERRABLE
          if (
            nondeferrable &&
            (referenceTableList.indexOf(tableName) < referenceTableList.indexOf(table))
          ) {
            fail(
              """Foreign key not marked as deferrable for table $tableName refers to a table $table that is defined later in ${filename}. 
                |This will cause problems when loading data sets unless the foreign key is marked deferrable. Please either
                |mark the foreign key as deferrable or move $table before $tableName in ${filename}"""
                .trimMargin()
            )
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
    val table: TestacoTable? = referenceSchema.tables.find { it.tableName == tableName }
    val referenceColumns =
      (table as? Table?)?.columns?.toSet()
        ?: fail("Concrete table configuration $tableName not found in reference data")
    if (columns.toSet() != referenceColumns.toSet()) {
      val missingInReference = columns.minus(referenceColumns)
      val missingInDb = referenceColumns.minus(columns)
      fail(
        """Table $tableName has extra columns $missingInReference in the database, 
          |or has extra $missingInDb columns in the testaco configuration.
          |A suitable table definition should be
          |${tableExample(tableName, databaseMetaData)}"""
          .trimMargin()
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
      }
        .toList()
    }
  }
}
