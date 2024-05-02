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
 * Options for XMPSchemaRegistry#registerAlias.
 */
public class AliasOptions : Options {

    public constructor() : super()

    internal constructor(options: Int) : super(options)

    public fun isSimple(): Boolean =
        getOptions() == PROP_DIRECT

    public fun isArray(): Boolean =
        getOption(PROP_ARRAY)

    public fun setArray(value: Boolean): AliasOptions {
        setOption(PROP_ARRAY, value)
        return this
    }

    public fun isArrayOrdered(): Boolean =
        getOption(PROP_ARRAY_ORDERED)

    public fun setArrayOrdered(value: Boolean): AliasOptions {
        setOption(PROP_ARRAY or PROP_ARRAY_ORDERED, value)
        return this
    }

    public fun isArrayAlternate(): Boolean =
        getOption(PROP_ARRAY_ALTERNATE)

    public fun setArrayAlternate(value: Boolean): AliasOptions {
        setOption(PROP_ARRAY or PROP_ARRAY_ORDERED or PROP_ARRAY_ALTERNATE, value)
        return this
    }

    public fun isArrayAltText(): Boolean =
        getOption(PROP_ARRAY_ALT_TEXT)

    public fun setArrayAltText(value: Boolean): AliasOptions {
        setOption(PROP_ARRAY or PROP_ARRAY_ORDERED or PROP_ARRAY_ALTERNATE or PROP_ARRAY_ALT_TEXT, value)
        return this
    }

    public fun toPropertyOptions(): PropertyOptions =
        PropertyOptions(getOptions())

    protected override fun defineOptionName(option: Int): String? {
        return when (option) {
            PROP_DIRECT -> "PROP_DIRECT"
            PROP_ARRAY -> "ARRAY"
            PROP_ARRAY_ORDERED -> "ARRAY_ORDERED"
            PROP_ARRAY_ALTERNATE -> "ARRAY_ALTERNATE"
            PROP_ARRAY_ALT_TEXT -> "ARRAY_ALT_TEXT"
            else -> null
        }
    }

    protected override fun getValidOptions(): Int =
        PROP_DIRECT or PROP_ARRAY or PROP_ARRAY_ORDERED or PROP_ARRAY_ALTERNATE or PROP_ARRAY_ALT_TEXT

    internal companion object {

        const val PROP_DIRECT = 0

        /**
         * The actual is an unordered array, the alias is to the first element of the array.
         */
        const val PROP_ARRAY = PropertyOptions.ARRAY

        /**
         * The actual is an ordered array, the alias is to the first element of the array.
         */
        const val PROP_ARRAY_ORDERED = PropertyOptions.ARRAY_ORDERED

        /**
         * The actual is an alternate array, the alias is to the first element of the array.
         */
        const val PROP_ARRAY_ALTERNATE = PropertyOptions.ARRAY_ALTERNATE

        /**
         * The actual is an alternate text array, the alias is to the 'x-default' element of the array.
         */
        const val PROP_ARRAY_ALT_TEXT = PropertyOptions.ARRAY_ALT_TEXT
    }
}
