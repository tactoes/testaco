package org.testaco

import java.sql.SQLException

class DatabaseUnitRuntimeException(message: String? = null, e: Exception? = null) : RuntimeException(message, e)
