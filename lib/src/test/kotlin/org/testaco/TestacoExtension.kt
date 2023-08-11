package org.testaco

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.fail
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.nio.charset.Charset
import java.sql.DatabaseMetaData
import javax.sql.DataSource


class TestacoExtension() : BeforeAllCallback {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    override fun beforeAll(context: ExtensionContext?) {
        val springContext: ApplicationContext = SpringExtension.getApplicationContext(context ?: fail("The extension needs a context, panic!"))
        val testacoConfiguration = springContext.getBean(TestacoConfiguration::class.java)
            ?: fail("No testaco configuration found in spring context, panic!")
        testacoConfiguration.databases.forEach { tdb ->
            val dataSource = (springContext.getBean(tdb.dataSource) as DataSource?)
                ?: fail("datasource ${tdb.dataSource} fetched from spring must not be null")
            val schemaFileName = "${testacoConfiguration.datadir}/${tdb.dataSource}_schema.json"
            val schemaResource = springContext.getResource(schemaFileName)
            if (!schemaResource.exists() || !schemaResource.isReadable) {
                fail(
                    """Testaco expects a readable schema file to exist in its data 
                |directory. With the current configuration the location would be $schemaFileName.
                |Please ensure such a file exists, and contains a structure that is compatible with
                |all the data sets that will be handled by testaco.
                    """.trimMargin(),
                )
            }
            val referenceSchema: TestacoSchema = try {
                 mapper.readValue(
                    schemaResource.getContentAsString(Charset.defaultCharset()),
                    TestacoSchema::class.java,
                )
            } catch (e: JsonMappingException) {
                throw IllegalStateException("Could not parse file $schemaFileName, parser gives reason: ${e.message}", e)
            }
            val databaseMetaData = dataSource.connection.metaData
            println("Testing metadata")
            verifySchema(databaseMetaData, referenceSchema, schemaFileName)
        }
        // TODO: Verify database schema against stored schema.
        // TODO: Verify stored schema against configuration.
        println("Extension goes here")
    }

    private fun verifySchema(
        databaseMetaData: DatabaseMetaData,
        referenceSchema: TestacoSchema,
        schemaFileName: String
    ) {
        databaseMetaData.getTables(null, null, null, arrayOf<String>("TABLE")).use { resultSet ->
            while (resultSet.next()) {
                val tableName: String = resultSet.getString("TABLE_NAME")
                    ?: fail("Don't know how to cope if metadata does not return tablename, panic!")
                val schema: String? = resultSet.getString("TABLE_SCHEM")
                val remarks: String? = resultSet.getString("REMARKS")

                val referenceTable: TestacoTable? = referenceSchema.tables.find { it.tableName == tableName }
                when (referenceTable) {
                    is IgnoredTable -> {} //Do nothing, we need to ignore this
                    is Table -> println("Table: $tableName $schema $remarks")
                    else -> fail("Reference schema $schemaFileName does not contain a definition for table $tableName")
                }
            }
        }
    }
}
