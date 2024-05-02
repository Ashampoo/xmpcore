// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.properties

import com.ashampoo.xmp.options.PropertyOptions

/**
 * This interface is used to return a property together with its path and namespace.
 * It is returned when properties are iterated with the <code>XMPIterator</code>.
 */
public interface XMPPropertyInfo : XMPProperty {

    /**
     * @return Returns the namespace of the property
     */
    public fun getNamespace(): String

    /**
     * @return Returns the path of the property, but only if returned by the iterator.
     */
    public fun getPath(): String

    /**
     * @return Returns the value of the property.
     */
    override fun getValue(): String

    /**
     * @return Returns the options of the property.
     */
    override fun getOptions(): PropertyOptions

}
