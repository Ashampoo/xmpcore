// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.options

/**
 * Options for XMPIterator construction.
 */
class IteratorOptions : Options() {

    /**
     * @return Returns whether the option is set.
     */
    fun isJustChildren(): Boolean =
        getOption(JUST_CHILDREN)

    /**
     * @return Returns whether the option is set.
     */
    fun isJustLeafname(): Boolean =
        getOption(JUST_LEAFNAME)

    /**
     * @return Returns whether the option is set.
     */
    fun isJustLeafnodes(): Boolean =
        getOption(JUST_LEAFNODES)

    /**
     * @return Returns whether the option is set.
     */
    fun isOmitQualifiers(): Boolean =
        getOption(OMIT_QUALIFIERS)

    /**
     * Sets the option and returns the instance.
     *
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setJustChildren(value: Boolean): IteratorOptions {
        setOption(JUST_CHILDREN, value)
        return this
    }

    /**
     * Sets the option and returns the instance.
     *
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setJustLeafname(value: Boolean): IteratorOptions {
        setOption(JUST_LEAFNAME, value)
        return this
    }

    /**
     * Sets the option and returns the instance.
     *
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setJustLeafnodes(value: Boolean): IteratorOptions {
        setOption(JUST_LEAFNODES, value)
        return this
    }

    /**
     * Sets the option and returns the instance.
     *
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    fun setOmitQualifiers(value: Boolean): IteratorOptions {
        setOption(OMIT_QUALIFIERS, value)
        return this
    }

    /**
     * @see Options.defineOptionName
     */
    override fun defineOptionName(option: Int): String? {
        return when (option) {
            JUST_CHILDREN -> "JUST_CHILDREN"
            JUST_LEAFNODES -> "JUST_LEAFNODES"
            JUST_LEAFNAME -> "JUST_LEAFNAME"
            OMIT_QUALIFIERS -> "OMIT_QUALIFIERS"
            else -> null
        }
    }

    /**
     * @see Options.getValidOptions
     */
    override fun getValidOptions(): Int =
        JUST_CHILDREN or JUST_LEAFNODES or JUST_LEAFNAME or OMIT_QUALIFIERS

    /**
     * Just do the immediate children of the root, default is subtree.
     */
    companion object {

        const val JUST_CHILDREN = 0x0100

        /**
         * Just do the leaf nodes, default is all nodes in the subtree.
         * Bugfix #2658965: If this option is set the Iterator returns the namespace
         * of the leaf instead of the namespace of the base property.
         */
        const val JUST_LEAFNODES = 0x0200

        /**
         * Return just the leaf part of the path, default is the full path.
         */
        const val JUST_LEAFNAME = 0x0400

        /**
         * Omit all qualifiers.
         */
        const val OMIT_QUALIFIERS = 0x1000
    }
}
