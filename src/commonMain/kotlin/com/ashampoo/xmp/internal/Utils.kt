// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.internal

import com.ashampoo.xmp.XMPConst

/**
 * Utility functions for the XMPToolkit implementation.
 */
internal object Utils {

    /**
     * segments of a UUID
     */
    const val UUID_SEGMENT_COUNT = 4

    /**
     * length of a UUID
     */
    const val UUID_LENGTH = 32 + UUID_SEGMENT_COUNT

    const val HEX_RADIX = 16

    private const val XML_NAME_LENGTH = 0x0100

    /**
     * table of XML name start chars (<= 0xFF)
     */
    private val xmlNameStartChars = BooleanArray(XML_NAME_LENGTH)

    /**
     * table of XML name chars (<= 0xFF)
     */
    private val xmlNameChars = BooleanArray(XML_NAME_LENGTH)

    private val controlCharRegex = Regex("[\\p{Cntrl}]")

    /** init char tables  */
    init {
        initCharTables()
    }

    /**
     * Normalize an xml:lang value so that comparisons are effectively case
     * insensitive as required by RFC 3066 (which superceeds RFC 1766). The
     * normalization rules:
     *
     *  *  The primary subtag is lower case, the suggested practice of ISO 639.
     *  *  All 2 letter secondary subtags are upper case, the suggested practice of ISO 3166.
     *  *  All other subtags are lower case.
     *
     * @param value raw value
     * @return Returns the normalized value.
     */
    @kotlin.jvm.JvmStatic
    fun normalizeLangValue(value: String): String {

        /* Don't normalize x-default */
        if (XMPConst.X_DEFAULT == value)
            return value

        var subTag = 1
        val buffer = StringBuilder()

        for (i in 0 until value.length) {

            when (value[i]) {

                '-', '_' -> {
                    /* Move to next subtag and convert underscore to hyphen */
                    buffer.append('-')
                    subTag++
                }

                ' ' -> {
                    /* Leave as is. */
                }

                else -> {

                    /* Convert second subtag to uppercase, all other to lowercase */
                    if (subTag != 2)
                        buffer.append(value[i].lowercaseChar())
                    else
                        buffer.append(value[i].uppercaseChar())
                }
            }
        }

        return buffer.toString()
    }

    /**
     * Split the name and value parts for field and qualifier selectors:
     *
     *  * [qualName="value"] - An element in an array of structs, chosen by a field value.
     *  * [?qualName="value"] - An element in an array, chosen by a qualifier value.
     *
     * The value portion is a string quoted by ''' or '"'.
     * The value may contain any character including a doubled quoting character.
     * The value may be empty.
     *
     * *Note:* It is assumed that the expression is formal correct
     *
     * @param selector the selector
     * @return Returns an array where the first entry contains the name and the second the value.
     */
    @kotlin.jvm.JvmStatic
    fun splitNameAndValue(selector: String): Array<String> {

        // get the name
        val eq = selector.indexOf('=')

        var pos = 1

        if (selector[pos] == '?')
            pos++

        val name = selector.substring(pos, eq)

        // get the value
        pos = eq + 1

        val quote = selector[pos]

        pos++

        val end = selector.length - 2 // quote and ]

        val value = StringBuilder(end - eq)

        while (pos < end) {

            value.append(selector[pos])

            pos++

            if (selector[pos] == quote) {
                // skip one quote in value
                pos++
            }
        }

        return arrayOf(name, value.toString())
    }

    /**
     * Check some requirements for an UUID:
     *
     *  * Length of the UUID is 32
     *  * The Delimiter count is 4 and all the 4 delimiter are on their right position (8, 13, 18, 23)
     *
     * @param uuid uuid to test
     * @return true - this is a well formed UUID, false - UUID has not the expected format
     */
    @kotlin.jvm.JvmStatic
    @Suppress("MagicNumber")
    fun checkUUIDFormat(uuid: String?): Boolean {

        var result = true
        var delimCnt = 0
        var delimPos = 0

        if (uuid == null)
            return false

        while (delimPos < uuid.length) {

            if (uuid[delimPos] == '-') {

                delimCnt++

                result = result && (delimPos == 8 || delimPos == 13 || delimPos == 18 || delimPos == 23)
            }

            delimPos++
        }

        return result && UUID_SEGMENT_COUNT == delimCnt && UUID_LENGTH == delimPos
    }

    /**
     * Simple check for valid XMLNames. Within ASCII range
     * ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6]
     * are accepted, above all characters
     * (which is not entirely correct according to the XML Spec).
     *
     * @param name an XML Name
     * @return Return `true` if the name is correct.
     */
    fun isXMLName(name: String): Boolean {

        if (name.isNotEmpty() && !isNameStartChar(name[0]))
            return false

        for (i in 1 until name.length)
            if (!isNameChar(name[i]))
                return false

        return true
    }

    /**
     * Checks if the value is a legal "unqualified" XML name, as
     * defined in the XML Namespaces proposed recommendation.
     * These are XML names, except that they must not contain a colon.
     *
     * @param name the value to check
     * @return Returns true if the name is a valid "unqualified" XML name.
     */
    @kotlin.jvm.JvmStatic
    fun isXMLNameNS(name: String): Boolean {

        if (name.isNotEmpty() && (!isNameStartChar(name[0]) || name[0] == ':'))
            return false

        for (index in 1 until name.length)
            if (!isNameChar(name[index]) || name[index] == ':')
                return false

        return true
    }

