package org.testaco.database.statement

import java.sql.Connection
import java.sql.SQLException

class BatchStatement internal constructor(connection: Connection) : AbstractBatchStatement(connection) {
    @Throws(SQLException::class)
    override fun addBatch(sql: String) {
        _statement?.addBatch(sql)
    }

    @Throws(SQLException::class)
    override fun executeBatch(): Int {
        val results: IntArray = _statement?.executeBatch()!!
        var result = 0
        for (i in results.indices) {
            result += results[i]
        }
        return result
    }

    @Throws(SQLException::class)
    override fun clearBatch() {
        _statement?.clearBatch()
    }
}
