// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

object XMPError {

    const val EMPTY_SCHEMA_TEXT: String = "Empty schema namespace URI"
    const val EMPTY_CONVERT_STRING_TEXT: String = "Empty convert-string"

    const val UNKNOWN: Int = 0
    const val BADPARAM: Int = 4
    const val BADVALUE: Int = 5
    const val INTERNALFAILURE: Int = 9
    const val BADSCHEMA: Int = 101
    const val BADXPATH: Int = 102
    const val BADOPTIONS: Int = 103
    const val BADINDEX: Int = 104
    const val BADSERIALIZE: Int = 107
    const val BADXML: Int = 201
    const val BADRDF: Int = 202
    const val BADXMP: Int = 203
    const val BADSTREAM: Int = 204
}
