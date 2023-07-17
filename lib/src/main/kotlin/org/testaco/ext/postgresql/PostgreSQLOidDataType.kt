package org.testaco.ext.postgresql

import org.postgresql.PGConnection
import org.postgresql.largeobject.LargeObjectManager
import org.testaco.dataset.datatype.BytesDataType
import java.io.ByteArrayInputStream
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

class PostgreSQLOidDataType : BytesDataType("OID", Types.BIGINT) {
    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): ByteArray? {
        val statement = resultSet.statement
        val connection = statement.connection
        val autoCommit = connection.autoCommit
        connection.autoCommit = false
        return try {
            val pgConnection = connection.unwrap(PGConnection::class.java)
            val lobj = pgConnection.largeObjectAPI
            val oid = resultSet.getLong(column)
            if (oid == 0L) {
                null
            } else {
                val obj = lobj.open(oid, LargeObjectManager.READ)
                val buf = ByteArray(obj.size())
                obj.read(buf, 0, obj.size())
                obj.close()
                buf
            }
        } finally {
            connection.autoCommit = autoCommit
        }
    }

    @Throws(SQLException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        val connection = statement.connection
        val autoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            val lobj = (statement.connection as PGConnection).largeObjectAPI
            val oid = lobj.createLO(LargeObjectManager.READ or LargeObjectManager.WRITE)
            val obj = lobj.open(oid, LargeObjectManager.WRITE)
            val bis = ByteArrayInputStream(super.typeCast(value) as ByteArray?)
            val buf = ByteArray(2048)
            var s: Int
            while (bis.read(buf, 0, 2048).also { s = it } > 0) {
                obj.write(buf, 0, s)
            }
            obj.close()
            statement.setLong(column, oid)
        } finally {
            connection.autoCommit = autoCommit
        }
    }
}
