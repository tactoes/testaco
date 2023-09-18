package org.testaco.util

import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.withSettings
import org.mockito.internal.creation.MockSettingsImpl
import org.mockito.invocation.InvocationOnMock
import org.mockito.quality.Strictness
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException

class MockResultSet(
    val columnNames: List<String>,
    private val data: List<List<Any>>
) {
    private fun columnIndex(column: String): Int = columnNames.indexOf(column)
        .also { if (it == -1) throw IllegalStateException("Unknown column $column") }
    private var rowIndex: Int = -1

    @Throws(SQLException::class)
    fun buildMock(): ResultSet {
        val rs = mock(ResultSet::class.java, withSettings().defaultAnswer { throw IllegalStateException("Unmocked call") })
        val rsmd = mock(ResultSetMetaData::class.java)
        // mock rs.next()
        Mockito.doAnswer { invocation: InvocationOnMock? ->
            rowIndex++
            rowIndex < data.size
        }.`when`(rs).next()

        // mock rs.getString(columnName)
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val columnName = invocation.getArgument(0, String::class.java)
            val columnIndex = columnIndex(columnName)
            data[rowIndex][columnIndex] as String
        }.`when`(rs).getString(ArgumentMatchers.anyString())

        // mock rs.getShort(columnName)
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val columnName = invocation.getArgument(0, String::class.java)
            val columnIndex = columnIndex(columnName)
            data[rowIndex][columnIndex] as Short
        }.`when`(rs).getShort(ArgumentMatchers.anyString())

        // mock rs.getInt(columnName)
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val columnName = invocation.getArgument(0, String::class.java)
            val columnIndex = columnIndex(columnName)
            data[rowIndex][columnIndex] as Int
        }.`when`(rs).getInt(ArgumentMatchers.anyString())

        Mockito.doAnswer { invocation: InvocationOnMock ->
            rowIndex = -1
            rowIndex
        }.`when`(rs).close()

        Mockito.doReturn(columnNames.size).`when`(rsmd).columnCount

        Mockito.doReturn(rsmd).`when`(rs).metaData
        return rs
    }
}