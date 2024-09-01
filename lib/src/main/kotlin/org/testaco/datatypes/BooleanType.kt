package org.testaco.datatypes

data class BooleanType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoBooleanType(name, sqlType, sqlTypeName)