// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.impl

import com.ashampoo.xmp.XMPConst
import com.ashampoo.xmp.XMPError
import com.ashampoo.xmp.XMPException
import com.ashampoo.xmp.XMPIterator
import com.ashampoo.xmp.XMPMeta
import com.ashampoo.xmp.XMPPathFactory.composeArrayItemPath
import com.ashampoo.xmp.XMPPathFactory.composeQualifierPath
import com.ashampoo.xmp.XMPPathFactory.composeStructFieldPath
import com.ashampoo.xmp.XMPUtils.convertToBoolean
import com.ashampoo.xmp.XMPUtils.convertToDouble
import com.ashampoo.xmp.XMPUtils.convertToInteger
import com.ashampoo.xmp.XMPUtils.convertToLong
import com.ashampoo.xmp.XMPUtils.decodeBase64
import com.ashampoo.xmp.impl.Utils.normalizeLangValue
import com.ashampoo.xmp.impl.XMPNodeUtils.appendLangItem
import com.ashampoo.xmp.impl.XMPNodeUtils.chooseLocalizedText
import com.ashampoo.xmp.impl.XMPNodeUtils.deleteNode
import com.ashampoo.xmp.impl.XMPNodeUtils.findNode
import com.ashampoo.xmp.impl.XMPNodeUtils.setNodeValue
import com.ashampoo.xmp.impl.XMPNodeUtils.verifySetOptions
import com.ashampoo.xmp.impl.XMPNormalizer.normalize
import com.ashampoo.xmp.impl.xpath.XMPPathParser.expandXPath
import com.ashampoo.xmp.options.IteratorOptions
import com.ashampoo.xmp.options.ParseOptions
import com.ashampoo.xmp.options.PropertyOptions
import com.ashampoo.xmp.properties.XMPProperty
import com.ashampoo.xmp.properties.XMPPropertyInfo

/**
 * Implementation for [XMPMeta].
 */
class XMPMetaImpl : XMPMeta {

    /**
     * root of the metadata tree
     */
    var root: XMPNode
        private set

    /**
     * the xpacket processing instructions content
     */
    private var packetHeader: String? = null

    /**
     * Constructor for an empty metadata object.
     */
    constructor() {
        // create root node
        this.root = XMPNode(null, null, PropertyOptions())
    }

    constructor(tree: XMPNode) {
        this.root = tree
    }

    override fun appendArrayItem(
        schemaNS: String,
        arrayName: String,
        arrayOptions: PropertyOptions,
        itemValue: String,
        itemOptions: PropertyOptions
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        if (!arrayOptions.isOnlyArrayOptions())
            throw XMPException("Only array form flags allowed for arrayOptions", XMPError.BADOPTIONS)

        // Check if array options are set correctly.
        val verifiedArrayOptions = verifySetOptions(arrayOptions, null)

        // Locate or create the array. If it already exists, make sure the array form from the options
        // parameter is compatible with the current state.
        val arrayPath = expandXPath(schemaNS, arrayName)

        // Just lookup, don't try to create.
        var arrayNode = findNode(this.root, arrayPath, false, null)

        if (arrayNode != null) {

            // The array exists, make sure the form is compatible. Zero arrayForm means take what exists.
            if (!arrayNode.options.isArray())
                throw XMPException("The named property is not an array", XMPError.BADXPATH)

        } else {

            // The array does not exist, try to create it.
            if (verifiedArrayOptions.isArray()) {

                arrayNode = findNode(this.root, arrayPath, true, verifiedArrayOptions)

                if (arrayNode == null)
                    throw XMPException("Failure creating array node", XMPError.BADXPATH)

            } else {

                // array options missing
                throw XMPException("Explicit arrayOptions required to create new array", XMPError.BADOPTIONS)
            }
        }

        doSetArrayItem(arrayNode, XMPConst.ARRAY_LAST_ITEM, itemValue, itemOptions, true)
    }

    override fun countArrayItems(schemaNS: String, arrayName: String): Int {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        val arrayPath = expandXPath(schemaNS, arrayName)
        val arrayNode = findNode(this.root, arrayPath, false, null) ?: return 0

        if (!arrayNode.options.isArray())
            throw XMPException("The named property is not an array", XMPError.BADXPATH)

        return arrayNode.getChildrenLength()
    }

