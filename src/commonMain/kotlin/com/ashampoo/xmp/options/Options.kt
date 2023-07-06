// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.options

import com.ashampoo.xmp.XMPError
import com.ashampoo.xmp.XMPException

/**
 * The base class for a collection of 32 flag bits. Individual flags are defined as enum value bit
 * masks. Inheriting classes add convenience accessor methods.
 */
abstract class Options {

    /**
     * the internal int containing all options
     */
    private var valueBits = 0

    /**
     * a map containing the bit names
     */
    private val optionNames = mutableMapOf<Int, String>()

    /**
     * The default constructor.
     */
    protected constructor()

    /**
     * Constructor with the options bit mask.
     *
     * @param options the options bit mask
     *
     */
    protected constructor(options: Int) {
        assertOptionsValid(options)
        setOptions(options)
    }

    protected abstract fun getValidOptions(): Int

    /**
     * Resets the options.
     */
    fun clear() {
        valueBits = 0
    }

    /**
     * @param optionBits an option bitmask
     * @return Returns true, if this object is equal to the given options.
     */
    fun isExactly(optionBits: Int): Boolean =
        getOptions() == optionBits

    /**
     * @param optionBits an option bitmask
     * @return Returns true, if this object contains all given options.
     */
    fun containsAllOptions(optionBits: Int): Boolean =
        getOptions() and optionBits == optionBits

    /**
     * @param optionBits an option bitmask
     * @return Returns true, if this object contain at least one of the given options.
     */
    fun containsOneOf(optionBits: Int): Boolean =
        getOptions() and optionBits != 0

    /**
     * @param optionBit the binary bit or bits that are requested
     * @return Returns if *all* of the requested bits are set or not.
     */
    protected fun getOption(optionBit: Int): Boolean =
        valueBits and optionBit != 0

    /**
     * @param optionBits the binary bit or bits that shall be set to the given value
     * @param value      the boolean value to set
     */
    fun setOption(optionBits: Int, value: Boolean) {
        this.valueBits = if (value)
            this.valueBits or optionBits
        else
            this.valueBits and optionBits.inv()
    }

    /**
     * Is friendly to access it during the tests.
     *
     * @return Returns the options.
     */
    fun getOptions(): Int = valueBits

    /**
     * @param options The options to set.
     *
     */
    fun setOptions(options: Int) {

        assertOptionsValid(options)

        this.valueBits = options
    }

    /**
     * @see Object.equals
     */
    override fun equals(other: Any?): Boolean =
        getOptions() == (other as? Options)?.getOptions()

    /**
     * @see Object.hashCode
     */
    override fun hashCode(): Int = getOptions()

    /**
     * Creates a human readable string from the set options. *Note:* This method is quite
     * expensive and should only be used within tests or as
     *
     * @return Returns a String listing all options that are set to `true` by their name,
     * like &quot;option1 | option4&quot;.
     */
    fun getOptionsString(): String {

        if (valueBits != 0) {

            val sb = StringBuilder()

            var theBits = valueBits

            while (theBits != 0) {

                val oneLessBit = theBits and theBits - 1 // clear rightmost one bit
                val singleBit = theBits xor oneLessBit
                val bitName = getOptionName(singleBit)
                sb.append(bitName)

                if (oneLessBit != 0)
                    sb.append(" | ")

                theBits = oneLessBit
            }

            return sb.toString()

        } else {
            return "<none>"
        }
    }

    /**
     * @return Returns the options as hex bitmask.
     */
    override fun toString(): String =
        "0x" + valueBits.toString(16)

    /**
     * To be implemeted by inheritants.
     *
     * @param option a single, valid option bit.
     * @return Returns a human readable name for an option bit.
     */
    protected abstract fun defineOptionName(option: Int): String?

    /**
     * The inheriting option class can do additional checks on the options.
     * *Note:* For performance reasons this method is only called
     * when setting bitmasks directly.
     * When get- and set-methods are used, this method must be called manually,
     * normally only when the Options-object has been created from a client
     * (it has to be made public therefore).
     *
     * @param options the bitmask to check.
     *
     */
    protected open fun assertConsistency(options: Int) = Unit // empty, no checks

    /**
     * Checks options before they are set.
     * First it is checked if only defined options are used,
     * second the additional [Options.assertConsistency]-method is called.
     *
     * @param options the options to check
     *
     */
    private fun assertOptionsValid(options: Int) {

        val invalidOptions = options and getValidOptions().inv()

        if (invalidOptions == 0)
            assertConsistency(options)
        else
            throw XMPException(
                "The option bit(s) 0x" + invalidOptions.toString(16) + " + are invalid!",
                XMPError.BADOPTIONS
            )
    }

    /**
     * Looks up or asks the inherited class for the name of an option bit.
     * Its save that there is only one valid option handed into the method.
     *
     * @param option a single option bit
     * @return Returns the option name or undefined.
     */
    private fun getOptionName(option: Int): String {

        var result = optionNames[option]

        if (result == null) {

            result = defineOptionName(option)

            if (result != null)
                optionNames[option] = result
            else
                result = "<option name not defined>"
        }

        return result
    }
}
