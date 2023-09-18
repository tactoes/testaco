package org.testaco.database.statement

import org.testaco.database.IDatabaseConnection
import java.sql.SQLException

abstract class AbstractStatementFactory : IStatementFactory {
    @Throws(SQLException::class)
    protected fun supportBatchStatement(connection: IDatabaseConnection): Boolean {
        return if (true) { //default batched statements
            connection.connection!!.metaData.supportsBatchUpdates()
        } else {
            false
        }
    }
}
