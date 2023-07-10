// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.impl.XMPMetaImpl
import com.ashampoo.xmp.impl.XMPMetaParser
import com.ashampoo.xmp.impl.XMPRDFWriter
import com.ashampoo.xmp.impl.XMPSchemaRegistryImpl
import com.ashampoo.xmp.options.ParseOptions
import com.ashampoo.xmp.options.SerializeOptions

/**
 * Creates `XMPMeta`-instances from an `InputStream`
 */
object XMPMetaFactory {

    @kotlin.jvm.JvmStatic
    val schemaRegistry = XMPSchemaRegistryImpl

    @kotlin.jvm.JvmStatic
    val versionInfo = XMPVersionInfo

    fun create(): XMPMeta = XMPMetaImpl()

    @Throws(XMPException::class)
    fun parseFromString(
        packet: String,
        options: ParseOptions = ParseOptions()
    ): XMPMeta =
        XMPMetaParser.parse(packet, options)

    @Throws(XMPException::class)
    fun serializeToString(
        xmp: XMPMeta,
        options: SerializeOptions = SerializeOptions()
    ): String {

        require(xmp is XMPMetaImpl) { "Serialization only works with XMPMetaImpl" }

        /* sort the internal data model on demand */
        if (options.getSort())
            xmp.sort()

        return XMPRDFWriter(xmp, options).serialize()
    }
}
