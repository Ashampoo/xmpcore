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
 * Representates an XMP XMPPath with segment accessor methods.
 */
class XMPPath {

    private val segments = mutableListOf<XMPPathSegment>()

    fun add(segment: XMPPathSegment) {
        segments.add(segment)
    }

    fun getSegment(index: Int): XMPPathSegment = segments[index]

    fun size(): Int = segments.size

    override fun toString(): String {

        val result = StringBuilder()
        var index = 1

        while (index < size()) {

            result.append(getSegment(index))

            if (index < size() - 1) {

                val kind = getSegment(index + 1).kind

                if (kind == STRUCT_FIELD_STEP || kind == QUALIFIER_STEP)
                    result.append('/')
            }

            index++
        }

        return result.toString()
    }

    companion object {

        /**
         * Marks a struct field step , also for top level nodes (schema "fields").
         */
        const val STRUCT_FIELD_STEP = 0x01

        /**
         * Marks a qualifier step.
         * Note: Order is significant to separate struct/qual from array kinds!
         */
        const val QUALIFIER_STEP = 0x02

        /**
         * Marks an array index step
         */
        const val ARRAY_INDEX_STEP = 0x03

        const val ARRAY_LAST_STEP = 0x04

        const val QUAL_SELECTOR_STEP = 0x05

        const val FIELD_SELECTOR_STEP = 0x06

        const val SCHEMA_NODE = -0x80000000

        const val STEP_SCHEMA = 0

        const val STEP_ROOT_PROP = 1
    }
}
