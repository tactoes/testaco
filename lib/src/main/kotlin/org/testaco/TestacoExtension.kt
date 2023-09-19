package org.testaco

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.fail
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testaco.database.schema.SchemaVerifier
import java.nio.charset.Charset
import javax.sql.DataSource


class TestacoExtension() : BeforeAllCallback {
  val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

  override fun beforeAll(context: ExtensionContext?) {
    contexts(context)
    if (springContext == null) {
      fail("No spring context found") }
    if (testacoConfiguration == null) {
      fail("No testaco configuration found")
    }
    testacoConfiguration!!.databases.forEach { tdb ->
      val dataSource = (springContext!!.getBean(tdb.dataSource) as DataSource?)
        ?: fail("datasource ${tdb.dataSource} fetched from spring must not be null")
      val schemaFileName = "${testacoConfiguration?.datadir}/${tdb.dataSource}_schema.json"
      val schemaResource = springContext!!.getResource(schemaFileName)
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
        ).copy(filename = schemaFileName)
      } catch (e: JsonMappingException) {
        fail("Could not parse file $schemaFileName, parser gives reason: ${e.message}", e)
      }
      val databaseMetaData = dataSource.connection.metaData
      SchemaVerifier.verifySchema(databaseMetaData, referenceSchema)
      referenceSchemas.put(tdb.dataSource, referenceSchema)
    }
    // TODO: Verify database schema against stored schema.
    // TODO: Verify stored schema against configuration.
    println("Extension goes here")
  }

  private fun contexts(context: ExtensionContext?) {
    springContext = SpringExtension.getApplicationContext(context
      ?: fail("The extension needs a context, panic!"))
    testacoConfiguration = springContext?.getBean(TestacoConfiguration::class.java)
      ?: fail("No testaco configuration found in spring context, panic!")
  }

  fun loadDataSet(dataSourceName: String, dataFile: String) {
    println("Starts")
    assert(referenceSchemas.contains(dataSourceName), {"Database $dataSourceName not known to testaco. It needs to be defined in the testaco configuration, along with a reference schema file"})
    val referenceSchema = referenceSchemas.get(dataSourceName)!!
    val localPath = "$dataSourceName/$dataFile"
    val dataset: JsonNode = readDataSet(dataSourceName, localPath)

    assert(dataset.isObject == true, {"Data set does not contain an object as its root node"})

    val tables = dataset.properties().map{ it.key }
    val missingtables = tables.minus(referenceSchema.referenceTableList)
    assert(missingtables.isEmpty(), {"Data set $localPath contains tables that do not exist in reference schema, aborting. Extraneous tables are $missingtables"})

    dataset.fields().asSequence().forEach { tableFromSet: MutableMap.MutableEntry<String, JsonNode>? ->
      if (tableFromSet == null) { fail("How? What? This should not have been null?") }
      val table: TestacoTable = referenceSchema.tables.find { it.tableName == tableFromSet.key} ?: fail("Table ${tableFromSet.key} not found in reference data schema")
      when (table) {
        is IgnoredTable -> fail("Dataset $dataFile contains data for ignored table ${tableFromSet.key}")
        is Table -> ???
      }
    }

    println("Ends")
  }

  private fun readDataSet(dataSourceName: String, localPath: String): JsonNode {
    val dataSource = (springContext?.getBean(dataSourceName) as DataSource?)
      ?: fail("datasource ${dataSourceName} fetched from spring must not be null")
    val dataFileName = "${testacoConfiguration?.datadir}/$localPath"
    val dataFileResource = springContext!!.getResource(dataFileName)
    if (!dataFileResource.exists() || !dataFileResource.isReadable) {
      fail(
        """Testaco expects $localPath to exist in its data 
            |directory. With the current configuration the location would be $dataFileName.
            |Please ensure such a file exists.""".trimMargin(),
      )
    }
    val dataset: JsonNode = try {
      mapper.readTree(
        dataFileResource.getContentAsString(Charset.defaultCharset())
      )
    } catch (e: JsonMappingException) {
      fail("Could not parse file $dataFileName, parser gives reason: ${e.message}", e)
    }
    return dataset
  }

  companion object {
    var springContext: ApplicationContext? = null
    var testacoConfiguration: TestacoConfiguration? = null
    val referenceSchemas: MutableMap<String, TestacoSchema> = mutableMapOf()
  }
}
