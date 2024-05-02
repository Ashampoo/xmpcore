// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.internal

internal data class QName(
    /** XML namespace prefix */
    val prefix: String?,
    /** XML localname */
    val localName: String
) {

    val hasPrefix: Boolean =
        prefix != null && prefix.isNotEmpty()

    companion object {

        /**
         * Splits a qname into prefix and localname.
         *
         * @param qname a QName
         */
        fun parse(qname: String): QName {

            val colon = qname.indexOf(':')

            return if (colon >= 0)
                QName(
                    prefix = qname.substring(0, colon),
                    localName = qname.substring(colon + 1)
                )
            else
                QName(
                    prefix = null,
                    localName = qname
                )
        }
    }
}
