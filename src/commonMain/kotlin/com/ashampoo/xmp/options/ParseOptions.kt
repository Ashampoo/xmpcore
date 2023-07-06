// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.options

/**
 * Options for [XMPMetaFactory.parse].
 */
class ParseOptions : Options() {

    /**
     * @return Returns the requireXMPMeta.
     */
    fun getRequireXMPMeta(): Boolean =
        getOption(REQUIRE_XMP_META)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setRequireXMPMeta(value: Boolean): ParseOptions {
        setOption(REQUIRE_XMP_META, value)
        return this
    }

    /**
     * @return Returns the strictAliasing.
     */
    fun getStrictAliasing(): Boolean =
        getOption(STRICT_ALIASING)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setStrictAliasing(value: Boolean): ParseOptions {
        setOption(STRICT_ALIASING, value)
        return this
    }

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setOmitNormalization(value: Boolean): ParseOptions {
        setOption(OMIT_NORMALIZATION, value)
        return this
    }

    /**
     * @return Returns the option "omit normalization".
     */
    fun getOmitNormalization(): Boolean =
        getOption(OMIT_NORMALIZATION)

    /**
     * @see Options.defineOptionName
     */
    override fun defineOptionName(option: Int): String? {
        return when (option) {
            REQUIRE_XMP_META -> "REQUIRE_XMP_META"
            STRICT_ALIASING -> "STRICT_ALIASING"
            OMIT_NORMALIZATION -> "OMIT_NORMALIZATION"
            else -> null
        }
    }

    /**
     * @see Options.getValidOptions
     */
    override fun getValidOptions(): Int =
        REQUIRE_XMP_META or STRICT_ALIASING or OMIT_NORMALIZATION

    /**
     * Require a surrounding "x:xmpmeta" element in the xml-document.
     */
    companion object {

        const val REQUIRE_XMP_META = 0x0001

        /**
         * Do not reconcile alias differences, throw an exception instead.
         */
        const val STRICT_ALIASING = 0x0004

        /**
         * Do not carry run the XMPNormalizer on a packet, leave it as it is.
         */
        const val OMIT_NORMALIZATION = 0x0020
    }
}
