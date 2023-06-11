package org.testaco.dataset.exceptions

class NoSuchColumnException(table: String, column: String, message: String = "Column ${column} in table ${table} not found"): DataSetException(message)