// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.options

import com.ashampoo.xmp.XMPErrorConst
import com.ashampoo.xmp.XMPException

/**
 * The base class for a collection of 32 flag bits. Individual flags are defined as enum value bit
 * masks. Inheriting classes add convenience accessor methods.
 */
public abstract class Options {

    /**
     * the internal int containing all options
     */
    private var valueBits = 0

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
     * @param optionBit the binary bit or bits that are requested
     * @return Returns if *all* of the requested bits are set or not.
     */
    protected fun getOption(optionBit: Int): Boolean =
        valueBits and optionBit != 0

    /**
     * @param optionBits the binary bit or bits that shall be set to the given value
     * @param value      the boolean value to set
     */
    public fun setOption(optionBits: Int, value: Boolean) {
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
    public fun getOptions(): Int = valueBits

    public fun setOptions(options: Int) {

        assertOptionsValid(options)

        this.valueBits = options
    }

    override fun equals(other: Any?): Boolean =
        getOptions() == (other as? Options)?.getOptions()

    override fun hashCode(): Int = getOptions()

    /**
     * @return Returns the options as hex bitmask.
     */
    override fun toString(): String =
        "0x" + valueBits.toString(16)

    /**
     * To be implemeted by inheritants.
     *
     * @param option a single, valid option bit.
     * @return Returns a human-readable name for an option bit.
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
    protected open fun assertConsistency(options: Int): Unit = Unit // empty, no checks

    /**
     * Checks options before they are set.
     * First it is checked if only defined options are used,
     * second the additional [Options.assertConsistency]-method is called.
     */
    private fun assertOptionsValid(options: Int) {

        val invalidOptions = options and getValidOptions().inv()

        if (invalidOptions != 0)
            throw XMPException(
                "The option bit(s) 0x" + invalidOptions.toString(16) + " + are invalid!",
                XMPErrorConst.BADOPTIONS
            )

        assertConsistency(options)
    }
}
