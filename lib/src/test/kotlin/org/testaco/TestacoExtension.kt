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
import javax.sql.DataSource


class TestacoExtension() : BeforeAllCallback {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    override fun beforeAll(context: ExtensionContext?) {
        assert(context != null, {"The extension needs a contest, panic!"})
        val springContext: ApplicationContext = SpringExtension.getApplicationContext(context!!)
        val testacoConfiguration = springContext.getBean(TestacoConfiguration::class.java)
        assert(testacoConfiguration != null, { "No testaco configuration found in spring context, panic!" })
        testacoConfiguration.databases.forEach { tdb ->
            val dataSource = springContext.getBean(tdb.dataSource) as DataSource
            assert(dataSource != null, { "datasource fetched from spring must not be null" })
            val schemaFileName = "${testacoConfiguration.datadir}/${tdb.dataSource}_schema.json"
            val schemaResource = springContext.getResource(schemaFileName)
            if (!schemaResource.exists() || !schemaResource.isReadable) {
                throw IllegalStateException(
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
            databaseMetaData.getTables(null, null, null, arrayOf<String>("TABLE")).use { resultSet ->
                while (resultSet.next()) {
                    val tableName: String? = resultSet.getString("TABLE_NAME")
                    val schema: String? = resultSet.getString("TABLE_SCHEM")
                    val remarks: String? = resultSet.getString("REMARKS")
                    assert(tableName != null, {"Don't know how to cope if metadata does not return tablename, panic!"})
                    if (!tdb.tableConfig.contains(IgnoredTable(tableName!!))) {
                        val referenceTable = referenceSchema.tables
                            .find { it.name == tableName }
                            ?: fail("Reference schema $schemaFileName does not contain a definition for table $tableName")
                        println("Table: $tableName $schema $remarks")
                    }
                }
            }
        }
        // TODO: Verify database schema against stored schema.
        // TODO: Verify stored schema against configuration.
        println("Extension goes here")
    }
}
