package org.testaco

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.fail
import org.springframework.core.io.Resource
import java.nio.charset.Charset
import javax.sql.DataSource

object DataSetHandler {
  private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

  fun readDataSet(localPath: String, referenceSchema: TestacoSchema): DataSet {
    val dataFileName = "${TestacoExtension.testacoConfiguration?.datadir}/$localPath"
    val dataFileResource = TestacoExtension.springContext!!.getResource(dataFileName)
    if (!dataFileResource.exists() || !dataFileResource.isReadable) {
      fail(
        """Testaco expects $localPath to exist in its data 
            |directory. With the current configuration the location would be $dataFileName.
            |Please ensure such a file exists.""".trimMargin(),
      )
    }
    val dataset = buildDataSetFromFile(dataFileResource, dataFileName, referenceSchema, localPath)

    return dataset
  }

  private fun buildDataSetFromFile(
    dataFileResource: Resource,
    dataFileName: String,
    referenceSchema: TestacoSchema,
    localPath: String
  ): DataSet {
    val dataset = try {
      mapper.readTree(
        dataFileResource.getContentAsString(Charset.defaultCharset())
      )
    } catch (e: JsonMappingException) {
      fail("Could not parse file $dataFileName, parser gives reason: ${e.message}", e)
    }

    assert(dataset.isObject(),
      { "Data set ${localPath} does not have an object as the top level element" })

    val tables = dataset.properties().map { it.key }
    val missingtables = tables.minus(referenceSchema.referenceTableList)
    assert(
      missingtables.isEmpty(),
      { "Data set $localPath contains tables that do not exist in reference schema, aborting. Extraneous tables are $missingtables" })
    return DataSet(dataset.fields().asSequence().map { tableFromSet: MutableMap.MutableEntry<String, JsonNode>? ->
      if (tableFromSet == null) {
        fail("How? What? This should not have been null?")
      }
      val tableData = tableFromSet.value
      val tableNameFromSet = tableFromSet.key
      val referenceTable = referenceSchema.tables.find { it.tableName == tableNameFromSet }
      when (referenceTable) {
        null -> fail("Table $tableNameFromSet not found in reference data schema")
        is IgnoredTable -> fail("Dataset $localPath contains data for ignored table $tableNameFromSet")
        is Table -> extractTable(
          tableData, referenceTable, tableNameFromSet, localPath
        )
      }
    }.toList())
  }

  private fun extractTable(tableData: JsonNode, referenceTable: Table, tableName: String, localPath: String): TTable {
    assert(tableData.isArray, { "Table $tableName needs to be an array representing table rows in $localPath" })
    return TTable(referenceTable.tableName,
      tableData.asSequence().map { row: JsonNode ->
        assert(
          row.isObject,
          { "Table $tableName needs to contain an array which in turn contains objects representing a row in $localPath" })
        Row(
          row.fields().asSequence().map { column: MutableMap.MutableEntry<String, JsonNode> ->
            Column(column.key, column.value)
          }.toList()
        )
      }.toList()
    )
  }

}