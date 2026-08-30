package com.example.spoofing

import java.security.SecureRandom
import java.util.Locale

/**
 * Generator for realistic device identifiers based on AxeSpoofer algorithms.
 */
object RandomIdGenerator {

    private val secureRandom = SecureRandom()
    private const val HEX_CHARS = "0123456789abcdef"

    /**
     * Generates a 16-character hexadecimal Android ID (e.g., "3a8f9c12b7e45d60").
     */
    fun generateAndroidId(): String {
        val sb = StringBuilder(16)
        for (i in 0 until 16) {
            sb.append(HEX_CHARS[secureRandom.nextInt(HEX_CHARS.length)])
        }
        return sb.toString()
    }

    /**
     * Generates a 16-character hexadecimal Google Services Framework (GSF) ID.
     */
    fun generateGsfId(): String {
        val sb = StringBuilder(16)
        for (i in 0 until 16) {
            sb.append(HEX_CHARS[secureRandom.nextInt(HEX_CHARS.length)])
        }
        return sb.toString()
    }

    /**
     * Generates a 15-digit IMEI number with a valid Luhn checksum.
     */
    fun generateImei(): String {
        // Standard TAC prefix (Type Allocation Code) for modern smartphones (8 digits)
        val tacPrefixes = listOf("86214305", "35892109", "35492108", "86542106", "35294811")
        val tac = tacPrefixes[secureRandom.nextInt(tacPrefixes.size)]
        val sb = StringBuilder(tac)
        
        // Next 6 digits for serial number
        for (i in 0 until 6) {
            sb.append(secureRandom.nextInt(10))
        }
        
        // 15th digit is Luhn check digit
        val checkDigit = calculateLuhnCheckDigit(sb.toString())
        sb.append(checkDigit)
        return sb.toString()
    }

    /**
     * Generates a valid 6-octet colon-separated WiFi MAC address.
     */
    fun generateMacAddress(): String {
        val mac = ByteArray(6)
        secureRandom.nextBytes(mac)
        // Ensure unicast (LSB of first byte is 0) and locally administered (bit 1 is 1)
        mac[0] = ((mac[0].toInt() and 0xFC) or 0x02).toByte()
        return mac.joinToString(":") { String.format(Locale.US, "%02x", it) }
    }

    /**
     * Generates a random build ID (e.g. AP2A.240805.005.F1 or UP1A.231005.007).
     */
    fun generateBuildId(): String {
        val prefixes = listOf("AP2A", "UP1A", "TP1A", "UQ1A", "UD1A")
        val prefix = prefixes[secureRandom.nextInt(prefixes.size)]
        val dateNum = String.format(Locale.US, "%06d", secureRandom.nextInt(999999))
        val subNum = String.format(Locale.US, "%03d", secureRandom.nextInt(999))
        return "$prefix.$dateNum.$subNum"
    }

    /**
     * Calculates the Luhn check digit for a numeric string.
     */
    private fun calculateLuhnCheckDigit(number: String): Int {
        var sum = 0
        var alternate = true
        for (i in number.length - 1 downTo 0) {
            var n = number[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = (n % 10) + 1
                }
            }
            sum += n
            alternate = !alternate
        }
        return (10 - (sum % 10)) % 10
    }
}
