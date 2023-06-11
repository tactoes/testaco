package org.testaco.database

import org.testaco.database.statement.PreparedStatementFactory
import org.testaco.dataset.datatype.DefaultDataTypeFactory
import org.testaco.dataset.datatype.IDataTypeFactory
import org.testaco.dataset.filter.IColumnFilter
import java.util.*

data class DatabaseConfig(
    val tableType: Array<String> = arrayOf("TABLE"),
    val batchSize: Int = 100,
    val fetchSize: Int = 100,
    val metaDataHandler: IMetadataHandler = DefaultMetadataHandler(),
    val statementFactory: PreparedStatementFactory = PreparedStatementFactory(),
    val resultsetFactory: CachedResultSetTableFactory = CachedResultSetTableFactory(),
    val dataTypeFactory: IDataTypeFactory = DefaultDataTypeFactory(),
//    val primaryKeyFilter: IColumnFilter = DefaultPrimaryKeyFilter(),
//    val identityColumnFilter: IColumnFilter = DefaultIdentityColumnFilter(),
    val escapePattern: String? = null,
    val batchedStatements: Boolean = false,
    val qualifiedTableNames: Boolean = false,
    val caseSensitiveTableNames: Boolean = false,
    val datatypeWarning: Boolean = true,
    val allowEmptyFields: Boolean = false,
    val skipOracleRecyclebinTables: Boolean = false)