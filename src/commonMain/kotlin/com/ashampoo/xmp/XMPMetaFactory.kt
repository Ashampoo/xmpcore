// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.internal.XMPErrorConst
import com.ashampoo.xmp.internal.XMPMetaParser
import com.ashampoo.xmp.internal.XMPRDFWriter
import com.ashampoo.xmp.options.ParseOptions
import com.ashampoo.xmp.options.SerializeOptions

/**
 * Creates `XMPMeta`-instances from an `InputStream`
 */
public object XMPMetaFactory {

    @kotlin.jvm.JvmStatic
    public val schemaRegistry: XMPSchemaRegistry = XMPSchemaRegistry

    @kotlin.jvm.JvmStatic
    public val versionInfo: XMPVersionInfo = XMPVersionInfo

    @kotlin.jvm.JvmStatic
    public fun create(): XMPMeta = XMPMeta()

    @kotlin.jvm.JvmStatic
    @kotlin.jvm.JvmOverloads
    @Throws(XMPException::class)
    public fun parseFromString(
        packet: String,
        options: ParseOptions? = null
    ): XMPMeta {

        try {

            return XMPMetaParser.parse(packet, options)

        } catch (ex: a) {

            throw ex

        } catch (ex: Exception) {

            /*
             * Ensure that only XMPException is thrown from this method.
             * Wrap all other exceptions accordingly.
             */

            throw XMPException("Parsing error.", XMPErrorConst.UNKNOWN, ex)
        }
    }

    @kotlin.jvm.JvmStatic
    @kotlin.jvm.JvmOverloads
    @Throws(XMPException::class)
    public fun serializeToString(
        xmp: XMPMeta,
        options: SerializeOptions? = null
    ): String {

        try {

            val actualOptions = options ?: SerializeOptions()

            /* sort the internal data model on demand */
            if (actualOptions.getSort())
                xmp.sort()

            return XMPRDFWriter.serialize(xmp, actualOptions)

        } catch (ex: XMPException) {

            throw ex

        } catch (ex: Exception) {

            /*
             * Ensure that only XMPException is thrown from this method.
             * Wrap all other exceptions accordingly.
             */

            throw XMPException("Serializing error.", XMPErrorConst.UNKNOWN, ex)
        }
    }
}
