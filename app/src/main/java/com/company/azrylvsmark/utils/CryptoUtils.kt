package com.company.azrylvsmark.utils

import java.security.MessageDigest

object CryptoUtils {
    private const val HASH_SALT = "azmassange_secure_phone_hash_salt_v1_"

    /**
     * Normalizes phone number:
     * - Removes whitespace, hyphens, parentheses
     * - Handles Indonesian leading '0' or '+620' correctly by stripping trunk prefix 0
     * - Ensures valid E.164 format with '+' prefix
     */
    fun normalizePhoneNumber(rawPhone: String, defaultCountryCode: String = "+62"): String {
        var clean = rawPhone.trim().replace(Regex("[\\s\\-\\(\\)]"), "")
        if (clean.isBlank()) return ""

        val cc = if (defaultCountryCode.startsWith("+")) defaultCountryCode else "+$defaultCountryCode"

        // If it starts with '+', handle any redundant trunk zero after +62
        if (clean.startsWith("+")) {
            if (clean.startsWith("+620")) {
                clean = "+62" + clean.substring(4)
            }
            return clean
        }

        // If it starts with '0', strip leading zero and prepend default country code
        if (clean.startsWith("0")) {
            clean = cc + clean.substring(1)
            return clean
        }

        // If it starts with '62', add '+' and strip any '0' right after '62'
        if (clean.startsWith("62")) {
            clean = if (clean.startsWith("620")) {
                "+62" + clean.substring(3)
            } else {
                "+$clean"
            }
            return clean
        }

        // Otherwise prepend default country code
        return cc + clean
    }

    /**
     * Formats phone number for clean readable presentation:
     * e.g., +62 812-3456-7890
     */
    fun formatDisplayPhoneNumber(normalizedPhone: String): String {
        if (!normalizedPhone.startsWith("+") || normalizedPhone.length < 8) return normalizedPhone
        return if (normalizedPhone.startsWith("+62")) {
            val rest = normalizedPhone.removePrefix("+62")
            when {
                rest.length <= 3 -> "+62 $rest"
                rest.length <= 7 -> "+62 ${rest.substring(0, 3)}-${rest.substring(3)}"
                else -> "+62 ${rest.substring(0, 3)}-${rest.substring(3, 7)}-${rest.substring(7)}"
            }
        } else {
            normalizedPhone
        }
    }

    /**
     * Computes deterministic SHA-256 hash with salt for secure phone lookup.
     * This prevents storing and exposing raw phone numbers in searchable directories.
     */
    fun hashPhoneNumber(phone: String): String {
        val normalized = normalizePhoneNumber(phone)
        val salted = HASH_SALT + normalized
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(salted.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
