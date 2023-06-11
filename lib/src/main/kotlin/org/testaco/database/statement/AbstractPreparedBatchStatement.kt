package org.testaco.database.statement

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * @author Manuel Laflamme
 * @version $Revision$
 * @since Mar 16, 2002
 */
abstract class AbstractPreparedBatchStatement internal constructor(sql: String, connection: Connection) :
    IPreparedBatchStatement {
    @JvmField
    protected val _statement: PreparedStatement

    init {
        _statement = connection.prepareStatement(sql)
    }

    @Throws(SQLException::class)
    override fun close() {
        _statement.close()
    }
}
