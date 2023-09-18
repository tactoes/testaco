package org.testaco.dataset

import org.testaco.database.IDatabaseConnection
import org.testaco.dataset.datatype.DefaultDataTypeFactory
import org.testaco.dataset.datatype.IDataTypeFactory
import org.testaco.dataset.exceptions.DataSetException
import java.sql.DatabaseMetaData
import java.sql.SQLException
import java.util.*

abstract class AbstractTableMetaData: ITableMetaData {
    @Throws(DataSetException::class)
    override fun getColumnIndex(columnName: String): Int =
        this.columns.indexOfFirst { it.columnName.uppercase() == columnName.uppercase() }

    @Throws(SQLException::class)
    fun getDataTypeFactory(connection: IDatabaseConnection): IDataTypeFactory {
        val metaData: DatabaseMetaData = connection.connection!!.metaData
        val dataTypeFactory = DefaultDataTypeFactory()

        val databaseProductName: String = metaData.databaseProductName
        if (dataTypeFactory.validDbProducts.isNotEmpty() && !dataTypeFactory.validDbProducts.contains(
                databaseProductName.lowercase()
            )
        ) {
            throw RuntimeException(
                "The configured data type factory '" + dataTypeFactory::class +
                        "' might cause problems with the current database '" + databaseProductName +
                        "' (e.g. some datatypes may not be supported properly). " +
                        "In rare cases you might see this message because the list of supported database " +
                        "products is incomplete (list=" + dataTypeFactory.validDbProducts + "). " +
                        "If so please request an update via the forums." +
                        "If you are using your own IDataTypeFactory extending " +
                        "DefaultDataTypeFactory, ensure that you override validDbProducts " +
                        "to specify the supported database products."
            )
        }
        return dataTypeFactory
    }
}
