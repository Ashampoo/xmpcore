// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.XMPConst.NS_MWG_RS
import com.ashampoo.xmp.XMPConst.XMP_MWG_RS_APPLIED_TO_DIMENSIONS
import com.ashampoo.xmp.XMPConst.XMP_MWG_RS_REGION_LIST
import com.ashampoo.xmp.XMPPathFactory.composeArrayItemPath
import com.ashampoo.xmp.XMPPathFactory.composeQualifierPath
import com.ashampoo.xmp.XMPPathFactory.composeStructFieldPath
import com.ashampoo.xmp.internal.Utils.normalizeLangValue
import com.ashampoo.xmp.internal.XMPErrorConst
import com.ashampoo.xmp.internal.XMPNode
import com.ashampoo.xmp.internal.XMPNodeUtils
import com.ashampoo.xmp.internal.XMPNodeUtils.appendLangItem
import com.ashampoo.xmp.internal.XMPNodeUtils.chooseLocalizedText
import com.ashampoo.xmp.internal.XMPNodeUtils.deleteNode
import com.ashampoo.xmp.internal.XMPNodeUtils.findNode
import com.ashampoo.xmp.internal.XMPNodeUtils.setNodeValue
import com.ashampoo.xmp.internal.XMPNodeUtils.verifySetOptions
import com.ashampoo.xmp.internal.XMPNormalizer.normalize
import com.ashampoo.xmp.internal.XMPPathParser.expandXPath
import com.ashampoo.xmp.internal.XMPUtils.convertToBoolean
import com.ashampoo.xmp.internal.XMPUtils.convertToDouble
import com.ashampoo.xmp.internal.XMPUtils.convertToInteger
import com.ashampoo.xmp.internal.XMPUtils.convertToLong
import com.ashampoo.xmp.internal.XMPUtils.decodeBase64
import com.ashampoo.xmp.options.IteratorOptions
import com.ashampoo.xmp.options.ParseOptions
import com.ashampoo.xmp.options.PropertyOptions

/**
 * This class represents the set of XMP metadata as a DOM representation. It has methods to read and
 * modify all kinds of properties, create an iterator over all properties and serialize the metadata
 * to a String.
 */
@Suppress("TooManyFunctions")
public class XMPMeta internal constructor() {

    /**
     * root of the metadata tree
     */
    internal var root: XMPNode = XMPNode(null, null, PropertyOptions())
        private set

    /**
     * the xpacket processing instructions content
     */
    private var packetHeader: String? = null

    private val arrayOptions = PropertyOptions().setArray(true)

    // ---------------------------------------------------------------------------------------------
    // Basic property manipulation functions

    /**
     * The property value getter-methods all take a property specification: the first two parameters
     * are always the top level namespace URI (the &quot;schema&quot; namespace) and the basic name
     * of the property being referenced. See the introductory discussion of path expression usage
     * for more information.
     *
     * All the functions return an object inherited from `PropertyBase` or
     * `null` if the property does not exist. The result object contains the value of
     * the property and option flags describing the property. Arrays and the non-leaf levels of
     * nodes do not have values.
     *
     * See [PropertyOptions] for detailed information about the options.
     *
     * This is the simplest property getter, mainly for top level simple properties or after using
     * the path composition functions in XMPPathFactory.
     *
     * @param schemaNS The namespace URI for the property.
     *                 The URI must be for a registered namespace.
     * @param propName The name of the property. May be a general path expression,
     *                 must not be `null` or the empty string.
     *                 Using a namespace prefix on the first component is optional.
     *                 If present without a schemaNS value then the prefix specifies the namespace.
     *                 The prefix must be for a registered namespace.
     *                 If both a schemaNS URI and propName prefix are present,
     *                 they must be corresponding parts of a registered namespace.
     * @return Returns a `XMPProperty` containing the value and the options or `null`
     *         if the property does not exist.
     */
    public fun getProperty(schemaNS: String, propName: String): XMPProperty? =
        getProperty(schemaNS, propName, XMPValueType.STRING)

    private fun getProperty(schemaNS: String, propName: String, valueType: XMPValueType): XMPProperty? {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (propName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_PROPERTY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = false,
            leafOptions = null
        ) ?: return null

        if (valueType != XMPValueType.STRING && propNode.options.isCompositeProperty())
            throw XMPException(
                "Property must be simple when a value type is requested",
                XMPErrorConst.BADXPATH
            )

        val value = evaluateNodeValue(valueType, propNode)

        return object : XMPProperty {

            override fun getValue(): String? =
                value?.toString()

            override fun getOptions(): PropertyOptions =
                propNode.options

            override fun getLanguage(): String? =
                null

            override fun toString(): String =
                value.toString()
        }
    }

    /**
     * Evaluates a raw node value to the given value type, apply special
     * conversions for defined types in XMP.
     */
    private fun evaluateNodeValue(valueType: XMPValueType, propNode: XMPNode): Any? =
        when (valueType) {

            XMPValueType.BOOLEAN -> convertToBoolean(propNode.value)

            XMPValueType.INTEGER -> convertToInteger(propNode.value)

            XMPValueType.LONG -> convertToLong(propNode.value)

            XMPValueType.DOUBLE -> convertToDouble(propNode.value)

            XMPValueType.BASE64 -> decodeBase64(propNode.value!!)

            /*
             * Leaf values return empty string instead of null
             * for the other cases the converter methods provides a "null" value.
             * a default value can only occur if this method is made public.
             */
            XMPValueType.STRING ->
                if (propNode.value != null || propNode.options.isCompositeProperty())
                    propNode.value
                else
                    ""
        }

    /**
     * Provides access to items within an array. The index is passed as an integer, you need not
     * worry about the path string syntax for array items, convert a loop index to a string, etc.
     *
     * @param schemaNS  The namespace URI for the array. Has the same usage as in `getProperty()`.
     * @param arrayName The name of the array. May be a general path expression,
     *                  must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in `getProperty()`.
     * @param itemIndex The index of the desired item. Arrays in XMP are indexed from 1.
     *                  The constant [XMPConst.ARRAY_LAST_ITEM] always refers to the last
     *                  existing array item.
     * @return Returns a `XMPProperty` containing the value and the options or
     *         `null` if the property does not exist.
     */
    public fun getArrayItem(schemaNS: String, arrayName: String, itemIndex: Int): XMPProperty? {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val itemPath = composeArrayItemPath(arrayName, itemIndex)

        return getProperty(schemaNS, itemPath)
    }

    /**
     * Returns the number of items in the array.
     *
     * @param schemaNS  The namespace URI for the array. Has the same usage as in getProperty.
     * @param arrayName The name of the array. May be a general path expression,
     *                  must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in `getProperty()`.
     * @return Returns the number of items in the array.
     */
    public fun countArrayItems(schemaNS: String, arrayName: String): Int {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val arrayPath = expandXPath(schemaNS, arrayName)
        val arrayNode = findNode(this.root, arrayPath, false, null) ?: return 0

        if (!arrayNode.options.isArray())
            throw XMPException("The named property is not an array", XMPErrorConst.BADXPATH)

        return arrayNode.getChildrenLength()
    }