    /**
     * Serializes the node value in XML encoding. Its used for tag bodies and attributes.
     *
     * *Note:* The attribute is always limited by quotes, thats why `&apos;` is never serialized.
     *
     * *Note:* Control chars are written unescaped, but if the user uses others than tab, LF
     * and CR the resulting XML will become invalid.
     *
     * @param value             a string
     * @param forAttribute      flag if string is attribute value (need to additional escape quotes)
     * @param escapeWhitespaces Decides if LF, CR and TAB are escaped.
     * @return Returns the value ready for XML output.
     */
    @kotlin.jvm.JvmStatic
    @Suppress("ComplexCondition", "kotlin:S3776")
    fun escapeXML(value: String, forAttribute: Boolean, escapeWhitespaces: Boolean): String {

        // quick check if character are contained that need special treatment
        var needsEscaping = false

        for (index in 0 until value.length) {

            val char = value[index]

            val isControlChar = char == '\t' || char == '\n' || char == '\r'

            if (
                char == '<' || char == '>' || char == '&' ||
                escapeWhitespaces && isControlChar || forAttribute && char == '"'
            ) {
                needsEscaping = true
                break
            }
        }

        if (!needsEscaping)
            return value

        // slow path with escaping
        val buffer = StringBuilder(value.length * 4 / 3)

        @Suppress("LoopWithTooManyJumpStatements")
        for (char in value) {

            val isControlChar = char == '\t' || char == '\n' || char == '\r'

            if (!(escapeWhitespaces && isControlChar)) {

                when (char) {

                    '<' -> {
                        buffer.append("&lt;")
                        continue
                    }

                    '>' -> {
                        buffer.append("&gt;")
                        continue
                    }

                    '&' -> {
                        buffer.append("&amp;")
                        continue
                    }

                    '"' -> {
                        buffer.append(if (forAttribute) "&quot;" else "\"")
                        continue
                    }

                    else -> {
                        buffer.append(char)
                        continue
                    }
                }

            } else {

                // write control chars escaped,
                // if there are others than tab, LF and CR the xml will become invalid.
                buffer.append("&#x")
                buffer.append(char.code.toString(HEX_RADIX).uppercase())
                buffer.append(';')
            }
        }

        return buffer.toString()
    }

    /**
     * Replaces the ASCII control chars with a space.
     *
     * @param value a node value
     * @return Returns the cleaned up value
     */
    @kotlin.jvm.JvmStatic
    fun replaceControlCharsWithSpace(value: String): String =
        value.replace(controlCharRegex, " ")

    /**
     * Simple check if a character is a valid XML start name char.
     * All characters according to the XML Spec 1.1 are accepted:
     * http://www.w3.org/TR/xml11/#NT-NameStartChar
     *
     * @param char a character
     * @return Returns true if the character is a valid first char of an XML name.
     */
    @Suppress("MagicNumber", "kotlin:S3776")
    private fun isNameStartChar(char: Char): Boolean =
        char.code <= 0xFF && xmlNameStartChars[char.code] ||
            char.code >= 0x100 && char.code <= 0x2FF ||
            char.code >= 0x370 && char.code <= 0x37D ||
            char.code >= 0x37F && char.code <= 0x1FFF ||
            char.code >= 0x200C && char.code <= 0x200D ||
            char.code >= 0x2070 && char.code <= 0x218F ||
            char.code >= 0x2C00 && char.code <= 0x2FEF ||
            char.code >= 0x3001 && char.code <= 0xD7FF ||
            char.code >= 0xF900 && char.code <= 0xFDCF ||
            char.code >= 0xFDF0 && char.code <= 0xFFFD ||
            char.code >= 0x10000 && char.code <= 0xEFFFF

    /**
     * Simple check if a character is a valid XML name char
     * (every char except the first one), according to the XML Spec 1.1:
     * http://www.w3.org/TR/xml11/#NT-NameChar
     *
     * @param char a character
     * @return Returns true if the character is a valid char of an XML name.
     */
    @Suppress("MagicNumber")
    private fun isNameChar(char: Char): Boolean =
        char.code <= 0xFF && xmlNameChars[char.code] ||
            isNameStartChar(char) ||
            char.code >= 0x300 && char.code <= 0x36F ||
            char.code >= 0x203F && char.code <= 0x2040

    /**
     * Initializes the char tables for the chars 0x00-0xFF for later use,
     * according to the XML 1.1 specification at http://www.w3.org/TR/xml11
     */
    @Suppress("MagicNumber")
    private fun initCharTables() {

        var char = 0.toChar()

        while (char < xmlNameChars.size.toChar()) {

            xmlNameStartChars[char.code] = char == ':' ||
                'A' <= char && char <= 'Z' ||
                char == '_' ||
                'a' <= char && char <= 'z' ||
                0xC0 <= char.code && char.code <= 0xD6 ||
                0xD8 <= char.code && char.code <= 0xF6 ||
                0xF8 <= char.code && char.code <= 0xFF

            xmlNameChars[char.code] = xmlNameStartChars[char.code] ||
                char == '-' ||
                char == '.' ||
                '0' <= char && char <= '9' ||
                char.code == 0xB7

            char++
        }
    }
}
