package org.testaco.database

import org.slf4j.LoggerFactory
import org.testaco.dataset.ITableMetaData
import java.sql.PreparedStatement
import java.sql.SQLException

class CachedResultSetTableFactory : IResultSetTableFactory {
    @Throws(SQLException::class)
    override fun createTable(
        tableName: String, selectStatement: String,
        connection: IDatabaseConnection
    ): IResultSetTable {
        if (logger.isTraceEnabled) logger.trace(
            "createTable(tableName={}, selectStatement={}, connection={}) - start",
            *arrayOf(tableName, selectStatement, connection)
        )
/*        return CachedResultSetTable(
            ForwardOnlyResultSetTable(
                tableName, selectStatement, connection
            )
        )*/
        return object: IResultSetTable {
            override fun close() {
                TODO("Not yet implemented")
            }

            override val tableMetaData: ITableMetaData
                get() = TODO("Not yet implemented")
            override val rowCount: Int
                get() = TODO("Not yet implemented")
        }
    }

    @Throws(SQLException::class)
    override fun createTable(
        metaData: ITableMetaData,
        connection: IDatabaseConnection
    ): IResultSetTable {
        logger.trace("createTable(metaData={}, connection={}) - start", metaData, connection)
        //val resultSetTable = ForwardOnlyResultSetTable(metaData, connection)
        //return CachedResultSetTable(resultSetTable)
        return object: IResultSetTable {
            override fun close() {
                TODO("Not yet implemented")
            }

            override val tableMetaData: ITableMetaData
                get() = TODO("Not yet implemented")
            override val rowCount: Int
                get() = TODO("Not yet implemented")
        }
    }

    @Throws(SQLException::class)
    override fun createTable(
        tableName: String,
        preparedStatement: PreparedStatement, connection: IDatabaseConnection
    ): IResultSetTable {
        if (logger.isTraceEnabled) logger.trace(
            "createTable(tableName={}, preparedStatement={}, connection={}) - start",
            *arrayOf(tableName, preparedStatement, connection)
        )

        // Reuse method from ForwardOnly factory
        //val table: ForwardOnlyResultSetTable = ForwardOnlyResultSetTableFactory()
        //    .createForwardOnlyResultSetTable(tableName, preparedStatement, connection)
        //return CachedResultSetTable(table)
        return object: IResultSetTable {
            override fun close() {
                TODO("Not yet implemented")
            }

            override val tableMetaData: ITableMetaData
                get() = TODO("Not yet implemented")
            override val rowCount: Int
                get() = TODO("Not yet implemented")
        }
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(CachedResultSetTableFactory::class.java)
    }
}
