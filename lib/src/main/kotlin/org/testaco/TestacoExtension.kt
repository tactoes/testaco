package org.testaco

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.fail
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testaco.comparator.DatasetComparator
import org.testaco.database.schema.SchemaVerifier
import org.testaco.datatypes.TestacoType
import java.io.File
import java.nio.charset.Charset
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource


class TestacoExtension() : BeforeAllCallback {
  val mapper = ObjectMapper()
    .registerModule(KotlinModule.Builder().build())
    .registerModule(SimpleModule().apply {
      addSerializer(DataSet::class.java, DataSetSerializer())
    })

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

  /**
   * Load a data file. File is loaded from the classpath.
   */
  fun loadDataSet(dataSourceName: String, dataFile: String) {
    assert(referenceSchemas.contains(dataSourceName), {"Database $dataSourceName not known to testaco. It needs to be defined in the testaco configuration, along with a reference schema file"})
    val dataSource = (springContext!!.getBean(dataSourceName) as DataSource?)
      ?: fail("datasource ${dataSourceName} fetched from spring must not be null")
    val referenceSchema = referenceSchemas.get(dataSourceName)!!
    val localPath = "$dataSourceName/$dataFile"

    val dataset: DataSet = DataSetHandler.readDataSet(localPath, referenceSchema)

    importDataSet(dataSourceName, dataset, dataSource)
  }

  fun dumpDataSet(dataSourceName: String, dataFile: String) {
    val dataset: DataSet = readDatabaseDataSet(dataSourceName)

    mapper.writerWithDefaultPrettyPrinter().writeValue(File(dataFile), dataset)

    println("Dump ends")
  }

  private fun readDatabaseDataSet(dataSourceName: String): DataSet {
    println("Dump starts")
    val dataSource = (springContext!!.getBean(dataSourceName) as DataSource?)
      ?: fail("datasource ${dataSourceName} fetched from spring must not be null")
    val referenceSchema: TestacoSchema = referenceSchemas.get(dataSourceName)
      ?: throw IllegalStateException("Database $dataSourceName not known to testaco. It needs to be defined in the testaco configuration, along with a reference schema file")

    val dataset: DataSet = exportDataSet(dataSource, referenceSchema)
    return dataset
  }

  fun compareDataSet(dataSourceName: String, dataFile: String) {
    val databaseDataset: DataSet = readDatabaseDataSet(dataSourceName)
    val referenceSchema = referenceSchemas.get(dataSourceName)!!
    val localPath = "$dataSourceName/$dataFile"

    val fileDataset: DataSet = DataSetHandler.readDataSet(localPath, referenceSchema)
    require(DatasetComparator(referenceSchema).compare(databaseDataset, fileDataset).equals())
  }

  private fun exportDataSet(dataSource: DataSource, referenceSchema: TestacoSchema): DataSet = DataSet(
    referenceSchema.tables.mapNotNull { table ->
      when (table) {
        is Table -> TTable(table.tableName, rows = exportRows(dataSource, referenceSchema.find(table.tableName) as Table))
        is IgnoredTable -> null
      }
    }.toSet()
  )

  private fun exportRows(dataSource: DataSource, table: Table): Set<Row> {
    println("Exporting table ${table.tableName}")
    val sql = """SELECT 
      |${table.columns.map {it.name}.joinToString(", ")}
      |FROM ${table.tableName}
    """.trimMargin()
    println("Sql: $sql")
    val st = dataSource.getConnection().prepareStatement(sql)
    val results: ResultSet = st.executeQuery()

    return buildSet<Row> {
      while (results.next()) {
          add(Row(table.columns.map { Column(it.name, it.read(it.name, results))}.toSet()))
      }
    }
  }

  private fun importDataSet(dataSourceName: String, dataset: DataSet, dataSource: DataSource) {
    dataset.tables.forEach { table: TTable ->
      println("Handling table ${table.name}")
      table.rows.forEach { row: Row ->
        print("  Row ")
        row.columns.forEach { column: Column ->
          print(" ${column.name}:${column.value}")
        }
        println()
        val sql = """INSERT INTO ${table.name} 
          |(${row.columns.map { it.name }.joinToString(", ")})
          | VALUES 
          | (${List(row.columns.count()) {"?"}.joinToString(", ")})""".trimMargin()
        val st = dataSource.getConnection().prepareStatement(sql)
        row.columns.mapIndexed { index, column ->
          try {
            setValue(dataSourceName, table.name, column.name, index + 1, column.value, st)
          } catch (e: IllegalStateException) {
            throw IllegalStateException("Could not insert column ${column.name} in table ${table.name} with value ${column.value} because ${e.message}", e)
          }
        }
        st.execute()
      }
    }
  }

  private fun setValue(dataSourceName: String, tableName: String, columnName: String, index: Int, value: JsonNode?, st: PreparedStatement) {
    val testacoType: TestacoType<*> = findTestacoType(dataSourceName, tableName, columnName)
    if (!(value?.isValueNode ?: false)) {
      throw IllegalStateException("Encountered a non-value node json fragment from data set: "+value)
    }
    testacoType.store(value, index, st)
  }

  private fun findTestacoType(dataSourceName: String, tableName: String, columnName: String): TestacoType<*> {
    val table: TestacoTable = referenceSchemas.get(dataSourceName)?.find(tableName) ?: throw IllegalStateException("Could not find table $tableName in schema $dataSourceName")
    val column: TestacoType<*> = (table as Table).columns.find { it.name == columnName } ?: throw IllegalStateException("Could not find column $columnName in table $tableName in schema $dataSourceName")
    return column
  }

  companion object {
    var springContext: ApplicationContext? = null
    var testacoConfiguration: TestacoConfiguration? = null
    val referenceSchemas: MutableMap<String, TestacoSchema> = mutableMapOf()
  }
}
