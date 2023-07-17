package org.testaco.database

import org.testaco.database.statement.PreparedStatementFactory
import org.testaco.dataset.datatype.DefaultDataTypeFactory
import org.testaco.dataset.datatype.IDataTypeFactory
import org.testaco.ext.postgresql.PostgresqlDataTypeFactory
import javax.sql.DataSource

open class DatabaseConfig(
    open val dataSource: DataSource,
    open val batchSize: Int = 100,
    open val fetchSize: Int = 100,
    open val metaDataHandler: IMetadataHandler = DefaultMetadataHandler(),
    open val statementFactory: PreparedStatementFactory = PreparedStatementFactory(),
    open val resultsetFactory: CachedResultSetTableFactory = CachedResultSetTableFactory(),
    open val dataTypeFactory: IDataTypeFactory = DefaultDataTypeFactory(),
    open val batchedStatements: Boolean = false,
    open val qualifiedTableNames: Boolean = false,
    open val caseSensitiveTableNames: Boolean = false,
    open val datatypeWarning: Boolean = true,
    open val allowEmptyFields: Boolean = false,
    open val skipTables: List<String> = emptyList(),
    open val tableConfig: List<TableConfig> = emptyList(),
)

data class PostgresqlDatabaseConfig(
    override val dataSource: DataSource,
    override val batchSize: Int = 100,
    override val fetchSize: Int = 100,
    override val metaDataHandler: IMetadataHandler = DefaultMetadataHandler(),
    override val statementFactory: PreparedStatementFactory = PreparedStatementFactory(),
    override val resultsetFactory: CachedResultSetTableFactory = CachedResultSetTableFactory(),
    override val dataTypeFactory: IDataTypeFactory = PostgresqlDataTypeFactory(),
    override val batchedStatements: Boolean = false,
    override val qualifiedTableNames: Boolean = false,
    override val caseSensitiveTableNames: Boolean = false,
    override val datatypeWarning: Boolean = true,
    override val allowEmptyFields: Boolean = false,
    override val skipTables: List<String> = emptyList(),
    override val tableConfig: List<TableConfig> = emptyList(),
) :
    DatabaseConfig(
        dataSource, batchSize, fetchSize, metaDataHandler, statementFactory,
        resultsetFactory, dataTypeFactory,
        batchedStatements, qualifiedTableNames, caseSensitiveTableNames,
        datatypeWarning, allowEmptyFields, skipTables, tableConfig,
    )

data class TableConfig(val name: String, val columns: ColumnConfig)

data class ColumnConfig(val name: String, val comparator: Comparator<*>)
