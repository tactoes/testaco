package org.testaco.database.statement

import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement

abstract class AbstractBatchStatement internal constructor(connection: Connection) : IBatchStatement {
    protected var _statement: Statement? = null

    init {
        _statement = try {
            connection.createStatement()
        } catch (e: SQLException) {
            throw e
        }
    }

    @Throws(SQLException::class)
    override fun close() {
        _statement!!.close()
    }
}