    /**
     * Provides access to fields within a nested structure. The namespace for the field is passed as
     * a URI, you need not worry about the path string syntax.
     *
     * The names of fields should be XML qualified names, that is within an XML namespace. The path
     * syntax for a qualified name uses the namespace prefix. This is unreliable since the prefix is
     * never guaranteed. The URI is the formal name, the prefix is just a local shorthand in a given
     * sequence of XML text.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in getProperty.
     * @param structName The name of the struct.
     *                   May be a general path expression, must not be `null` or the empty string.
     *                   Has the same namespace prefix usage as propName in `getProperty()`.
     * @param fieldNS    The namespace URI for the field. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param fieldName  The name of the field. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * structName parameter.
     * @return Returns a `XMPProperty` containing the value and the options or
     * `null` if the property does not exist. Arrays and non-leaf levels of
     * structs do not have values.
     */
    public fun getStructField(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String
    ): XMPProperty? {

        /* Note that fieldNS and fieldName are checked inside composeStructFieldPath */

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (structName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val fieldPath = structName + composeStructFieldPath(fieldNS, fieldName)

        return getProperty(schemaNS, fieldPath)
    }

    /**
     * Provides access to a qualifier attached to a property. The namespace for the qualifier is
     * passed as a URI, you need not worry about the path string syntax. In many regards qualifiers
     * are like struct fields. See the introductory discussion of qualified properties for more
     * information.
     *
     * The names of qualifiers should be XML qualified names, that is within an XML namespace. The
     * path syntax for a qualified name uses the namespace prefix. This is unreliable since the
     * prefix is never guaranteed. The URI is the formal name, the prefix is just a local shorthand
     * in a given sequence of XML text.
     *
     * *Note:* Qualifiers are only supported for simple leaf properties.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in getProperty.
     * @param propName   The name of the struct.
     *                   May be a general path expression, must not be `null` or the empty string.
     *                   Has the same namespace prefix usage as propName in `getProperty()`.
     * @param qualNS   The namespace URI for the qualifier. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param qualName The name of the qualifier. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * propName parameter.
     * @return Returns a `XMPProperty` containing the value and the options of the
     * qualifier or `null` if the property does not exist. The name of the
     * qualifier must be a single XML name, must not be `null` or the empty
     * string. Has the same namespace prefix usage as the propName parameter.
     *
     * The value of the qualifier is only set if it has one (Arrays and non-leaf levels of
     * structs do not have values).
     */
    public fun getQualifier(
        schemaNS: String,
        propName: String,
        qualNS: String,
        qualName: String
    ): XMPProperty? {

        // qualNS and qualName are checked inside composeQualfierPath
        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (propName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_PROPERTY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val qualPath = propName + composeQualifierPath(qualNS, qualName)

        return getProperty(schemaNS, qualPath)
    }

// ---------------------------------------------------------------------------------------------
// Functions for setting property values

    /**
     * The property value `setters` all take a property specification, their
     * differences are in the form of this. The first two parameters are always the top level
     * namespace URI (the `schema` namespace) and the basic name of the property being
     * referenced. See the introductory discussion of path expression usage for more information.
     *
     * All of the functions take a string value for the property and option flags describing the
     * property. The value must be Unicode in UTF-8 encoding. Arrays and non-leaf levels of structs
     * do not have values. Empty arrays and structs may be created using appropriate option flags.
     * All levels of structs that is assigned implicitly are created if necessary. appendArayItem
     * implicitly creates the named array if necessary.
     *
     * See [PropertyOptions] for detailed information about the options.
     *
     * This is the simplest property setter, mainly for top level simple properties or after using
     * the path composition functions in [XMPPathFactory].
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in getProperty.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the value for the property (only leaf properties have a value).
     * Arrays and non-leaf levels of structs do not have values.
     * Must be `null` if the value is not relevant.
     * The value is automatically detected: Boolean, Integer, Long, Double, XMPDateTime and
     * byte[] are handled, on all other `toString()` is called.
     * @param options   Option flags describing the property. See the earlier description.
     */
    @kotlin.jvm.JvmOverloads
    public fun setProperty(
        schemaNS: String,
        propName: String,
        propValue: Any?,
        options: PropertyOptions = PropertyOptions()
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (propName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_PROPERTY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val verifiedOptions = verifySetOptions(options, propValue)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = true,
            leafOptions = verifySetOptions(options, propValue)
        ) ?: throw XMPException("Specified property does not exist", XMPErrorConst.BADXPATH)

        setNode(propNode, propValue, verifiedOptions, false)
    }

    /**
     * The internals for setProperty() and related calls, used after the node is found or created.
     */
    private fun setNode(
        node: XMPNode,
        value: Any?,
        newOptions: PropertyOptions,
        deleteExisting: Boolean
    ) {

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
                throw XMPException("Composite nodes can't have values", XMPErrorConst.BADXPATH)

            // Can't change an array to a struct, or vice versa.
            if (node.options.getOptions() and compositeMask != 0 &&
                newOptions.getOptions() and compositeMask != node.options.getOptions() and compositeMask
            )
                throw XMPException("Requested and existing composite form mismatch", XMPErrorConst.BADXPATH)

            node.removeChildren()
        }
    }

    /**
     * Replaces an item within an array. The index is passed as an integer, you need not worry about
     * the path string syntax for array items, convert a loop index to a string, etc. The array
     * passed must already exist. In normal usage the selected array item is modified. A new item is
     * automatically appended if the index is the array size plus 1.
     *
     * @param schemaNS  The namespace URI for the struct. Has the same usage as in getProperty.
     * @param arrayName The name of the array.
     *                  May be a general path expression, must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in getProperty.
     * @param itemIndex The index of the desired item. Arrays in XMP are indexed from 1. To address
     * the last existing item, use [XMPMeta.countArrayItems] to find
     * out the length of the array.
     * @param itemValue the new value of the array item. Has the same usage as propValue in
     * `setProperty()`.
     * @param options   the set options for the item.
     */
    @kotlin.jvm.JvmOverloads
    public fun setArrayItem(
        schemaNS: String,
        arrayName: String,
        itemIndex: Int,
        itemValue: String,
        options: PropertyOptions = PropertyOptions()
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        // Just lookup, don't try to create.
        val arrayPath = expandXPath(schemaNS, arrayName)
        val arrayNode = findNode(this.root, arrayPath, false, null)

        if (arrayNode == null)
            throw XMPException("Specified array does not exist", XMPErrorConst.BADXPATH)

        doSetArrayItem(arrayNode, itemIndex, itemValue, options, false)
    }

    /**
     * Inserts an item into an array previous to the given index. The index is passed as an integer,
     * you need not worry about the path string syntax for array items, convert a loop index to a
     * string, etc. The array passed must already exist. In normal usage the selected array item is
     * modified. A new item is automatically appended if the index is the array size plus 1.
     *
     * @param schemaNS  The namespace URI for the struct. Has the same usage as in getProperty.
     * @param arrayName The name of the array.
     *                  May be a general path expression, must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in getProperty.
     * @param itemIndex The index to insert the new item. Arrays in XMP are indexed from 1. Use
     * `XMPConst.ARRAY_LAST_ITEM` to append items.
     * @param itemValue the new value of the array item. Has the same usage as
     * propValue in `setProperty()`.
     * @param options   the set options that decide about the kind of the node.
     */
    @kotlin.jvm.JvmOverloads
    public fun insertArrayItem(
        schemaNS: String,
        arrayName: String,
        itemIndex: Int,
        itemValue: String,
        options: PropertyOptions = PropertyOptions()
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        // Just lookup, don't try to create.
        val arrayPath = expandXPath(schemaNS, arrayName)
        val arrayNode = findNode(this.root, arrayPath, false, null)

        if (arrayNode == null)
            throw XMPException("Specified array does not exist", XMPErrorConst.BADXPATH)

        doSetArrayItem(arrayNode, itemIndex, itemValue, options, true)
    }

    /**
     * Simplifies the construction of an array by not requiring that you pre-create an empty array.
     * The array that is assigned is created automatically if it does not yet exist. Each call to
     * appendArrayItem() appends an item to the array. The corresponding parameters have the same
     * use as setArrayItem(). The arrayOptions parameter is used to specify what kind of array. If
     * the array exists, it must have the specified form.
     *
     * @param schemaNS  The namespace URI for the struct. Has the same usage as in getProperty.
     * @param arrayName The name of the array.
     *                  May be a general path expression, must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in getProperty.
     * @param arrayOptions Option flags describing the array form. The only valid options are
     *
     *  *  [PropertyOptions.ARRAY],
     *  *  [PropertyOptions.ARRAY_ORDERED],
     *  *  [PropertyOptions.ARRAY_ALTERNATE] or
     *  *  [PropertyOptions.ARRAY_ALT_TEXT].
     *
     * *Note:* the array options only need to be provided if the array is not
     * already existing, otherwise you can set them to `null` or use [XMPMeta.appendArrayItem].
     *
     * @param itemValue    the value of the array item. Has the same usage as propValue in getProperty.
     * @param itemOptions  Option flags describing the item to append ([PropertyOptions])
     */
    @kotlin.jvm.JvmOverloads
    public fun appendArrayItem(
        schemaNS: String,
        arrayName: String,
        arrayOptions: PropertyOptions = PropertyOptions(),
        itemValue: String,
        itemOptions: PropertyOptions = PropertyOptions()
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        if (!arrayOptions.isOnlyArrayOptions())
            throw XMPException("Only array form flags allowed for arrayOptions", XMPErrorConst.BADOPTIONS)

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
                throw XMPException("The named property is not an array", XMPErrorConst.BADXPATH)

        } else {

            // The array does not exist, try to create it.
            if (verifiedArrayOptions.isArray()) {

                arrayNode = findNode(this.root, arrayPath, true, verifiedArrayOptions)

                if (arrayNode == null)
                    throw XMPException("Failure creating array node", XMPErrorConst.BADXPATH)

            } else {

                // array options missing
                throw XMPException(
                    "Explicit arrayOptions required to create new array",
                    XMPErrorConst.BADOPTIONS
                )
            }
        }

        doSetArrayItem(arrayNode, XMPConst.ARRAY_LAST_ITEM, itemValue, itemOptions, true)
    }

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
        itemOptions: PropertyOptions = PropertyOptions(),
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
            throw XMPException("Array index out of bounds", XMPErrorConst.BADINDEX)
        }
    }

    /**
     * Provides access to fields within a nested structure. The namespace for the field is passed as
     * a URI, you need not worry about the path string syntax. The names of fields should be XML
     * qualified names, that is within an XML namespace. The path syntax for a qualified name uses
     * the namespace prefix, which is unreliable because the prefix is never guaranteed. The URI is
     * the formal name, the prefix is just a local shorthand in a given sequence of XML text.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in getProperty.
     * @param structName The name of the struct. May be a general path expression, must not be null
     * or the empty string. Has the same namespace prefix usage as propName in getProperty.
     * @param fieldNS    The namespace URI for the field. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param fieldName  The name of the field. Must be a single XML name, must not be null or the
     * empty string. Has the same namespace prefix usage as the structName parameter.
     * @param fieldValue the value of thefield, if the field has a value.
     * Has the same usage as propValue in getProperty.
     * @param options    Option flags describing the field. See the earlier description.
     */
    @kotlin.jvm.JvmOverloads
    public fun setStructField(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String,
        fieldValue: String?,
        options: PropertyOptions = PropertyOptions()
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (structName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val fieldPath = structName + composeStructFieldPath(fieldNS, fieldName)

        setProperty(schemaNS, fieldPath, fieldValue, options)
    }

    /**
     * Provides access to a qualifier attached to a property. The namespace for the qualifier is
     * passed as a URI, you need not worry about the path string syntax. In many regards qualifiers
     * are like struct fields. See the introductory discussion of qualified properties for more
     * information. The names of qualifiers should be XML qualified names, that is within an XML
     * namespace. The path syntax for a qualified name uses the namespace prefix, which is
     * unreliable because the prefix is never guaranteed. The URI is the formal name, the prefix is
     * just a local shorthand in a given sequence of XML text. The property the qualifier
     * will be attached has to exist.
     *
     * @param schemaNS  The namespace URI for the struct. Has the same usage as in getProperty.
     * @param propName  The name of the property to which the qualifier is attached. Has the same
     * usage as in getProperty.
     * @param qualNS    The namespace URI for the qualifier. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param qualName  The name of the qualifier. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * propName parameter.
     * @param qualValue A pointer to the `null` terminated UTF-8 string that is the
     * value of the qualifier, if the qualifier has a value. Has the same usage as propValue
     * in getProperty.
     * @param options   Option flags describing the qualifier. See the earlier description.
     */
    @kotlin.jvm.JvmOverloads
    public fun setQualifier(
        schemaNS: String,
        propName: String,
        qualNS: String,
        qualName: String,
        qualValue: String,
        options: PropertyOptions = PropertyOptions()
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (propName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_PROPERTY_NAME_TEXT, XMPErrorConst.BADPARAM)

        if (!doesPropertyExist(schemaNS, propName))
            throw XMPException("Specified property does not exist!", XMPErrorConst.BADXPATH)

        val qualPath = propName + composeQualifierPath(qualNS, qualName)

        setProperty(schemaNS, qualPath, qualValue, options)
    }

// ---------------------------------------------------------------------------------------------
// Functions for deleting and detecting properties.
// These should be obvious from the descriptions of the getters and setters.

    /**
     * Deletes the given XMP subtree rooted at the given property.
     * It is not an error if the property does not exist.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in getProperty.
     */
    public fun deleteProperty(schemaNS: String, propName: String) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (propName.isEmpty())
            throw XMPException("Can't delete empty property name.", XMPErrorConst.BADPARAM)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = false,
            leafOptions = null
        ) ?: return

        deleteNode(propNode)
    }

    /**
     * Deletes the given XMP subtree rooted at the given array item.
     * It is not an error if the array item does not exist.
     *
     * @param schemaNS  The namespace URI for the array. Has the same usage as in getProperty.
     * @param arrayName The name of the array. May be a general path expression, must not be
     * `null` or the empty string. Has the same namespace prefix usage as
     * propName in `getProperty()`.
     * @param itemIndex The index of the desired item. Arrays in XMP are indexed from 1. The
     * constant `XMPConst.ARRAY_LAST_ITEM` always refers to the last
     * existing array item.
     */
    public fun deleteArrayItem(schemaNS: String, arrayName: String, itemIndex: Int) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val itemPath = composeArrayItemPath(arrayName, itemIndex)

        deleteProperty(schemaNS, itemPath)
    }

    /**
     * Deletes the given XMP subtree rooted at the given struct field.
     * It is not an error if the field does not exist.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in `getProperty()`.
     * @param structName The name of the struct. May be a general path expression, must not be
     * `null` or the empty string. Has the same namespace prefix usage as
     * propName in getProperty.
     * @param fieldNS    The namespace URI for the field. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param fieldName  The name of the field. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * structName parameter.
     */
    public fun deleteStructField(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String
    ) {

        // fieldNS and fieldName are checked inside composeStructFieldPath

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (structName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val fieldPath = structName + composeStructFieldPath(fieldNS, fieldName)

        deleteProperty(schemaNS, fieldPath)
    }

    /**
     * Deletes the given XMP subtree rooted at the given qualifier.
     * It is not an error if the qualifier does not exist.
     *
     * @param schemaNS The namespace URI for the struct. Has the same usage as in `getProperty()`.
     * @param propName The name of the property to which the qualifier is attached. Has the same
     * usage as in getProperty.
     * @param qualNS   The namespace URI for the qualifier. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param qualName The name of the qualifier. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * propName parameter.
     */
    public fun deleteQualifier(schemaNS: String, propName: String, qualNS: String, qualName: String) {

        // Note: qualNS and qualName are checked inside composeQualfierPath
        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (propName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_PROPERTY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val qualPath = propName + composeQualifierPath(qualNS, qualName)

        deleteProperty(schemaNS, qualPath)
    }

    /**
     * Returns whether the property exists.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns true if the property exists.
     */
    public fun doesPropertyExist(schemaNS: String, propName: String): Boolean {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (propName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_PROPERTY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = false,
            leafOptions = null
        )

        return propNode != null
    }

    /**
     * Tells if the array item exists.
     *
     * @param schemaNS  The namespace URI for the array. Has the same usage as in `getProperty()`.
     * @param arrayName The name of the array. May be a general path expression, must not be
     * `null` or the empty string. Has the same namespace prefix usage as
     * propName in `getProperty()`.
     * @param itemIndex The index of the desired item. Arrays in XMP are indexed from 1. The
     * constant `XMPConst.ARRAY_LAST_ITEM` always refers to the last
     * existing array item.
     * @return Returns `true` if the array exists, `false` otherwise.
     */
    public fun doesArrayItemExist(schemaNS: String, arrayName: String, itemIndex: Int): Boolean {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (arrayName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val path = composeArrayItemPath(arrayName, itemIndex)

        return doesPropertyExist(schemaNS, path)
    }

    /**
     * Tells if the struct field exists.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in `getProperty()`.
     * @param structName The name of the struct. May be a general path expression,
     *                   must not be `null` or the empty string.
     *                   Has the same namespace prefix usage as propName in `getProperty()`.
     * @param fieldNS    The namespace URI for the field.
     *                   Has the same URI and prefix usage as the schemaNS parameter.
     * @param fieldName  The name of the field. Must be a single XML name,
     *                   must not be `null` or the empty string.
     *                   Has the same namespace prefix usage as the structName parameter.
     * @return Returns true if the field exists.
     */
    public fun doesStructFieldExist(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String
    ): Boolean {

        // fieldNS and fieldName are checked inside composeStructFieldPath()

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (structName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val path = composeStructFieldPath(fieldNS, fieldName)

        return doesPropertyExist(schemaNS, structName + path)
    }

    /**
     * Tells if the qualifier exists.
     *
     * @param schemaNS The namespace URI for the struct. Has the same usage as in `getProperty()`.
     * @param propName The name of the property to which the qualifier is attached. Has the same
     * usage as in `getProperty()`.
     * @param qualNS   The namespace URI for the qualifier. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param qualName The name of the qualifier. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * propName parameter.
     * @return Returns true if the qualifier exists.
     */
    public fun doesQualifierExist(
        schemaNS: String,
        propName: String,
        qualNS: String,
        qualName: String
    ): Boolean {

        // qualNS and qualName are checked inside composeQualifierPath()

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (propName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_PROPERTY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val path = composeQualifierPath(qualNS, qualName)

        return doesPropertyExist(schemaNS, propName + path)
    }

// ---------------------------------------------------------------------------------------------
// Specialized Get and Set functions

    /**
     * These functions provide convenient support for localized text properties, including a number
     * of special and obscure aspects. Localized text properties are stored in alt-text arrays. They
     * allow multiple concurrent localizations of a property value, for example a document title or
     * copyright in several languages. The most important aspect of these functions is that they
     * select an appropriate array item based on one or two RFC 3066 language tags. One of these
     * languages, the "specific" language, is preferred and selected if there is an exact match. For
     * many languages it is also possible to define a "generic" language that may be used if there
     * is no specific language match. The generic language must be a valid RFC 3066 primary subtag,
     * or the empty string. For example, a specific language of "en-US" should be used in the US,
     * and a specific language of "en-UK" should be used in England. It is also appropriate to use
     * "en" as the generic language in each case. If a US document goes to England, the "en-US"
     * title is selected by using the "en" generic language and the "en-UK" specific language. It is
     * considered poor practice, but allowed, to pass a specific language that is just an RFC 3066
     * primary tag. For example "en" is not a good specific language, it should only be used as a
     * generic language. Passing "i" or "x" as the generic language is also considered poor practice
     * but allowed. Advice from the W3C about the use of RFC 3066 language tags can be found at:
     * http://www.w3.org/International/articles/language-tags/
     *
     * *Note:* RFC 3066 language tags must be treated in a case insensitive manner. The XMP
     * Toolkit does this by normalizing their capitalization:
     *
     *  *  The primary subtag is lower case, the suggested practice of ISO 639.
     *  *  All 2 letter secondary subtags are upper case, the suggested practice of ISO 3166.
     *  *  All other subtags are lower case. The XMP specification defines an artificial language,
     *  * "x-default", that is used to explicitly denote a default item in an alt-text array.
     *
     * The XMP toolkit normalizes alt-text arrays such that the x-default item is the first item.
     * The SetLocalizedText function has several special features related to the x-default item, see
     * its description for details. The selection of the array item is the same for GetLocalizedText
     * and SetLocalizedText:
     *
     *  *  Look for an exact match with the specific language.
     *  *  If a generic language is given, look for a partial match.
     *  *  Look for an x-default item.
     *  *  Choose the first item.
     *
     * A partial match with the generic language is where the start of the item's language matches
     * the generic string and the next character is '-'. An exact match is also recognized as a
     * degenerate case. It is fine to pass x-default as the specific language. In this case,
     * selection of an x-default item is an exact match by the first rule, not a selection by the
     * 3rd rule. The last 2 rules are fallbacks used when the specific and generic languages fail to
     * produce a match. `getLocalizedText` returns information about a selected item in
     * an alt-text array. The array item is selected according to the rules given above.
     *
     * @param schemaNS     The namespace URI for the alt-text array. Has the same usage as in `getProperty()`.
     * @param altTextName  The name of the alt-text array. May be a general path expression, must not
     * be `null` or the empty string. Has the same namespace prefix usage as
     * propName in `getProperty()`.
     * @param genericLang  The name of the generic language as an RFC 3066 primary subtag. May be
     * `null` or the empty string if no generic language is wanted.
     * @param specificLang The name of the specific language as an RFC 3066 tag. Must not be
     * `null` or the empty string.
     * @return Returns an `XMPProperty` containing the value, the actual language and
     * the options if an appropriate alternate collection item exists, `null`
     * if the property does not exist.
     */
    public fun getLocalizedText(
        schemaNS: String,
        altTextName: String,
        genericLang: String?,
        specificLang: String
    ): XMPProperty? {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (altTextName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        if (specificLang.isEmpty())
            throw XMPException("Empty specific language", XMPErrorConst.BADPARAM)

        val normalizedGenericLang = genericLang?.let { normalizeLangValue(it) }
        val normalizedSpecificLang = normalizeLangValue(specificLang)

        val arrayPath = expandXPath(schemaNS, altTextName)

        // *** This expand/find idiom is used in 3 Getters.
        val arrayNode = findNode(this.root, arrayPath, false, null) ?: return null
        val result = chooseLocalizedText(arrayNode, normalizedGenericLang, normalizedSpecificLang)
        val match = result[0] as Int
        val itemNode = result[1] as? XMPNode

        return if (match != XMPNodeUtils.CLT_NO_VALUES) {

            object : XMPProperty {

                override fun getValue(): String =
                    itemNode!!.value!!

                override fun getOptions(): PropertyOptions =
                    itemNode!!.options

                override fun getLanguage(): String =
                    itemNode!!.getQualifier(1).value!!

                override fun toString(): String =
                    itemNode!!.value.toString()
            }

        } else {
            null
        }
    }

    /**
     * Modifies the value of a selected item in an alt-text array. Creates an appropriate array item
     * if necessary, and handles special cases for the x-default item. If the selected item is from
     * a match with the specific language, the value of that item is modified. If the existing value
     * of that item matches the existing value of the x-default item, the x-default item is also
     * modified. If the array only has 1 existing item (which is not x-default), an x-default item
     * is added with the given value. If the selected item is from a match with the generic language
     * and there are no other generic matches, the value of that item is modified. If the existing
     * value of that item matches the existing value of the x-default item, the x-default item is
     * also modified. If the array only has 1 existing item (which is not x-default), an x-default
     * item is added with the given value. If the selected item is from a partial match with the
     * generic language and there are other partial matches, a new item is created for the specific
     * language. The x-default item is not modified. If the selected item is from the last 2 rules
     * then a new item is created for the specific language. If the array only had an x-default
     * item, the x-default item is also modified. If the array was empty, items are created for the
     * specific language and x-default.
     *
     * @param schemaNS     The namespace URI for the alt-text array. Has the same usage as in `getProperty()`.
     * @param altTextName  The name of the alt-text array. May be a general path expression,
     *                     must not be `null` or the empty string.
     *                     Has the same namespace prefix usage as propName in `getProperty()`.
     * @param genericLang  The name of the generic language as an RFC 3066 primary subtag.
     *                     May be `null` or the empty string if no generic language is wanted.
     * @param specificLang The name of the specific language as an RFC 3066 tag.
     *                     Must not be `null` or the empty string.
     * @param itemValue    A pointer to the `null` terminated UTF-8 string that is the new
     *                     value for the appropriate array item
     */
    public fun setLocalizedText(
        schemaNS: String,
        altTextName: String,
        genericLang: String?,
        specificLang: String,
        itemValue: String
    ) {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (altTextName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_ARRAY_NAME_TEXT, XMPErrorConst.BADPARAM)

        if (specificLang.isEmpty())
            throw XMPException("Empty specific language", XMPErrorConst.BADPARAM)

        val normalizedGenericLang = genericLang?.let { normalizeLangValue(it) }
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

            throw XMPException("Failed to find or create array node", XMPErrorConst.BADXPATH)

        } else if (!arrayNode.options.isArrayAltText()) {

            if (!arrayNode.hasChildren() && arrayNode.options.isArrayAlternate())
                arrayNode.options.setArrayAltText(true)
            else
                throw XMPException("Specified property is no alt-text array", XMPErrorConst.BADXPATH)
        }

        // Make sure the x-default item, if any, is first.
        var haveXDefault = false
        var xdItem: XMPNode? = null

        for (item in arrayNode.iterateChildren()) {

            if (!item.hasQualifier() || XMPConst.XML_LANG != item.getQualifier(1).name)
                throw XMPException("Language qualifier must be first", XMPErrorConst.BADXPATH)

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
                throw XMPException(
                    "Unexpected result from ChooseLocalizedText",
                    XMPErrorConst.INTERNALFAILURE
                )
        }

        // Add an x-default at the front if needed.
        if (!haveXDefault && arrayNode.getChildrenLength() == 1)
            appendLangItem(arrayNode, XMPConst.X_DEFAULT, itemValue)
    }

// ---------------------------------------------------------------------------------------------
// Functions accessing properties as binary values.

    /**
     * These are very similar to `getProperty()` and `SetProperty()` above,
     * but the value is returned or provided in a literal form instead of as a UTF-8 string.
     * The path composition functions in `XMPPathFactory` may be used to compose an path
     * expression for fields in nested structures, items in arrays, or qualifiers.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `Boolean` value or `null` if the property does not exist.
     */
    public fun getPropertyBoolean(schemaNS: String, propName: String): Boolean? =
        getPropertyObject(schemaNS, propName, XMPValueType.BOOLEAN) as? Boolean

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns an `Integer` value or `null` if the property does not exist.
     */
    public fun getPropertyInteger(schemaNS: String, propName: String): Int? =
        getPropertyObject(schemaNS, propName, XMPValueType.INTEGER) as? Int

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `Long` value or `null` if the property does not exist.
     */
    public fun getPropertyLong(schemaNS: String, propName: String): Long? =
        getPropertyObject(schemaNS, propName, XMPValueType.LONG) as? Long

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `Double` value or `null` if the property does not exist.
     */
    public fun getPropertyDouble(schemaNS: String, propName: String): Double? =
        getPropertyObject(schemaNS, propName, XMPValueType.DOUBLE) as? Double

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `byte[]`-array contained the decoded base64 value or `null` if the property does
     * not exist.
     */
    public fun getPropertyBase64(schemaNS: String, propName: String): ByteArray? =
        getPropertyObject(schemaNS, propName, XMPValueType.BASE64) as? ByteArray

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * *Note:* There is no `setPropertyString()`,
     * because `setProperty()` sets a string value.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `String` value or `null` if the property does not exist.
     */
    public fun getPropertyString(schemaNS: String, propName: String): String? =
        getPropertyObject(schemaNS, propName, XMPValueType.STRING) as? String

    /**
     * Returns a property, but the result value can be requested.
     */
    private fun getPropertyObject(schemaNS: String, propName: String, valueType: XMPValueType): Any? {

        if (schemaNS.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_SCHEMA_TEXT, XMPErrorConst.BADPARAM)

        if (propName.isEmpty())
            throw XMPException(XMPErrorConst.EMPTY_PROPERTY_NAME_TEXT, XMPErrorConst.BADPARAM)

        val propNode = findNode(
            xmpTree = this.root,
            xpath = expandXPath(schemaNS, propName),
            createNodes = false,
            leafOptions = null
        ) ?: return null

        if (valueType != XMPValueType.STRING && propNode.options.isCompositeProperty())
            throw XMPException(
                "Property must be simple when a value type is requested",
                XMPErrorConst.BADXPATH
            )

        return evaluateNodeValue(valueType, propNode)
    }

    /**
     * Convenience method to set a property to a literal `boolean` value.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as `boolean`.
     * @param options   options of the property to set (optional).
     */
    @kotlin.jvm.JvmOverloads
    public fun setPropertyBoolean(
        schemaNS: String,
        propName: String,
        propValue: Boolean,
        options: PropertyOptions = PropertyOptions()
    ): Unit = setProperty(
        schemaNS,
        propName,
        if (propValue) XMPConst.TRUE_STRING else XMPConst.FALSE_STRING,
        options
    )

    /**
     * Convenience method to set a property to a literal `int` value.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as `int`.
     * @param options   options of the property to set (optional).
     */
    @kotlin.jvm.JvmOverloads
    public fun setPropertyInteger(
        schemaNS: String,
        propName: String,
        propValue: Int,
        options: PropertyOptions = PropertyOptions()
    ): Unit = setProperty(schemaNS, propName, propValue, options)

    /**
     * Convenience method to set a property to a literal `long` value.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as `long`.
     * @param options   options of the property to set (optional).
     */
    @kotlin.jvm.JvmOverloads
    public fun setPropertyLong(
        schemaNS: String,
        propName: String,
        propValue: Long,
        options: PropertyOptions = PropertyOptions()
    ): Unit = setProperty(schemaNS, propName, propValue, options)

    /**
     * Convenience method to set a property to a literal `double` value.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as `double`.
     * @param options   options of the property to set (optional).
     */
    @kotlin.jvm.JvmOverloads
    public fun setPropertyDouble(
        schemaNS: String,
        propName: String,
        propValue: Double,
        options: PropertyOptions = PropertyOptions()
    ): Unit = setProperty(schemaNS, propName, propValue, options)

    /**
     * Convenience method to set a property from a binary `byte[]`-array,
     * which is serialized as base64-string.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as byte array.
     * @param options   options of the property to set (optional).
     */
    @kotlin.jvm.JvmOverloads
    public fun setPropertyBase64(
        schemaNS: String,
        propName: String,
        propValue: ByteArray,
        options: PropertyOptions = PropertyOptions()
    ): Unit = setProperty(schemaNS, propName, propValue, options)

    /**
     * Constructs an iterator for the properties within this XMP object.
     *
     * @return Returns an `XMPIterator`.
     */
    public fun iterator(): XMPIterator =
        iterator(IteratorOptions())

    /**
     * Constructs an iterator for the properties within this XMP object using some options.
     *
     * @param options Option flags to control the iteration.
     * @return Returns an `XMPIterator`.
     */
    public fun iterator(options: IteratorOptions): XMPIterator =
        iterator(null, null, options)

    /**
     * Construct an iterator for the properties within an XMP object. According to the parameters it iterates
     * the entire data tree, properties within a specific schema, or a subtree rooted at a specific node.
     *
     * @param schemaNS Optional schema namespace URI to restrict the iteration.
     *                 Omitted (visit all schema) by passing `null` or empty String.
     * @param propName Optional property name to restrict the iteration. May be an arbitrary path
     *                 expression. Omitted (visit all properties) by passing `null` or empty
     *                 String. If no schema URI is given, it is ignored.
     * @param options  Option flags to control the iteration. See [IteratorOptions] for details.
     * @return Returns an `XMPIterator` for this `XMPMeta`-object considering the given options.
     */
    public fun iterator(
        schemaNS: String?,
        propName: String?,
        options: IteratorOptions?
    ): XMPIterator =
        XMPIterator(this, schemaNS, propName, options)

    /**
     * This correlates to the about-attribute,
     * returns the empty String if no name is set.
     *
     * @return Returns the name of the XMP object.
     */
    public fun getObjectName(): String =
        root.name ?: ""

    /**
     * @param name Sets the name of the XMP object.
     */
    public fun setObjectName(name: String) {
        root.name = name
    }

    /**
     * @return Returns the unparsed content of the &lt;?xpacket&gt; processing instruction.
     * This contains normally the attribute-like elements 'begin="&lt;BOM&gt;"
     * id="W5M0MpCehiHzreSzNTczkc9d"' and possibly the deprecated elements 'bytes="1234"' or
     * 'encoding="XXX"'. If the parsed packet has not been wrapped into an xpacket,
     * `null` is returned.
     */
    public fun getPacketHeader(): String? =
        packetHeader

    /**
     * Sets the packetHeader attributes, only used by the parser.
     */
    public fun setPacketHeader(packetHeader: String?) {
        this.packetHeader = packetHeader
    }

    /**
     * Sorts the complete datamodel according to the following rules:
     *
     *  * Schema nodes are sorted by prefix.
     *  * Properties at top level and within structs are sorted by full name, that is prefix + local name.
     *  * Array items are not sorted, even if they have no certain order such as bags.
     *  * Qualifier are sorted, with the exception of "xml:lang" and/or "rdf:type"
     * that stay at the top of the list in that order.
     */
    public fun sort(): Unit =
        root.sort()

    /**
     * Perform the normalization as a separate parsing step.
     * Normally it is done during parsing, unless the parsing option
     * [ParseOptions.OMIT_NORMALIZATION] is set to `true`.
     * *Note:* It does no harm to call this method to an already normalized xmp object.
     * It was a PDF/A requirement to get hand on the unnormalized `XMPMeta` object.
     */
    public fun normalize(options: ParseOptions): XMPMeta =
        normalize(this, options)

    public fun printAllToConsole() {

        val iterator: XMPIterator = iterator()

        while (iterator.hasNext()) {

            val propertyInfo = iterator.next() as? XMPPropertyInfo ?: continue

            println("${propertyInfo.getPath()} = ${propertyInfo.getValue()}")
        }
    }

    /*
     * Convenience methods for commonly used fields
     *
     * Note that these are not standard API for XMP Core.
     * This was added by Ashampoo.
     */

    /** Returns the ISO date string */
    public fun getDateTimeOriginal(): String? =
        getPropertyString(XMPConst.NS_EXIF, "DateTimeOriginal")

    public fun setDateTimeOriginal(isoDate: String): Unit =
        setProperty(XMPConst.NS_EXIF, "DateTimeOriginal", isoDate)

    public fun deleteDateTimeOriginal(): Unit =
        deleteProperty(XMPConst.NS_EXIF, "DateTimeOriginal")

    public fun getOrientation(): Int? =
        getPropertyInteger(XMPConst.NS_TIFF, "Orientation")

    public fun setOrientation(orientation: Int): Unit =
        setPropertyInteger(XMPConst.NS_TIFF, "Orientation", orientation)

    public fun getRating(): Int? =
        getPropertyInteger(XMPConst.NS_XMP, "Rating")

    public fun setRating(rating: Int): Unit =
        setPropertyInteger(XMPConst.NS_XMP, "Rating", rating)

    public fun getGpsLatitude(): String? =
        getPropertyString(XMPConst.NS_EXIF, "GPSLatitude")

    public fun getGpsLongitude(): String? =
        getPropertyString(XMPConst.NS_EXIF, "GPSLongitude")

    public fun setGpsCoordinates(
        latitudeDdm: String,
        longitudeDdm: String
    ) {

        /* This was a mandatory flag in the past, so we write it. */
        setProperty(XMPConst.NS_EXIF, "GPSVersionID", XMPConst.DEFAULT_GPS_VERSION_ID)

        setProperty(XMPConst.NS_EXIF, "GPSLatitude", latitudeDdm)
        setProperty(XMPConst.NS_EXIF, "GPSLongitude", longitudeDdm)
    }

    public fun deleteGpsCoordinates() {

        deleteProperty(XMPConst.NS_EXIF, "GPSVersionID")
        deleteProperty(XMPConst.NS_EXIF, "GPSLatitude")
        deleteProperty(XMPConst.NS_EXIF, "GPSLongitude")
    }

    /**
     * Check for common used fields if they have a positive flagged value.
     */
    public fun isFlagged(): Boolean =
        getPropertyBoolean(XMPConst.NS_DM, XMPConst.FLAGGED_TAG_ADOBE_NAME) == true ||
            getPropertyBoolean(XMPConst.NS_ACDSEE, XMPConst.FLAGGED_TAG_ACDSEE_NAME) == true ||
            getPropertyBoolean(XMPConst.NS_MYLIO, XMPConst.FLAGGED_TAG_MYLIO_NAME) == true ||
            getPropertyBoolean(XMPConst.NS_NARRATIVE, XMPConst.FLAGGED_TAG_NARRATIVE_NAME) == true

    /**
     * Sets flagged/tagged/picked marker for standard schema and other commonly used fields by popular tools.
     */
    public fun setFlagged(flagged: Boolean) {

        /* Set the standard schema */
        setProperty(
            schemaNS = XMPConst.NS_DM,
            propName = XMPConst.FLAGGED_TAG_ADOBE_NAME,
            propValue = if (flagged)
                XMPConst.FLAGGED_TAG_ADOBE_TRUE
            else
                XMPConst.FLAGGED_TAG_ADOBE_FALSE
        )

        setProperty(
            schemaNS = XMPConst.NS_ACDSEE,
            propName = XMPConst.FLAGGED_TAG_ACDSEE_NAME,
            propValue = if (flagged)
                XMPConst.FLAGGED_TAG_ACDSEE_TRUE
            else
                XMPConst.FLAGGED_TAG_ACDSEE_FALSE
        )

        setProperty(
            schemaNS = XMPConst.NS_MYLIO,
            propName = XMPConst.FLAGGED_TAG_MYLIO_NAME,
            propValue = if (flagged)
                XMPConst.FLAGGED_TAG_MYLIO_TRUE
            else
                XMPConst.FLAGGED_TAG_MYLIO_FALSE
        )

        setProperty(
            schemaNS = XMPConst.NS_NARRATIVE,
            propName = XMPConst.FLAGGED_TAG_NARRATIVE_NAME,
            propValue = if (flagged)
                XMPConst.FLAGGED_TAG_NARRATIVE_TRUE
            else
                XMPConst.FLAGGED_TAG_NARRATIVE_FALSE
        )
    }

    /**
     * Gets the regular keywords specified by XMP standard.
     */
    public fun getKeywords(): Set<String> {

        val subjectCount = countArrayItems(XMPConst.NS_DC, XMPConst.XMP_DC_SUBJECT)

        if (subjectCount == 0)
            return emptySet()

        val keywords = mutableSetOf<String>()

        for (index in 1..subjectCount) {

            val keyword = getPropertyString(
                XMPConst.NS_DC,
                "${XMPConst.XMP_DC_SUBJECT}[$index]"
            ) ?: continue

            keywords.add(keyword)
        }

        return keywords
    }

    public fun setKeywords(keywords: Set<String>) {

        /* Delete existing entries, if any */
        deleteProperty(XMPConst.NS_DC, XMPConst.XMP_DC_SUBJECT)

        if (keywords.isEmpty())
            return

        /* Create a new array property. */
        setProperty(
            XMPConst.NS_DC,
            XMPConst.XMP_DC_SUBJECT,
            null,
            arrayOptions
        )

        /* Fill the new array with keywords. */
        for (keyword in keywords.sorted())
            appendArrayItem(
                schemaNS = XMPConst.NS_DC,
                arrayName = XMPConst.XMP_DC_SUBJECT,
                itemValue = keyword
            )
    }

    /**
     * Gets ACDSee keywords from the ACDSee namespace.
     * This can be used as an alternative if the regular keyword property is empty.
     */
    public fun getAcdSeeKeywords(): Set<String> {

        val propertyExists = doesPropertyExist(XMPConst.NS_ACDSEE, XMPConst.XMP_ACDSEE_KEYWORDS)

        if (!propertyExists)
            return emptySet()

        val keywordCount = countArrayItems(XMPConst.NS_ACDSEE, XMPConst.XMP_ACDSEE_KEYWORDS)

        if (keywordCount == 0)
            return emptySet()

        val keywords = mutableSetOf<String>()

        for (index in 1..keywordCount) {

            val keyword = getPropertyString(
                XMPConst.NS_ACDSEE,
                "${XMPConst.XMP_ACDSEE_KEYWORDS}[$index]"
            ) ?: continue

            keywords.add(keyword)
        }

        return keywords
    }

    public fun getFaces(): Map<String, XMPRegionArea> {

        val regionListExists = doesPropertyExist(XMPConst.NS_MWG_RS, XMP_MWG_RS_REGION_LIST)

        if (!regionListExists)
            return emptyMap()

        val regionCount = countArrayItems(XMPConst.NS_MWG_RS, XMP_MWG_RS_REGION_LIST)

        if (regionCount == 0)
            return emptyMap()

        val faces = mutableMapOf<String, XMPRegionArea>()

        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 1..regionCount) {

            val prefix = "Regions/mwg-rs:RegionList[$index]/mwg-rs"

            val regionType = getPropertyString(XMPConst.NS_MWG_RS, "$prefix:Type")

            /* We only want faces. */
            if (regionType != XMPConst.XMP_MWG_RS_TYPE_FACE)
                continue

            val name = getPropertyString(XMPConst.NS_MWG_RS, "$prefix:Name")
            val xPos = getPropertyDouble(XMPConst.NS_MWG_RS, "$prefix:Area/stArea:x")
            val yPos = getPropertyDouble(XMPConst.NS_MWG_RS, "$prefix:Area/stArea:y")
            val width = getPropertyDouble(XMPConst.NS_MWG_RS, "$prefix:Area/stArea:w")
            val height = getPropertyDouble(XMPConst.NS_MWG_RS, "$prefix:Area/stArea:h")

            /* Skip regions with missing data. */
            @Suppress("ComplexCondition")
            if (name == null || xPos == null || yPos == null || width == null || height == null)
                continue

            faces[name] = XMPRegionArea(xPos, yPos, width, height)
        }

        return faces
    }

    public fun setFaces(
        faces: Map<String, XMPRegionArea>,
        widthPx: Int,
        heightPx: Int
    ): Unit {

        /* Delete existing entries, if any */
        deleteProperty(NS_MWG_RS, "Regions")

        if (faces.isNotEmpty()) {

            setStructField(
                NS_MWG_RS, XMP_MWG_RS_APPLIED_TO_DIMENSIONS,
                XMPConst.TYPE_DIMENSIONS, "w",
                widthPx.toString()
            )

            setStructField(
                NS_MWG_RS, XMP_MWG_RS_APPLIED_TO_DIMENSIONS,
                XMPConst.TYPE_DIMENSIONS, "h",
                heightPx.toString()
            )

            setStructField(
                NS_MWG_RS, XMP_MWG_RS_APPLIED_TO_DIMENSIONS,
                XMPConst.TYPE_DIMENSIONS, "unit", "pixel"
            )

            setStructField(
                NS_MWG_RS, "Regions", NS_MWG_RS, "RegionList",
                null, arrayOptions
            )

            faces.onEachIndexed { index, face ->

                val oneBasedIndex = index + 1

                val structNameItem = "Regions/mwg-rs:RegionList[$oneBasedIndex]"
                val structNameArea = "$structNameItem/mwg-rs:Area"

                insertArrayItem(
                    schemaNS = NS_MWG_RS,
                    arrayName = "Regions/mwg-rs:RegionList",
                    itemIndex = oneBasedIndex,
                    itemValue = "",
                    options = PropertyOptions().setStruct(true)
                )

                setStructField(
                    NS_MWG_RS,
                    structNameItem,
                    XMPConst.NS_MWG_RS,
                    "Type",
                    XMPConst.XMP_MWG_RS_TYPE_FACE
                )

                setStructField(
                    NS_MWG_RS,
                    structNameItem,
                    XMPConst.NS_MWG_RS,
                    "Name",
                    face.key
                )

                setStructField(
                    NS_MWG_RS,
                    structNameArea,
                    XMPConst.TYPE_AREA,
                    "x",
                    face.value.xPos.toString()
                )

                setStructField(
                    NS_MWG_RS,
                    structNameArea,
                    XMPConst.TYPE_AREA,
                    "x",
                    face.value.xPos.toString()
                )

                setStructField(
                    NS_MWG_RS,
                    structNameArea,
                    XMPConst.TYPE_AREA,
                    "y",
                    face.value.yPos.toString()
                )

                setStructField(
                    NS_MWG_RS,
                    structNameArea,
                    XMPConst.TYPE_AREA,
                    "w",
                    face.value.width.toString()
                )

                setStructField(
                    NS_MWG_RS,
                    structNameArea,
                    XMPConst.TYPE_AREA,
                    "h",
                    face.value.height.toString()
                )

                setStructField(
                    NS_MWG_RS,
                    structNameArea,
                    XMPConst.TYPE_AREA,
                    "unit",
                    "normalized"
                )
            }
        }
    }

    public fun getPersonsInImage(): Set<String> {

        val personsInImageCount =
            countArrayItems(XMPConst.NS_IPTC_EXT, XMPConst.XMP_IPTC_EXT_PERSON_IN_IMAGE)

        if (personsInImageCount == 0)
            return emptySet()

        val personsInImage = mutableSetOf<String>()

        for (index in 1..personsInImageCount) {

            val personName =
                getPropertyString(
                    XMPConst.NS_IPTC_EXT,
                    "${XMPConst.XMP_IPTC_EXT_PERSON_IN_IMAGE}[$index]"
                ) ?: continue

            personsInImage.add(personName)
        }

        return personsInImage
    }

    public fun setPersonsInImage(
        personsInImage: Set<String>
    ): Unit {

        /* Delete existing entries, if any */
        deleteProperty(XMPConst.NS_IPTC_EXT, XMPConst.XMP_IPTC_EXT_PERSON_IN_IMAGE)

        if (personsInImage.isEmpty())
            return

        /* Create a new array property. */
        setProperty(
            XMPConst.NS_IPTC_EXT,
            XMPConst.XMP_IPTC_EXT_PERSON_IN_IMAGE,
            null,
            arrayOptions
        )

        /* Fill the new array with persons. */
        for (person in personsInImage.sorted())
            appendArrayItem(
                schemaNS = XMPConst.NS_IPTC_EXT,
                arrayName = XMPConst.XMP_IPTC_EXT_PERSON_IN_IMAGE,
                itemValue = person
            )
    }

    /**
     * Get album names
     */
    public fun getAlbums(): Set<String> {

        val subjectCount = countArrayItems(XMPConst.NS_ASHAMPOO, XMPConst.XMP_ASHAMPOO_ALBUMS)

        if (subjectCount == 0)
            return emptySet()

        val keywords = mutableSetOf<String>()

        for (index in 1..subjectCount) {

            val keyword = getPropertyString(
                XMPConst.NS_ASHAMPOO,
                "${XMPConst.XMP_ASHAMPOO_ALBUMS}[$index]"
            ) ?: continue

            keywords.add(keyword)
        }

        return keywords
    }

    public fun setAlbums(
        albums: Set<String>
    ) {

        /* Delete existing entries, if any */
        deleteProperty(XMPConst.NS_ASHAMPOO, XMPConst.XMP_ASHAMPOO_ALBUMS)

        if (albums.isEmpty())
            return

        /* Create a new array property. */
        setProperty(
            XMPConst.NS_ASHAMPOO,
            XMPConst.XMP_ASHAMPOO_ALBUMS,
            null,
            arrayOptions
        )

        /* Fill the new array with album names. */
        for (albumName in albums.sorted())
            appendArrayItem(
                schemaNS = XMPConst.NS_ASHAMPOO,
                arrayName = XMPConst.XMP_ASHAMPOO_ALBUMS,
                itemValue = albumName
            )
    }

    /**
     * Extract the first entry of Iptc4xmpExt:LocationShown
     * or falls back to "photoshop" namespace, if present.
     */
    public fun getLocation(): XMPLocation? {

        val shownLocationsCount = countArrayItems(XMPConst.NS_IPTC_EXT, "Iptc4xmpExt:LocationShown")

        var locationName: String? = null
        var location: String? = null
        var city: String? = null
        var state: String? = null
        var country: String? = null

        /*
         * First try to receive the information from Iptc4xmpExt:LocationShown,
         * because that's the best place to store this information.
         */

        if (shownLocationsCount > 0) {

            val locationNameAltPath = "${XMPConst.XMP_IPTC_EXT_LOCATION_SHOWN}[1]/Iptc4xmpExt:LocationName"
            val iterator: XMPIterator = iterator(XMPConst.NS_IPTC_EXT, locationNameAltPath, null)

            while (iterator.hasNext()) {

                val propertyInfo = iterator.next()

                val value = propertyInfo.getValue()

                if (value.isNotBlank()) {
                    locationName = value
                    break
                }
            }

            location =
                getPropertyString(
                    XMPConst.NS_IPTC_EXT,
                    "${XMPConst.XMP_IPTC_EXT_LOCATION_SHOWN}[1]/Iptc4xmpExt:Sublocation"
                )

            city =
                getPropertyString(
                    XMPConst.NS_IPTC_EXT,
                    "${XMPConst.XMP_IPTC_EXT_LOCATION_SHOWN}[1]/Iptc4xmpExt:City"
                )

            state =
                getPropertyString(
                    XMPConst.NS_IPTC_EXT,
                    "${XMPConst.XMP_IPTC_EXT_LOCATION_SHOWN}[1]/Iptc4xmpExt:ProvinceState"
                )

            country =
                getPropertyString(
                    XMPConst.NS_IPTC_EXT,
                    "${XMPConst.XMP_IPTC_EXT_LOCATION_SHOWN}[1]/Iptc4xmpExt:CountryName"
                )
        }

        /*
         * For missing values fall back to the Photoshop namespace.
         */

        if (city.isNullOrBlank())
            city = getPropertyString(XMPConst.NS_PHOTOSHOP, "City")

        if (state.isNullOrBlank())
            state = getPropertyString(XMPConst.NS_PHOTOSHOP, "State")

        if (country.isNullOrBlank())
            country = getPropertyString(XMPConst.NS_PHOTOSHOP, "Country")

        /*
         * If all fields are NULL we don't have location info in this XMP.
         */
        @Suppress("ComplexCondition")
        if (
            locationName.isNullOrBlank() &&
            location.isNullOrBlank() &&
            city.isNullOrBlank() &&
            state.isNullOrBlank() &&
            country.isNullOrBlank()
        )
            return null

        return XMPLocation(
            name = locationName,
            location = location,
            city = city,
            state = state,
            country = country
        )
    }

    private enum class XMPValueType {

        STRING,
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        BASE64
    }
}
