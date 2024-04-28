package org.testaco.database.exceptions

internal class AmbiguousTableNameException(val msg: String? = null, val e: Exception? = null) : DataSetException(msg, e)
