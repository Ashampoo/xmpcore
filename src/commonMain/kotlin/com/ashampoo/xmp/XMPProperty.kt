// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.options.PropertyOptions

/**
 * This interface is used to return a text property together with its and options.
 */
public interface XMPProperty {

    /**
     * @return Returns the value of the property.
     */
    public fun getValue(): String?

    /**
     * @return Returns the options of the property.
     */
    public fun getOptions(): PropertyOptions

    /**
     * Only set by {@link XMPMeta.getLocalizedText}.
     *
     * @return Returns the language of the alt-text item.
     */
    public fun getLanguage(): String?

}
