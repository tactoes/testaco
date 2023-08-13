package org.testaco.database.schema

import java.sql.DatabaseMetaData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.opentest4j.AssertionFailedError
import org.testaco.TestacoSchema
import org.testaco.util.MockResultSet

class SchemaVerifierTest {
  val uut = SchemaVerifier
  val metadata: DatabaseMetaData = mock()

  @Test
  fun `fail if reference schema is missing class`() {
    val tables =
      MockResultSet(listOf("TABLE_NAME", "TABLE_SCHEM"), listOf(listOf("aliases", "test")))
        .buildMock()
    val columns = MockResultSet(listOf("COLUMN_NAME"), listOf(listOf("id", "alias"))).buildMock()
    `when`(metadata.getTables(any(), any(), any(), eq(arrayOf("TABLE")))).thenReturn(tables)
    `when`(metadata.getColumns(any(), any(), eq("aliases"), any())).thenReturn(columns)

    val message =
      assertThrowsExactly(
          AssertionFailedError::class.java,
          { uut.verifySchema(metadata, TestacoSchema(emptyList(), "")) }
        )
        .message

    assertEquals(
      """
    Reference schema  does not contain a definition for table aliases
    Options for declaration are 
    {"IgnoredTable":{"tableName":"aliases"}}
    {"Table":{"tableName":"aliases","columns":[{"name":"id"}]}}
    """
        .trimIndent(),
      message?.trim()
    )
  }
}
