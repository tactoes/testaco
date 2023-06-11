package org.testaco.dataset.datatype

import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KClass

/**
 * Abstract data type implementation that provides generic methods that are
 * appropriate for most data type implementations. Among those is the
 * generic implementation of the [.compare] method.
 */
abstract class AbstractDataType<T: Any>(
    private val name: String,
    override val sqlType: Int,
    override val typeClass: KClass<T>,
    override val isNumber: Boolean,
    override val isDateTime: Boolean
) : DataType<T> {
    inline fun <reified T: Any> typeClass(): KClass<T> {
        return T::class
    }

    inline fun <reified T: Comparable<T>> compare(o1: T, o2: T): Int {
        return o1.compareTo(o2)
    }

    override fun typeCast(value: Any): Any? {
        TODO("Not yet implemented")
    }

    /**
     * Checks whether the given objects are equal or not.
     * @param o1 first object
     * @param o2 second object
     * @return `true` if both objects are `null` (and hence equal)
     * or if the `o1.equals(o2)` is `true`.
     */
    protected fun areObjectsEqual(o1: Any?, o2: Any?): Boolean {
        if (o1 == null && o2 == null) {
            return true
        }
        return if (o1 != null && o1 == o2) {
            true
        } else false
        // Note that no more check is needed for o2 because it definitely does is not equal to o1
        // Instead immediately proceed with the typeCast method
    }

    /**
     * @param clazz The fully qualified name of the class to be loaded
     * @param connection The JDBC connection needed to load the given class
     * @return The loaded class
     * @throws ClassNotFoundException
     */
    @Throws(ClassNotFoundException::class)
    protected fun loadClass(clazz: String?, connection: Connection): Class<*> {
        val connectionClassLoader = connection.javaClass.classLoader
        return this.loadClass(clazz, connectionClassLoader)
    }

    /**
     * @param clazz The fully qualified name of the class to be loaded
     * @param classLoader The classLoader to be used to load the given class
     * @return The loaded class
     * @throws ClassNotFoundException
     */
    @Throws(ClassNotFoundException::class)
    protected fun loadClass(clazz: String?, classLoader: ClassLoader): Class<*> {
        return classLoader.loadClass(clazz)
    }

    override fun toString(): String {
        return name
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractDataType::class.java)
    }
}
