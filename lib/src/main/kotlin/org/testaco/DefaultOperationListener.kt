package org.testaco

import org.slf4j.LoggerFactory
import org.testaco.database.IDatabaseConnection
import java.sql.SQLException

open class DefaultOperationListener : IOperationListener {
    override fun connectionRetrieved(connection: IDatabaseConnection) {
        logger.debug("connectionCreated(connection={}) - start", connection)
        // Is by default a no-op
    }

    override fun onSetUpFinished(connection: IDatabaseConnection) {
        logger.debug("operationSetUpFinished(connection={}) - start", connection)
        closeConnection(connection)
    }

    override fun onTearDownFinished(connection: IDatabaseConnection) {
        logger.debug("operationTearDownFinished(connection={}) - start", connection)
        closeConnection(connection)
    }

    private fun closeConnection(connection: IDatabaseConnection) {
        logger.debug("closeConnection(connection={}) - start", connection)
        try {
            connection.close()
        } catch (e: SQLException) {
            logger.warn("Exception while closing the connection: $e", e)
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger(DefaultOperationListener::class.java)
    }
}