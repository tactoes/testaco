package org.testaco.dataset.exceptions

class NoSuchTableException(val tableName: String) : DataSetException("Table $tableName not found")
