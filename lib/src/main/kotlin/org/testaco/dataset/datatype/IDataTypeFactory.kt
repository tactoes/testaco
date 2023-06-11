package org.testaco.dataset.datatype

interface IDataTypeFactory: IDbProductRelatable {
    fun createDataType(sqlType: Int, sqlTypeName: String): DataType<*>
    fun createDataType(sqlType: Int, sqlTypeName: String, tableName: String, columnName: String): DataType<*>
}
