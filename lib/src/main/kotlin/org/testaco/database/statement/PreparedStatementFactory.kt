package org.testaco.database.statement

import org.testaco.database.IDatabaseConnection
import java.sql.SQLException

class PreparedStatementFactory : AbstractStatementFactory() {
    @Throws(SQLException::class)
    override fun createBatchStatement(connection: IDatabaseConnection): IBatchStatement {
        return if (supportBatchStatement(connection)) {
            BatchStatement(connection.connection)
        } else {
            SimpleStatement(connection.connection)
        }
    }

    @Throws(SQLException::class)
    override fun createPreparedBatchStatement(sql: String, connection: IDatabaseConnection): IPreparedBatchStatement {
        val statement: IPreparedBatchStatement = if (supportBatchStatement(connection)) {
            PreparedBatchStatement(sql, connection.connection)
        } else {
            SimplePreparedStatement(sql, connection.connection)
        }
        return AutomaticPreparedBatchStatement(statement, connection.config.batchSize)
    }
}
