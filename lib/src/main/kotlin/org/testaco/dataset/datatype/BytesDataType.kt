package org.testaco.dataset.datatype

import org.slf4j.LoggerFactory
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.sql.Blob
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.regex.Pattern

open class BytesDataType(name: String?, sqlType: Int) :
    AbstractDataType<ByteArray>(name!!, sqlType, ByteArray::class, false, false) {

    @Throws(IOException::class)
    private fun toByteArray(input: InputStream, length: Int): ByteArray {
        val out = ByteArrayOutputStream(length)
        val bufferedInput = BufferedInputStream(input)
        var i = bufferedInput.read()
        while (i != -1) {
            out.write(i)
            i = bufferedInput.read()
        }
        return out.toByteArray()
    }

    @Throws(IOException::class)
    fun loadFile(filename: String): ByteArray {
        // Not an URL, try as file name
        val file = File(filename)
        return toByteArray(FileInputStream(file), file.length().toInt())
    }

    @Throws(IOException::class)
    fun loadURL(urlAsString: String?): ByteArray {
        // Not an URL, try as file name
        val url = URL(urlAsString)
        return toByteArray(url.openStream(), 0)
    }

    override fun typeCast(value: Any): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value is ByteArray) {
            return value
        }
        if (value is String) {
            var stringValue = value.toString()

            // If the string starts with <text [encoding id]>, it means that the user
            // intentionally wants to transform the text into a blob.
            //
            // Example of a valid string:  "<text UTF-8>This is a valid string with the accent 'é'"
            if (DataType.isExtendedSyntax(stringValue)) {
                val matcher = inputPattern.matcher(stringValue)
                if (matcher.matches()) {
                    val commandLine = matcher.group(1).uppercase(Locale.getDefault())
                    stringValue = matcher.group(2)
                    val split = commandLine.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val command = split[0]
                    when (command) {
                        "TEXT" -> {
                            var encoding = "UTF-8" // Default
                            if (split.size > 1) {
                                encoding = split[1]
                            }
                            logger.debug(
                                "Data explicitly states that given string is text encoded "
                                        + encoding
                            )
                            return try {
                                stringValue.toByteArray(charset(encoding))
                            } catch (unsupportedEncodingException: UnsupportedEncodingException) {
                                "Error:  [text " + encoding + "] has an invalid encoding id.".toByteArray()
                            }
                        }
                        "BASE64" -> {
                            logger.debug("Data explicitly states that given string is base46")
                            return Base64.getDecoder().decode(stringValue)
                        }
                        "FILE" -> {
                            return try {
                                logger.debug("Data explicitly states that given string is a file name")
                                loadFile(stringValue)
                            } catch (e: IOException) {
                                val errMsg = "Could not load file following instruction >>$value<<"
                                logger.error(errMsg)
                                "Error:  $errMsg".toByteArray()
                            }
                        }
                        "URL" -> {
                            return try {
                                logger.debug("Data explicitly states that given string is a URL")
                                loadURL(stringValue)
                            } catch (e: IOException) {
                                val errMsg = "Could not load URL following instruction >>$value<<"
                                logger.error(errMsg)
                                "Error:  $errMsg".toByteArray()
                            }
                        }
                    }
                }
            }

            // Assume not an uri if length greater than max uri length
            if (stringValue.length == 0 || stringValue.length > MAX_URI_LENGTH) {
                logger.debug("Assuming given string to be Base64 and not a URI")
                return Base64.getDecoder().decode(value)
            }
            return try {
                logger.debug("Assuming given string to be a URI")
                try {
                    // Try value as URL
                    loadURL(stringValue)
                } catch (e1: MalformedURLException) {
                    logger.debug("Given string is not a valid URI - trying to resolve it as file...")
                    try {
                        // Not an URL, try as file name
                        loadFile(stringValue)
                    } catch (e2: FileNotFoundException) {
                        logger.debug("Assuming given string to be Base64 and not a URI or File")
                        // Not a file name either
                        Base64.getDecoder().decode(stringValue)
                    }
                }
            } catch (e: IOException) {
                throw TypeCastException(value, this, e)
            }
        }
        if (value is Blob) {
            return try {
                val blobValue: Blob = value
                if (blobValue.length() == 0L) {
                    null
                } else blobValue.getBytes(1, blobValue.length().toInt())
            } catch (e: SQLException) {
                throw TypeCastException(value, this, e)
            }
        }
        if (value is URL) {
            return try {
                toByteArray(value.openStream(), 0)
            } catch (e: IOException) {
                throw TypeCastException(value, this, e)
            }
        }
        if (value is File) {
            return try {
                val file: File = value
                toByteArray(FileInputStream(file), file.length().toInt())
            } catch (e: IOException) {
                throw TypeCastException(value, this, e)
            }
        }
        throw TypeCastException(value, this)
    }

    @Throws(TypeCastException::class)
    protected fun compareNonNulls(value1: Any, value2: Any): Int {
        logger.debug("compareNonNulls(value1={}, value2={}) - start", value1, value2)

        val value1cast = typeCast(value1) as ByteArray?
        val value2cast = typeCast(value2) as ByteArray?
        return compare(value1cast, value2cast)
    }

    @Throws(TypeCastException::class)
    fun compare(v1: ByteArray?, v2: ByteArray?): Int {
        if (logger.isDebugEnabled) {
            logger.debug("compare(v1={}, v2={}) - start", v1, v2)
        }
        val len1 = v1!!.size
        val len2 = v2!!.size
        val n = Math.min(len1, len2)
        val i = 0
        var k = i
        val lim = n + i
        while (k < lim) {
            val c1 = v1[k]
            val c2 = v2[k]
            if (c1 != c2) {
                return c1 - c2
            }
            k++
        }
        return len1 - len2
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): ByteArray? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value: ByteArray = resultSet.getBytes(column)
        return if (resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    @Throws(SQLException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "setSqlValue(value={}, column={}, statement={}) - start",
                *arrayOf(value, column, statement)
            )
        }
        statement.setObject(column, typeCast(value), sqlType)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BytesDataType::class.java)
        private const val MAX_URI_LENGTH = 256
        private val inputPattern = Pattern.compile("^\\[(.*?)](.*)")
    }
}
