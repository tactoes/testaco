package org.testaco.database.statement

import java.sql.SQLException

interface IBatchStatement {
    @Throws(SQLException::class)
    fun addBatch(sql: String)

    @Throws(SQLException::class)
    fun executeBatch(): Int

    @Throws(SQLException::class)
    fun clearBatch()

    @Throws(SQLException::class)
    fun close()
}
