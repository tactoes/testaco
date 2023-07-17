package org.testaco.database.exceptions

import org.testaco.dataset.exceptions.DataSetException

internal class AmbiguousTableNameException(val msg: String? = null, val e: Exception? = null) : DataSetException(msg, e)
