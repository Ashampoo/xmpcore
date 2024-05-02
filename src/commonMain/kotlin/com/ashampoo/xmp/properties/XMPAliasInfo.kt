// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.properties

import com.ashampoo.xmp.options.AliasOptions

/**
 * This interface is used to return info about an alias.
 */
public interface XMPAliasInfo {

    /**
     * @return Returns the namespace URI for the base property.
     */
    public fun getNamespace(): String

    /**
     * @return Returns the default prefix for the given base property.
     */
    public fun getPrefix(): String

    /**
     * @return Returns the path of the base property.
     */
    public fun getPropName(): String

    /**
     * @return Returns the kind of the alias. This can be a direct alias
     * (ARRAY), a simple property to an ordered array
     * (ARRAY_ORDERED), to an alternate array
     * (ARRAY_ALTERNATE) or to an alternate text array
     * (ARRAY_ALT_TEXT).
     */
    public fun getAliasForm(): AliasOptions

}
