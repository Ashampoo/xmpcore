// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.options.ParseOptions
import com.ashampoo.xmp.options.SerializeOptions

/**
 * Creates `XMPMeta`-instances from an `InputStream`
 */
object XMPMetaFactory {

    @kotlin.jvm.JvmStatic
    val schemaRegistry = XMPSchemaRegistry

    @kotlin.jvm.JvmStatic
    val versionInfo = XMPVersionInfo

    @kotlin.jvm.JvmStatic
    fun create(): XMPMeta = XMPMeta()

    @kotlin.jvm.JvmStatic
    @Throws(XMPException::class)
    fun parseFromString(
        packet: String,
        options: ParseOptions? = null
    ): XMPMeta =
        XMPMetaParser.parse(packet, options)

    @kotlin.jvm.JvmStatic
    @Throws(XMPException::class)
    fun serializeToString(
        xmp: XMPMeta,
        options: SerializeOptions? = null
    ): String {

        val actualOptions = options ?: SerializeOptions()

        /* sort the internal data model on demand */
        if (actualOptions.getSort())
            xmp.sort()

        return XMPRDFWriter.serialize(xmp, actualOptions)
    }
}
