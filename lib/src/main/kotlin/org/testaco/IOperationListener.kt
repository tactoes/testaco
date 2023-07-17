package org.testaco

import org.slf4j.LoggerFactory
import org.testaco.database.IDatabaseConnection

interface IOperationListener {
    fun connectionRetrieved(connection: IDatabaseConnection)
    fun onSetUpFinished(connection: IDatabaseConnection)
    fun onTearDownFinished(connection: IDatabaseConnection)

    companion object {
        val NO_OP_OPERATION_LISTENER: IOperationListener = object : IOperationListener {
            private val logger = LoggerFactory.getLogger(this::class.java)
            override fun connectionRetrieved(connection: IDatabaseConnection) {
                logger.trace("connectionCreated(connection={}) - start", connection)
            }

            override fun onSetUpFinished(connection: IDatabaseConnection) {
                logger.trace("operationSetUpDone(connection={}) - start", connection)
            }

            override fun onTearDownFinished(connection: IDatabaseConnection) {
                logger.trace("operationTearDownDone(connection={}) - start", connection)
            }
        }
    }
}
