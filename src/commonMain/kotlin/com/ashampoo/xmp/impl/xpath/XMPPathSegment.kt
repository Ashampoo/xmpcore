// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.impl.xpath

/**
 * A segment of a parsed `XMPPath`.
 */
class XMPPathSegment {

    /**
     * name of the path segment
     */
    var name: String?

    /**
     * kind of the path segment
     */
    var kind = 0

    /**
     * flag if segment is an alias
     */
    var isAlias = false

    /**
     * alias form if applicable
     */
    var aliasForm = 0

    /**
     * Constructor with initial values.
     */
    constructor(name: String) {
        this.name = name
    }

    /**
     * Constructor with initial values.
     *
     * Note: Name can be NULL for XMPPath.ARRAY_INDEX_STEP and others.
     */
    constructor(name: String?, kind: Int) {
        this.name = name
        this.kind = kind
    }

    override fun toString(): String =
        name ?: "null"

}
