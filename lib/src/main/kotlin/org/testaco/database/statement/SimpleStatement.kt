package org.testaco.database.statement

import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException

class SimpleStatement internal constructor(connection: Connection) : AbstractBatchStatement(connection) {
    private val _list: MutableList<String> = ArrayList()
    @Throws(SQLException::class)
    override fun addBatch(sql: String) {
        _list.add(sql)
    }

    @Throws(SQLException::class)
    override fun executeBatch(): Int {
        var result = 0
        for (i in _list.indices) {
            val sql = _list[i]
            if (logger.isDebugEnabled) logger.debug("testaco SQL: $sql")
            val r = _statement!!.execute(sql)
            if (!r) {
                result += _statement!!.updateCount
            }
        }
        return result
    }

    @Throws(SQLException::class)
    override fun clearBatch() {
        _list.clear()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SimpleStatement::class.java)
    }
}
