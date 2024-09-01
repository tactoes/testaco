package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import org.postgresql.util.PGInterval
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class IntervalType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
    override fun read(columnName: String, rs: ResultSet): JsonNode {
        val i = rs.getString(columnName)
        return if (rs.wasNull()) {
            NullNode.instance
        } else {
            TextNode.valueOf(i)
        }
    }
    override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
        if (value == null) {
            statement.setNull(columnIndex, Types.OTHER)
        } else {
            when (value) {
                is TextNode -> statement.setObject(columnIndex, PGInterval(value.textValue()))
                else -> error("Interval database types require json data sets to store values as TextNodes")
            }
        }
    }
}