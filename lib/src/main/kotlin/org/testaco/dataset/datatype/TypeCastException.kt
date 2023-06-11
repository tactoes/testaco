package org.testaco.dataset.datatype

class TypeCastException : RuntimeException {
    constructor(
        value: Any?,
        dataType: DataType<*>?
    ) : super(org.testaco.dataset.datatype.TypeCastException.Companion.buildMessage(value, dataType))

    constructor(
        value: Any?,
        dataType: DataType<*>?,
        e: Throwable?
    ) : super(org.testaco.dataset.datatype.TypeCastException.Companion.buildMessage(value, dataType), e)


    companion object {
        private fun buildMessage(value: Any?, dataType: DataType<*>?): String {
            val valueClass = if (value == null) "null" else value.javaClass.name
            return "Unable to typecast value <" + value + "> of type <" +
                    valueClass + "> to " + dataType
        }
    }
}
