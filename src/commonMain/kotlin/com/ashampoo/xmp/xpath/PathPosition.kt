// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.xpath

/**
 * This objects contains all needed char positions to parse.
 */
internal class PathPosition {

    /**
     * the complete path
     */
    var path: String? = null

    /**
     * the start of a segment name
     */
    var nameStart = 0

    /**
     * the end of a segment name
     */
    var nameEnd = 0

    /**
     * the begin of a step
     */
    var stepBegin = 0

    /**
     * the end of a step
     */
    var stepEnd = 0

}
