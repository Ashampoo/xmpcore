// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

/**
 * This exception wraps all errors that occur in the XMP Toolkit.
 */
class XMPException(
    message: String,
    val errorCode: Int,
    cause: Throwable? = null
) : Exception(message, cause)
