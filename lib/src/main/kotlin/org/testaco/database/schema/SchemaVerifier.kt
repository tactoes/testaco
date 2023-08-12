package org.testaco.database.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import org.junit.jupiter.api.fail
import org.testaco.*

object SchemaVerifier {
    private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    fun verifySchema(
        databaseMetaData: DatabaseMetaData,
        referenceSchema: TestacoSchema,
        schemaFileName: String
    ) {
        databaseMetaData.getTables(null, null, null, arrayOf<String>("TABLE")).use { resultSet ->
            while (resultSet.next()) {
                val tableName: String =
                    resultSet.getString("TABLE_NAME")
                        ?: fail("Don't know how to cope if metadata does not return tablename, panic!")
                val schema: String? = resultSet.getString("TABLE_SCHEM")
                val remarks: String? = resultSet.getString("REMARKS")

                val referenceTable: TestacoTable? =
                    referenceSchema.tables.find { it.tableName == tableName }
                when (referenceTable) {
                    is IgnoredTable -> {} // Do nothing, we need to ignore this
                    is Table -> println("Table: $tableName $schema $remarks")
                    else -> {
                        fail(
                            """
                                |Reference schema $schemaFileName does not contain a definition for table $tableName
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
            ${ignoredExample(tableName, metadata)}
            ${tableExample(tableName, metadata)}
            """
            .trimIndent()
    }

    private fun ignoredExample(tableName: String, metadata: DatabaseMetaData): String =
        mapper.writeValueAsString(IgnoredTable(tableName))
    private fun tableExample(tableName: String, metadata: DatabaseMetaData): String {
        val columns: List<TestacoColumn> =
            metadata.getColumns(null, null, tableName, null).use { result ->
                generateSequence {
                    if (result.next()) {
                        val columnName = result.getString("COLUMN_NAME")
                        val dataType = result.getInt("DATA_TYPE")
                        val dataTypeName = result.getString("TYPE_NAME")
                        val isAutoIncrement = result.getString("IS_AUTOINCREMENT")
                        val isGeneratedColumn = result.getString("IS_GENERATEDCOLUMN")
                        TestacoColumn(columnName)
                    } else {
                        null
                    }
                }.toList()
            }
        return mapper.writeValueAsString(Table(tableName, columns))
    }
}
