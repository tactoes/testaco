package org.testaco.database

import org.slf4j.LoggerFactory
import org.testaco.database.DatabaseDataSourceConnection
import java.sql.Connection
import java.sql.SQLException
import javax.naming.InitialContext
import javax.sql.DataSource

class DatabaseDataSourceConnection @JvmOverloads constructor(
    private val _dataSource: DataSource, override val schema: String = "",
    private val _user: String, private val _password: String,
    override val config: DatabaseConfig
) : AbstractDatabaseConnection(config), IDatabaseConnection {
    private fun connection(): Connection = _dataSource.getConnection(_user, _password)
    override val connection: Connection
        get() = TODO("Not yet implemented")

    override fun close() {
        TODO("Not yet implemented")
    }
}
