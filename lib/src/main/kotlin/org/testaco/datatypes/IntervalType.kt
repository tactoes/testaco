package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.postgis.PGgeometry
import org.postgresql.util.PGInterval
import java.sql.*

data class IntervalType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
    override fun read(columnName: String, rs: ResultSet): String? {
        val i = rs.getString(columnName)
        return if (rs.wasNull()) {
            null
        } else {
            i
        }
    }
    override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
        if (value == null) {
            statement.setNull(columnIndex, Types.OTHER)
        } else {
            when (value) {
                is TextNode -> statement.setObject(columnIndex, PGInterval(value.textValue()))
                else -> throw IllegalStateException("Interval database types require json data sets to store values as TextNodes")
            }
        }
    }
}