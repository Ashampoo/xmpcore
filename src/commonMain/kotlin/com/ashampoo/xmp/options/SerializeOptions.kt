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
 * Options for [XMPMetaFactory.serializeToBuffer].
 */
public class SerializeOptions : Options {

    /**
     * Default constructor.
     */
    constructor()

    /**
     * Constructor using inital options
     *
     * @param options the inital options
     *
     */
    constructor(options: Int) : super(options)

    fun getOmitPacketWrapper(): Boolean =
        getOption(OMIT_PACKET_WRAPPER)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setOmitPacketWrapper(value: Boolean): SerializeOptions {
        setOption(OMIT_PACKET_WRAPPER, value)
        return this
    }

    fun getOmitXmpMetaElement(): Boolean =
        getOption(OMIT_XMPMETA_ELEMENT)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setOmitXmpMetaElement(value: Boolean): SerializeOptions {
        setOption(OMIT_XMPMETA_ELEMENT, value)
        return this
    }

    fun getReadOnlyPacket(): Boolean =
        getOption(READONLY_PACKET)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setReadOnlyPacket(value: Boolean): SerializeOptions {
        setOption(READONLY_PACKET, value)
        return this
    }

    fun getUseCompactFormat(): Boolean =
        getOption(USE_COMPACT_FORMAT)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setUseCompactFormat(value: Boolean): SerializeOptions {
        setOption(USE_COMPACT_FORMAT, value)
        return this
    }

    fun getUseCanonicalFormat(): Boolean =
        getOption(USE_CANONICAL_FORMAT)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setUseCanonicalFormat(value: Boolean): SerializeOptions {
        setOption(USE_CANONICAL_FORMAT, value)
        return this
    }

    fun getSort(): Boolean =
        getOption(SORT)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setSort(value: Boolean): SerializeOptions {
        setOption(SORT, value)
        return this
    }

    /**
     * @return Returns clone of this SerializeOptions-object with the same options set.
     */
    fun clone(): SerializeOptions =
        SerializeOptions(getOptions())

    /**
     * @see Options.defineOptionName
     */
    override fun defineOptionName(option: Int): String? {
        return when (option) {
            OMIT_PACKET_WRAPPER -> "OMIT_PACKET_WRAPPER"
            READONLY_PACKET -> "READONLY_PACKET"
            USE_COMPACT_FORMAT -> "USE_COMPACT_FORMAT"
            OMIT_XMPMETA_ELEMENT -> "OMIT_XMPMETA_ELEMENT"
            SORT -> "NORMALIZED"
            else -> null
        }
    }

    /**
     * @see Options.getValidOptions
     */
    override fun getValidOptions(): Int =
        OMIT_PACKET_WRAPPER or READONLY_PACKET or USE_COMPACT_FORMAT or OMIT_XMPMETA_ELEMENT or SORT

    companion object {

        /**
         * Omit the XML packet wrapper.
         */
        const val OMIT_PACKET_WRAPPER = 0x0010

        /**
         * Mark packet as read-only. Default is a writeable packet.
         */
        const val READONLY_PACKET = 0x0020

        /**
         * Use a compact form of RDF.
         * The compact form is the default serialization format (this flag is technically ignored).
         * To serialize to the canonical form, set the flag USE_CANONICAL_FORMAT.
         * If both flags &quot;compact&quot; and &quot;canonical&quot; are set, canonical is used.
         */
        const val USE_COMPACT_FORMAT = 0x0040

        /**
         * Use the canonical form of RDF if set. By default the compact form is used
         */
        const val USE_CANONICAL_FORMAT = 0x0080

        /**
         * Omit the &lt;x:xmpmeta&gt;-tag
         */
        const val OMIT_XMPMETA_ELEMENT = 0x1000

        /**
         * Sort the struct properties and qualifier before serializing
         */
        const val SORT = 0x2000
    }
}
