package org.testaco.dataset.datatype

import java.util.*
import java.util.regex.Pattern

/**
 * A datatype that is capable of storing UUIDs into BINARY fields (big-endian).
 *
 * For the UUID to be detected as such, the string value of the field has to be
 * in the form of `uuid'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'`, where the
 * x's are the actual string value of the UUID, hex-encoded. Example:
 * <pre>
 * &lt;company id="uuid'791ae85a-d8d0-11e2-8c43-50e549c9b654'" name="ACME"/&gt;
</pre> *
 */
class UuidAwareBytesDataType internal constructor(name: String?, sqlType: Int) : BytesDataType(name, sqlType) {
    @Throws(TypeCastException::class)
    override fun typeCast(value: Any): Any? {
        return super.typeCast(uuidAwareValueOf(value))
    }

    companion object {
        /**
         * The regular expression for a hexadecimal UUID representation.
         */
        private val UUID_RE =
            Pattern.compile("uuid'([0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12})'")

        private fun uuidAwareValueOf(value: Any): Any {
            if (value is String) {
                val m = UUID_RE.matcher(value)
                if (m.find()) {
                    val uuid = UUID.fromString(m.group(1))
                    return uuidToBytes(uuid)
                }
            }
            return value
        }

        private fun uuidToBytes(uuid: UUID): ByteArray {
            val msb = uuid.mostSignificantBits
            val lsb = uuid.leastSignificantBits
            return byteArrayOf(
                extractByte(msb, 0), extractByte(msb, 1),
                extractByte(msb, 2), extractByte(msb, 3), extractByte(msb, 4),
                extractByte(msb, 5), extractByte(msb, 6), extractByte(msb, 7),
                extractByte(lsb, 0), extractByte(lsb, 1), extractByte(lsb, 2),
                extractByte(lsb, 3), extractByte(lsb, 4), extractByte(lsb, 5),
                extractByte(lsb, 6), extractByte(lsb, 7)
            )
        }

        private fun extractByte(value: Long, byteIndex: Int): Byte {
            return (value shr 56 - byteIndex * 8 and 0xffL).toByte()
        }
    }
}
