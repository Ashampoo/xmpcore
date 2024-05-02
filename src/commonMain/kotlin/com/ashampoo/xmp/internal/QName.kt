// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.internal

internal class QName {

    /**
     * XML namespace prefix
     */
    var prefix: String? = null
        private set

    /**
     * XML localname
     */
    var localName: String? = null
        private set

    /**
     * Splits a qname into prefix and localname.
     *
     * @param qname a QName
     */
    constructor(qname: String) {

        val colon = qname.indexOf(':')

        if (colon >= 0) {
            prefix = qname.substring(0, colon)
            localName = qname.substring(colon + 1)
        } else {
            prefix = ""
            localName = qname
        }
    }

    constructor(prefix: String?, localName: String?) {
        this.prefix = prefix
        this.localName = localName
    }

    fun hasPrefix(): Boolean =
        prefix != null && prefix!!.isNotEmpty()

}
