package org.testaco.database.statement

import org.testaco.database.IDatabaseConnection
import java.sql.SQLException

/**
 * @author Manuel Laflamme
 * @version $Revision$
 * @since Mar 16, 2002
 */
interface IStatementFactory {
    @Throws(SQLException::class)
    fun createBatchStatement(connection: IDatabaseConnection): IBatchStatement
    @Throws(SQLException::class)
    fun createPreparedBatchStatement(sql: String, connection: IDatabaseConnection): IPreparedBatchStatement
}