    override fun deleteArrayItem(schemaNS: String, arrayName: String, itemIndex: Int) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        val itemPath = composeArrayItemPath(arrayName, itemIndex)

        deleteProperty(schemaNS, itemPath)
    }

    override fun deleteProperty(schemaNS: String, propName: String) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = false,
            leafOptions = null
        ) ?: return

        deleteNode(propNode)
    }

    override fun deleteQualifier(schemaNS: String, propName: String, qualNS: String, qualName: String) {

        // Note: qualNS and qualName are checked inside composeQualfierPath
        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        val qualPath = propName + composeQualifierPath(qualNS, qualName)

        deleteProperty(schemaNS, qualPath)
    }

    override fun deleteStructField(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String
    ) {

        // fieldNS and fieldName are checked inside composeStructFieldPath

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (structName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        val fieldPath = structName + composeStructFieldPath(fieldNS, fieldName)

        deleteProperty(schemaNS, fieldPath)
    }

    override fun doesPropertyExist(schemaNS: String, propName: String): Boolean {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = false,
            leafOptions = null
        )

        return propNode != null
    }

    override fun doesArrayItemExist(schemaNS: String, arrayName: String, itemIndex: Int): Boolean {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        val path = composeArrayItemPath(arrayName, itemIndex)

        return doesPropertyExist(schemaNS, path)
    }

    override fun doesStructFieldExist(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String
    ): Boolean {

        // fieldNS and fieldName are checked inside composeStructFieldPath()

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (structName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        val path = composeStructFieldPath(fieldNS, fieldName)

        return doesPropertyExist(schemaNS, structName + path)
    }

    override fun doesQualifierExist(
        schemaNS: String,
        propName: String,
        qualNS: String,
        qualName: String
    ): Boolean {

        // qualNS and qualName are checked inside composeQualifierPath()

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        val path = composeQualifierPath(qualNS, qualName)

        return doesPropertyExist(schemaNS, propName + path)
    }

    override fun getArrayItem(schemaNS: String, arrayName: String, itemIndex: Int): XMPProperty? {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        val itemPath = composeArrayItemPath(arrayName, itemIndex)

        return getProperty(schemaNS, itemPath)
    }

    override fun getLocalizedText(
        schemaNS: String,
        altTextName: String,
        genericLang: String?,
        specificLang: String
    ): XMPProperty? {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (altTextName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        if (specificLang.isEmpty())
            throw XMPException("Empty specific language", XMPError.BADPARAM)

        val normalizedGenericLang = if (genericLang != null) normalizeLangValue(genericLang) else null
        val normalizedSpecificLang = normalizeLangValue(specificLang)

        val arrayPath = expandXPath(schemaNS, altTextName)

        // *** This expand/find idiom is used in 3 Getters.
        val arrayNode = findNode(this.root, arrayPath, false, null) ?: return null
        val result = chooseLocalizedText(arrayNode, normalizedGenericLang, normalizedSpecificLang)
        val match = result[0] as Int
        val itemNode = result[1] as? XMPNode

        return if (match != XMPNodeUtils.CLT_NO_VALUES) {

            object : XMPProperty {
                override fun getValue(): String {
                    return itemNode!!.value!!
                }

                override fun getOptions(): PropertyOptions {
                    return itemNode!!.options
                }

                override fun getLanguage(): String {
                    return itemNode!!.getQualifier(1).value!!
                }

                override fun toString(): String {
                    return itemNode!!.value.toString()
                }
            }

        } else {
            null
        }
    }

    override fun setLocalizedText(
        schemaNS: String,
        altTextName: String,
        genericLang: String?,
        specificLang: String,
        itemValue: String,
        options: PropertyOptions
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (altTextName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        if (specificLang.isEmpty())
            throw XMPException("Empty specific language", XMPError.BADPARAM)

        val normalizedGenericLang = if (genericLang != null) normalizeLangValue(genericLang) else null
        val normalizedSpecificLang = normalizeLangValue(specificLang)

        val arrayPath = expandXPath(schemaNS, altTextName)

        // Find the array node and set the options if it was just created.
        val arrayNode = findNode(
            this.root, arrayPath, true,
            PropertyOptions(
                PropertyOptions.ARRAY or PropertyOptions.ARRAY_ORDERED
                    or PropertyOptions.ARRAY_ALTERNATE or PropertyOptions.ARRAY_ALT_TEXT
            )
        )

        if (arrayNode == null) {

            throw XMPException("Failed to find or create array node", XMPError.BADXPATH)

        } else if (!arrayNode.options.isArrayAltText()) {

            if (!arrayNode.hasChildren() && arrayNode.options.isArrayAlternate())
                arrayNode.options.setArrayAltText(true)
            else
                throw XMPException("Specified property is no alt-text array", XMPError.BADXPATH)
        }

        // Make sure the x-default item, if any, is first.
        var haveXDefault = false
        var xdItem: XMPNode? = null

        for (item in arrayNode.iterateChildren()) {

            if (!item.hasQualifier() || XMPConst.XML_LANG != item.getQualifier(1).name)
                throw XMPException("Language qualifier must be first", XMPError.BADXPATH)

            if (XMPConst.X_DEFAULT == item.getQualifier(1).value) {
                xdItem = item
                haveXDefault = true
                break
            }
        }

        // Moves x-default to the beginning of the array
        if (xdItem != null && arrayNode.getChildrenLength() > 1) {

            arrayNode.removeChild(xdItem)
            arrayNode.addChild(1, xdItem)
        }

        // Find the appropriate item.
        // chooseLocalizedText will make sure the array is a language alternative.
        val result = chooseLocalizedText(arrayNode, normalizedGenericLang, normalizedSpecificLang)
        val match = result[0] as Int
        val itemNode = result[1] as? XMPNode

        val specificXDefault = XMPConst.X_DEFAULT == normalizedSpecificLang

        when (match) {

            XMPNodeUtils.CLT_NO_VALUES -> {

                // Create the array items for the specificLang and x-default, with x-default first.
                appendLangItem(arrayNode, XMPConst.X_DEFAULT, itemValue)

                haveXDefault = true

                if (!specificXDefault)
                    appendLangItem(arrayNode, normalizedSpecificLang, itemValue)
            }

            XMPNodeUtils.CLT_SPECIFIC_MATCH -> if (!specificXDefault) {

                // Update the specific item, update x-default if it matches the old value.
                if (haveXDefault && xdItem != itemNode && xdItem != null && xdItem.value == itemNode!!.value)
                    xdItem.value = itemValue

                // ! Do this after the x-default check!
                itemNode!!.value = itemValue

            } else {

                // Update all items whose values match the old x-default value.
                check(haveXDefault && xdItem == itemNode)

                val it = arrayNode.iterateChildren()

                while (it.hasNext()) {

                    val currItem = it.next()

                    if (currItem == xdItem || currItem.value != xdItem?.value)
                        continue

                    currItem.value = itemValue
                }

                // And finally do the x-default item.
                if (xdItem != null)
                    xdItem.value = itemValue
            }

            XMPNodeUtils.CLT_SINGLE_GENERIC -> {

                // Update the generic item, update x-default if it matches the old value.
                if (haveXDefault && xdItem != itemNode && xdItem != null && xdItem.value == itemNode!!.value)
                    xdItem.value = itemValue

                // ! Do this after the x-default check!
                itemNode!!.value = itemValue
            }

            XMPNodeUtils.CLT_FIRST_ITEM, XMPNodeUtils.CLT_MULTIPLE_GENERIC -> {

                // Create the specific language, ignore x-default.
                appendLangItem(arrayNode, normalizedSpecificLang, itemValue)

                if (specificXDefault) haveXDefault = true
            }

            XMPNodeUtils.CLT_XDEFAULT -> {

                // Create the specific language, update x-default if it was the only item.
                if (xdItem != null && arrayNode.getChildrenLength() == 1)
                    xdItem.value = itemValue

                appendLangItem(arrayNode, normalizedSpecificLang, itemValue)
            }

            else -> // does not happen under normal circumstances
                throw XMPException("Unexpected result from ChooseLocalizedText", XMPError.INTERNALFAILURE)
        }

        // Add an x-default at the front if needed.
        if (!haveXDefault && arrayNode.getChildrenLength() == 1)
            appendLangItem(arrayNode, XMPConst.X_DEFAULT, itemValue)
    }

    override fun getProperty(schemaNS: String, propName: String): XMPProperty? =
        getProperty(schemaNS, propName, VALUE_STRING)

    /**
     * Returns a property, but the result value can be requested. It can be one
     * of [XMPMetaImpl.VALUE_STRING], [XMPMetaImpl.VALUE_BOOLEAN],
     * [XMPMetaImpl.VALUE_INTEGER], [XMPMetaImpl.VALUE_LONG],
     * [XMPMetaImpl.VALUE_DOUBLE], [XMPMetaImpl.VALUE_DATE],
     * [XMPMetaImpl.VALUE_TIME_IN_MILLIS], [XMPMetaImpl.VALUE_BASE64].
     */
    private fun getProperty(schemaNS: String, propName: String, valueType: Int): XMPProperty? {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = false,
            leafOptions = null
        ) ?: return null

        if (valueType != VALUE_STRING && propNode.options.isCompositeProperty())
            throw XMPException("Property must be simple when a value type is requested", XMPError.BADXPATH)

        val value = evaluateNodeValue(valueType, propNode)

        return object : XMPProperty {

            override fun getValue(): String? {
                return value?.toString()
            }

            override fun getOptions(): PropertyOptions {
                return propNode.options
            }

            override fun getLanguage(): String? {
                return null
            }

            override fun toString(): String {
                return value.toString()
            }
        }
    }

    /**
     * Returns a property, but the result value can be requested.
     */
    private fun getPropertyObject(schemaNS: String, propName: String, valueType: Int): Any? {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = false,
            leafOptions = null
        ) ?: return null

        if (valueType != VALUE_STRING && propNode.options.isCompositeProperty())
            throw XMPException("Property must be simple when a value type is requested", XMPError.BADXPATH)

        return evaluateNodeValue(valueType, propNode)
    }

    override fun getPropertyBoolean(schemaNS: String, propName: String): Boolean? =
        getPropertyObject(schemaNS, propName, VALUE_BOOLEAN) as? Boolean

    override fun setPropertyBoolean(
        schemaNS: String,
        propName: String,
        propValue: Boolean,
        options: PropertyOptions
    ) {
        setProperty(
            schemaNS,
            propName,
            if (propValue) XMPConst.TRUE_STRING else XMPConst.FALSE_STRING,
            options
        )
    }

    override fun getPropertyInteger(schemaNS: String, propName: String): Int? =
        getPropertyObject(schemaNS, propName, VALUE_INTEGER) as? Int

    override fun setPropertyInteger(
        schemaNS: String,
        propName: String,
        propValue: Int,
        options: PropertyOptions
    ) {
        setProperty(schemaNS, propName, propValue, options)
    }

    override fun getPropertyLong(schemaNS: String, propName: String): Long? =
        getPropertyObject(schemaNS, propName, VALUE_LONG) as? Long

    override fun setPropertyLong(
        schemaNS: String,
        propName: String,
        propValue: Long,
        options: PropertyOptions
    ) {
        setProperty(schemaNS, propName, propValue, options)
    }

    override fun getPropertyDouble(schemaNS: String, propName: String): Double? =
        getPropertyObject(schemaNS, propName, VALUE_DOUBLE) as? Double

    override fun setPropertyDouble(
        schemaNS: String,
        propName: String,
        propValue: Double,
        options: PropertyOptions
    ) {
        setProperty(schemaNS, propName, propValue, options)
    }

    override fun getPropertyBase64(schemaNS: String, propName: String): ByteArray? =
        getPropertyObject(schemaNS, propName, VALUE_BASE64) as? ByteArray

    override fun getPropertyString(schemaNS: String, propName: String): String? =
        getPropertyObject(schemaNS, propName, VALUE_STRING) as? String

    override fun setPropertyBase64(
        schemaNS: String,
        propName: String,
        propValue: ByteArray,
        options: PropertyOptions
    ) {
        setProperty(schemaNS, propName, propValue, options)
    }

    override fun getQualifier(
        schemaNS: String,
        propName: String,
        qualNS: String,
        qualName: String
    ): XMPProperty? {

        // qualNS and qualName are checked inside composeQualfierPath
        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        val qualPath = propName + composeQualifierPath(qualNS, qualName)

        return getProperty(schemaNS, qualPath)
    }

    override fun getStructField(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String
    ): XMPProperty? {

        // fieldNS and fieldName are checked inside composeStructFieldPath

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (structName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        val fieldPath = structName + composeStructFieldPath(fieldNS, fieldName)

        return getProperty(schemaNS, fieldPath)
    }

    override fun iterator(): XMPIterator =
        iterator(IteratorOptions())

    override fun iterator(options: IteratorOptions): com.ashampoo.xmp.XMPIterator =
        iterator(null, null, options)

    override fun iterator(
        schemaNS: String?,
        propName: String?,
        options: IteratorOptions
    ): XMPIterator =
        XMPIteratorImpl(this, schemaNS, propName, options)

    override fun setArrayItem(
        schemaNS: String,
        arrayName: String,
        itemIndex: Int,
        itemValue: String,
        options: PropertyOptions
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        // Just lookup, don't try to create.
        val arrayPath = expandXPath(schemaNS, arrayName)
        val arrayNode = findNode(this.root, arrayPath, false, null)

        if (arrayNode == null)
            throw XMPException("Specified array does not exist", XMPError.BADXPATH)

        doSetArrayItem(arrayNode, itemIndex, itemValue, options, false)
    }

    override fun insertArrayItem(
        schemaNS: String,
        arrayName: String,
        itemIndex: Int,
        itemValue: String,
        options: PropertyOptions
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        // Just lookup, don't try to create.
        val arrayPath = expandXPath(schemaNS, arrayName)
        val arrayNode = findNode(this.root, arrayPath, false, null)

        if (arrayNode == null)
            throw XMPException("Specified array does not exist", XMPError.BADXPATH)

        doSetArrayItem(arrayNode, itemIndex, itemValue, options, true)
    }

    override fun setProperty(
        schemaNS: String,
        propName: String,
        propValue: Any?,
        options: PropertyOptions
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        val verifiedOptions = verifySetOptions(options, propValue)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = true,
            leafOptions = verifySetOptions(options, propValue)
        ) ?: throw XMPException("Specified property does not exist", XMPError.BADXPATH)

        setNode(propNode, propValue, verifiedOptions, false)
    }

    override fun setQualifier(
        schemaNS: String,
        propName: String,
        qualNS: String,
        qualName: String,
        qualValue: String,
        options: PropertyOptions
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        if (!doesPropertyExist(schemaNS, propName))
            throw XMPException("Specified property does not exist!", XMPError.BADXPATH)

        val qualPath = propName + composeQualifierPath(qualNS, qualName)

        setProperty(schemaNS, qualPath, qualValue, options)
    }

    override fun setStructField(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String,
        fieldValue: String?,
        options: PropertyOptions
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPError.EMPTY_SCHEMA_TEXT, XMPError.BADPARAM)

        if (structName.isEmpty())
            throw XMPException("Empty array name", XMPError.BADPARAM)

        val fieldPath = structName + composeStructFieldPath(fieldNS, fieldName)

        setProperty(schemaNS, fieldPath, fieldValue, options)
    }

    override fun getObjectName(): String =
        root.name ?: ""

    override fun setObjectName(name: String) {
        root.name = name
    }

    override fun getPacketHeader(): String? =
        packetHeader

    /**
     * Sets the packetHeader attributes, only used by the parser.
     */
    fun setPacketHeader(packetHeader: String?) {
        this.packetHeader = packetHeader
    }

    override fun sort() {
        this.root.sort()
    }

    override fun normalize(options: ParseOptions) {
        normalize(this, options)
    }

    override fun printAllToConsole() {

        val iterator: XMPIterator = iterator()

        while (iterator.hasNext()) {

            val propertyInfo = iterator.next() as? XMPPropertyInfo ?: continue

            println("${propertyInfo.getPath()} = ${propertyInfo.getValue()}")
        }
    }

    // -------------------------------------------------------------------------------------
    // private

    /**
     * Locate or create the item node and set the value. Note the index
     * parameter is one-based! The index can be in the range [1..size + 1] or
     * "last()", normalize it and check the insert flags. The order of the
     * normalization checks is important. If the array is empty we end up with
     * an index and location to set item size + 1.
     */
    private fun doSetArrayItem(
        arrayNode: XMPNode,
        itemIndex: Int,
        itemValue: String,
        itemOptions: PropertyOptions,
        insert: Boolean
    ) {

        val itemNode = XMPNode(XMPConst.ARRAY_ITEM_NAME, null)

        val verifiedItemOptions = verifySetOptions(itemOptions, itemValue)

        // in insert mode the index after the last is allowed,
        // even ARRAY_LAST_ITEM points to the index *after* the last.
        val maxIndex = if (insert)
            arrayNode.getChildrenLength() + 1
        else
            arrayNode.getChildrenLength()

        val limitedItemIndex = if (itemIndex == XMPConst.ARRAY_LAST_ITEM)
            maxIndex
        else
            itemIndex

        if (1 <= limitedItemIndex && limitedItemIndex <= maxIndex) {

            if (!insert)
                arrayNode.removeChild(limitedItemIndex)

            arrayNode.addChild(limitedItemIndex, itemNode)
            setNode(itemNode, itemValue, verifiedItemOptions, false)

        } else {
            throw XMPException("Array index out of bounds", XMPError.BADINDEX)
        }
    }

    /**
     * The internals for setProperty() and related calls, used after the node is found or created.
     */
    private fun setNode(node: XMPNode, value: Any?, newOptions: PropertyOptions, deleteExisting: Boolean) {

        val compositeMask = PropertyOptions.ARRAY or PropertyOptions.ARRAY_ALT_TEXT or
            PropertyOptions.ARRAY_ALTERNATE or PropertyOptions.ARRAY_ORDERED or PropertyOptions.STRUCT

        if (deleteExisting)
            node.clear()

        // its checked by setOptions(), if the merged result is a valid options set
        node.options.mergeWith(newOptions)

        if (node.options.getOptions() and compositeMask == 0) {

            // This is setting the value of a leaf node.
            setNodeValue(node, value)

        } else {

            if (value != null && value.toString().isNotEmpty())
                throw XMPException("Composite nodes can't have values", XMPError.BADXPATH)

            // Can't change an array to a struct, or vice versa.
            if (node.options.getOptions() and compositeMask != 0 &&
                newOptions.getOptions() and compositeMask != node.options.getOptions() and compositeMask
            )
                throw XMPException("Requested and existing composite form mismatch", XMPError.BADXPATH)

            node.removeChildren()
        }
    }

    /**
     * Evaluates a raw node value to the given value type, apply special
     * conversions for defined types in XMP.
     */
    private fun evaluateNodeValue(valueType: Int, propNode: XMPNode): Any? {

        val value: Any?
        val rawValue = propNode.value

        value = when (valueType) {

            VALUE_BOOLEAN -> convertToBoolean(rawValue)

            VALUE_INTEGER -> convertToInteger(rawValue)

            VALUE_LONG -> convertToLong(rawValue)

            VALUE_DOUBLE -> convertToDouble(rawValue)

            VALUE_BASE64 -> decodeBase64(rawValue!!)

            // leaf values return empty string instead of null
            // for the other cases the converter methods provides a "null" value.
            // a default value can only occur if this method is made public.
            VALUE_STRING ->
                if (rawValue != null || propNode.options.isCompositeProperty()) rawValue else ""

            else ->
                if (rawValue != null || propNode.options.isCompositeProperty()) rawValue else ""
        }

        return value
    }

    companion object {

        /**
         * Property values are Strings by default
         */

        private const val VALUE_STRING = 0
        private const val VALUE_BOOLEAN = 1
        private const val VALUE_INTEGER = 2
        private const val VALUE_LONG = 3
        private const val VALUE_DOUBLE = 4
        private const val VALUE_BASE64 = 7
    }
}
