// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.internal

import com.ashampoo.xmp.XMPException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Utility methods for XMP. I included only those that are different from the
 * Java default conversion utilities.
 */
internal object XMPUtils {

    /**
     * Convert from string to Boolean.
     *
     * @param value The string representation of the Boolean.
     * @return The appropriate boolean value for the string.
     *         The checked values for `true` and `false` are:
     *  * [XMPConst.TRUE_STRING] and [XMPConst.FALSE_STRING]
     *  * &quot;t&quot; and &quot;f&quot;
     *  * &quot;on&quot; and &quot;off&quot;
     *  * &quot;yes&quot; and &quot;no&quot;
     *  * &quot;value != 0&quot; and &quot;value == 0&quot;
     */
    @kotlin.jvm.JvmStatic
    fun convertToBoolean(value: String?): Boolean {

        if (value.isNullOrEmpty())
            throw XMPException(XMPErrorConst.EMPTY_CONVERT_STRING_TEXT, XMPErrorConst.BADVALUE)

        val valueLowercase = value.lowercase()

        try {

            /* First try interpretation as Integer (anything not 0 is true) */
            return valueLowercase.toInt() != 0

        } catch (ex: NumberFormatException) {

            /* Fallback to other common true values */
            return "true" == valueLowercase || "t" == valueLowercase ||
                "on" == valueLowercase || "yes" == valueLowercase
        }
    }

    @kotlin.jvm.JvmStatic
    fun convertToInteger(rawValue: String?): Int {
        try {

            if (rawValue.isNullOrEmpty())
                throw XMPException(XMPErrorConst.EMPTY_CONVERT_STRING_TEXT, XMPErrorConst.BADVALUE)

            return if (rawValue.startsWith("0x"))
                rawValue.substring(2).toInt(16)
            else
                rawValue.toInt()

        } catch (ex: NumberFormatException) {
            throw XMPException("Invalid integer string", XMPErrorConst.BADVALUE, ex)
        }
    }

    @kotlin.jvm.JvmStatic
    fun convertToLong(rawValue: String?): Long {

        try {

            if (rawValue.isNullOrEmpty())
                throw XMPException(XMPErrorConst.EMPTY_CONVERT_STRING_TEXT, XMPErrorConst.BADVALUE)

            return if (rawValue.startsWith("0x"))
                rawValue.substring(2).toLong(16)
            else
                rawValue.toLong()

        } catch (ex: NumberFormatException) {
            throw XMPException("Invalid long string", XMPErrorConst.BADVALUE, ex)
        }
    }

    @kotlin.jvm.JvmStatic
    fun convertToDouble(rawValue: String?): Double {

        try {

            if (rawValue.isNullOrEmpty())
                throw XMPException(XMPErrorConst.EMPTY_CONVERT_STRING_TEXT, XMPErrorConst.BADVALUE)

            return rawValue.toDouble()

        } catch (ex: NumberFormatException) {
            throw XMPException("Invalid double string", XMPErrorConst.BADVALUE, ex)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @kotlin.jvm.JvmStatic
    fun encodeBase64(buffer: ByteArray): String =
        Base64.encode(buffer)

    @OptIn(ExperimentalEncodingApi::class)
    @kotlin.jvm.JvmStatic
    fun decodeBase64(base64String: String): ByteArray {

        try {

            return Base64.decode(base64String.encodeToByteArray())

        } catch (ex: Throwable) {
            throw XMPException("Invalid base64 string", XMPErrorConst.BADVALUE, ex)
        }
    }
}
