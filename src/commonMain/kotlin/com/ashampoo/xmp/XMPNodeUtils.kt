// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.Utils.normalizeLangValue
import com.ashampoo.xmp.Utils.replaceControlCharsWithSpace
import com.ashampoo.xmp.Utils.splitNameAndValue
import com.ashampoo.xmp.XMPUtils.encodeBase64
import com.ashampoo.xmp.options.AliasOptions
import com.ashampoo.xmp.options.PropertyOptions
import com.ashampoo.xmp.xpath.XMPPath
import com.ashampoo.xmp.xpath.XMPPathSegment

/**
 * Utilities for `XMPNode`.
 */
object XMPNodeUtils {

    const val CLT_NO_VALUES = 0

    const val CLT_SPECIFIC_MATCH = 1

    const val CLT_SINGLE_GENERIC = 2

    const val CLT_MULTIPLE_GENERIC = 3

    const val CLT_XDEFAULT = 4

    const val CLT_FIRST_ITEM = 5

    /**
     * Find or create a schema node if `createNodes` is false and
     *
     * Note: If `createNodes` is `true`, it is **always** returned a valid node.
     */
    @kotlin.jvm.JvmStatic
    fun findSchemaNode(tree: XMPNode, namespaceURI: String?, createNodes: Boolean): XMPNode? =
        findSchemaNode(tree, namespaceURI, null, createNodes)

    /**
     * Find or create a schema node if `createNodes` is true.
     *
     * Note: If `createNodes` is `true`, it is **always** returned a valid node.
     */
    fun findSchemaNode(
        tree: XMPNode,
        namespaceURI: String?,
        suggestedPrefix: String?,
        createNodes: Boolean
    ): XMPNode? {

        // make sure that its the root
        require(tree.parent == null)

        var schemaNode = tree.findChildByName(namespaceURI)

        if (schemaNode == null && createNodes) {

            schemaNode = XMPNode(
                name = namespaceURI,
                value = null,
                options = PropertyOptions().setSchemaNode(true)
            )

            schemaNode.isImplicit = true

            // only previously registered schema namespaces are allowed in the XMP tree.
            var prefix = XMPSchemaRegistry.getNamespacePrefix(namespaceURI!!)

            if (prefix == null) {

                prefix = if (!suggestedPrefix.isNullOrEmpty())
                    XMPSchemaRegistry.registerNamespace(namespaceURI, suggestedPrefix)
                else
                    throw XMPException("Unregistered schema namespace URI", XMPError.BADSCHEMA)
            }

            schemaNode.value = prefix

            tree.addChild(schemaNode)
        }

        return schemaNode
    }

    /**
     * Find or create a child node under a given parent node.
     */
    fun findChildNode(parent: XMPNode, childName: String?, createNodes: Boolean): XMPNode? {

        if (!parent.options.isSchemaNode() && !parent.options.isStruct()) {

            when {

                !parent.isImplicit ->
                    throw XMPException(
                        "Named children only allowed for schemas and structs: $childName", XMPError.BADXPATH
                    )

                parent.options.isArray() ->
                    throw XMPException("Named children not allowed for arrays: $childName", XMPError.BADXPATH)

                createNodes ->
                    parent.options.setStruct(true)
            }
        }

        var childNode = parent.findChildByName(childName)

        if (childNode == null && createNodes) {

            childNode = XMPNode(childName, null)

            childNode.isImplicit = true

            parent.addChild(childNode)
        }

        check(childNode != null || !createNodes)

        return childNode
    }

    /**
     * Follow an expanded path expression to find or create a node.
     *
     * @param xmpTree     the node to begin the search.
     * @param xpath       the complete xpath
     * @param createNodes flag if nodes shall be created (when called by `setProperty()`)
     * @param leafOptions the options for the created leaf nodes (only when`createNodes == true`).
     * @return Returns the node if found or created or `null`.

     */
    @kotlin.jvm.JvmStatic
    fun findNode(
        xmpTree: XMPNode,
        xpath: XMPPath?,
        createNodes: Boolean,
        leafOptions: PropertyOptions?
    ): XMPNode? {

        if (xpath == null || xpath.size() == 0)
            throw XMPException("Empty XMPPath", XMPError.BADXPATH)

        // Root of implicitly created subtree to possible delete it later.
        // Valid only if leaf is new.
        var rootImplicitNode: XMPNode? = null

        var currNode: XMPNode? =
            findSchemaNode(xmpTree, xpath.getSegment(XMPPath.STEP_SCHEMA).name, createNodes)

        if (currNode == null)
            return null

        if (currNode.isImplicit) {

            currNode.isImplicit = false // Clear the implicit node bit.
            rootImplicitNode = currNode // Save the top most implicit node.
        }

        // Now follow the remaining steps of the original XMPPath.
        try {

            for (index in 1 until xpath.size()) {

                currNode = followXPathStep(currNode!!, xpath.getSegment(index), createNodes)

                if (currNode == null) {

                    // delete implicitly created nodes
                    if (createNodes)
                        deleteNode(rootImplicitNode!!)

                    return null

                } else if (currNode.isImplicit) {

                    // clear the implicit node flag
                    currNode.isImplicit = false

                    // if node is an ALIAS (can be only in root step, auto-create array
                    // when the path has been resolved from a not simple alias type
                    if (index == 1 &&
                        xpath.getSegment(index).isAlias && xpath.getSegment(index).aliasForm != 0
                    ) {
                        currNode.options.setOption(xpath.getSegment(index).aliasForm, true)
                    } else if ( // "CheckImplicitStruct" in C++
                        index < xpath.size() - 1 &&
                        xpath.getSegment(index).kind == XMPPath.STRUCT_FIELD_STEP &&
                        !currNode.options.isCompositeProperty()
                    ) {
                        currNode.options.setStruct(true)
                    }

                    if (rootImplicitNode == null)
                        rootImplicitNode = currNode // Save the top most implicit node.
                }
            }

        } catch (ex: XMPException) {

            // if new notes have been created prior to the error, delete them
            if (rootImplicitNode != null)
                deleteNode(rootImplicitNode)

            throw ex
        }

        if (rootImplicitNode != null) {

            // set options only if a node has been successful created
            if (leafOptions != null)
                currNode!!.options.mergeWith(leafOptions)

            currNode!!.options = currNode.options
        }

        return currNode
    }

    /**
     * Deletes the the given node and its children from its parent.
     * Takes care about adjusting the flags.
     *
     * @param node the top-most node to delete.
     */
    @kotlin.jvm.JvmStatic
    fun deleteNode(node: XMPNode) {

        val parent = node.parent

        if (node.options.isQualifier())
            parent!!.removeQualifier(node)
        else
            parent!!.removeChild(node)

        // delete empty Schema nodes
        if (!parent.hasChildren() && parent.options.isSchemaNode())
            parent.parent!!.removeChild(parent)
    }

    /**
     * This is setting the value of a leaf node.
     *
     * @param node  an XMPNode
     * @param value a value
     */
    @kotlin.jvm.JvmStatic
    fun setNodeValue(node: XMPNode, value: Any?) {

        val strValue = serializeNodeValue(value)

        if (!(node.options.isQualifier() && XMPConst.XML_LANG == node.name))
            node.value = strValue
        else
            node.value = normalizeLangValue(strValue!!)
    }

    /**
     * Verifies the PropertyOptions for consistancy and updates them as needed.
     * If options are `null` they are created with default values.
     *
     * @param options   the `PropertyOptions`
     * @param itemValue the node value to set
     * @return Returns the updated options.
     *
     */
    @kotlin.jvm.JvmStatic
    fun verifySetOptions(options: PropertyOptions, itemValue: Any?): PropertyOptions {

        if (options.isArrayAltText())
            options.setArrayAlternate(true)

        if (options.isArrayAlternate())
            options.setArrayOrdered(true)

        if (options.isArrayOrdered())
            options.setArray(true)

        if (options.isCompositeProperty() && itemValue != null && itemValue.toString().isNotEmpty())
            throw XMPException("Structs and arrays can't have values", XMPError.BADOPTIONS)

        options.assertConsistency(options.getOptions())

        return options
    }

    /**
     * Converts the node value to String, apply special conversions for defined
     * types in XMP.
     *
     * @param value the node value to set
     * @return Returns the String representation of the node value.
     */
    fun serializeNodeValue(value: Any?): String? {

        if (value == null)
            return null

        val strValue: String = when (value) {
            is Boolean -> if (value) XMPConst.TRUE_STRING else XMPConst.FALSE_STRING
            is Int -> value.toString()
            is Long -> value.toString()
            is Double -> value.toString()
            is ByteArray -> encodeBase64(value)
            else -> value.toString()
        }

        return replaceControlCharsWithSpace(strValue)
    }

    /**
     * After processing by ExpandXPath, a step can be of these forms:
     *
     *  * qualName - A top level property or struct field.
     *  * [index] - An element of an array.
     *  * [last()] - The last element of an array.
     *  * [qualName="value"] - An element in an array of structs, chosen by a field value.
     *  * [?qualName="value"] - An element in an array, chosen by a qualifier value.
     *  * ?qualName - A general qualifier.
     *
     * Find the appropriate child node, resolving aliases, and optionally creating nodes.
     */
    private fun followXPathStep(
        parentNode: XMPNode,
        nextStep: XMPPathSegment,
        createNodes: Boolean
    ): XMPNode? {

        var nextNode: XMPNode? = null
        val stepKind = nextStep.kind

        if (stepKind == XMPPath.STRUCT_FIELD_STEP) {
            nextNode = findChildNode(parentNode, nextStep.name, createNodes)
        } else if (stepKind == XMPPath.QUALIFIER_STEP) {
            nextNode = findQualifierNode(parentNode, nextStep.name!!.substring(1), createNodes)
        } else {

            // This is an array indexing step. First get the index, then get the node.
            if (!parentNode.options.isArray())
                throw XMPException("Indexing applied to non-array", XMPError.BADXPATH)

            val index = when (stepKind) {

                XMPPath.ARRAY_INDEX_STEP ->
                    findIndexedItem(parentNode, nextStep.name!!, createNodes)

                XMPPath.ARRAY_LAST_STEP ->
                    parentNode.getChildrenLength()

                XMPPath.FIELD_SELECTOR_STEP -> {

                    val result = splitNameAndValue(nextStep.name!!)
                    val fieldName = result[0]
                    val fieldValue = result[1]

                    lookupFieldSelector(parentNode, fieldName, fieldValue)
                }

                XMPPath.QUAL_SELECTOR_STEP -> {

                    val result = splitNameAndValue(nextStep.name!!)
                    val qualName = result[0]
                    val qualValue = result[1]

                    lookupQualSelector(parentNode, qualName, qualValue, nextStep.aliasForm)
                }

                else ->
                    throw XMPException(
                        "Unknown array indexing step in FollowXPathStep",
                        XMPError.INTERNALFAILURE
                    )
            }

            if (1 <= index && index <= parentNode.getChildrenLength())
                nextNode = parentNode.getChild(index)
        }

        return nextNode
    }

    /**
     * Find or create a qualifier node under a given parent node. Returns a pointer to the
     * qualifier node, and optionally an iterator for the node's position in the parent's vector
     * of qualifiers. The iterator is unchanged if no qualifier node (null) is returned.
     *
     * *Note:* On entry, the qualName parameter must not have the leading '?' from the XMPPath step.
     */
    private fun findQualifierNode(parent: XMPNode?, qualName: String, createNodes: Boolean): XMPNode? {

        require(!qualName.startsWith("?"))

        var qualNode = parent!!.findQualifierByName(qualName)

        if (qualNode == null && createNodes) {
            qualNode = XMPNode(qualName, null)
            qualNode.isImplicit = true
            parent.addQualifier(qualNode)
        }

        return qualNode
    }

    /**
     * @param arrayNode   an array node
     * @param segment     the segment containing the array index
     * @param createNodes flag if new nodes are allowed to be created.
     * @return Returns the index or index = -1 if not found
     */
    private fun findIndexedItem(arrayNode: XMPNode, segment: String, createNodes: Boolean): Int {

        var index: Int

        try {

            val innerSegment = segment.substring(1, segment.length - 1)

            index = innerSegment.toInt()

            if (index < 1)
                throw XMPException("Array index must be larger than zero", XMPError.BADXPATH)

        } catch (ex: NumberFormatException) {
            throw XMPException("Array index not digits.", XMPError.BADXPATH, ex)
        }

        if (createNodes && index == arrayNode.getChildrenLength() + 1) {

            // Append a new last + 1 node.
            val newItem = XMPNode(XMPConst.ARRAY_ITEM_NAME, null)

            newItem.isImplicit = true

            arrayNode.addChild(newItem)
        }

        return index
    }

    /**
     * Searches for a field selector in a node:
     * [fieldName="value] - an element in an array of structs, chosen by a field value.
     * No implicit nodes are created by field selectors.
     *
     * @param arrayNode
     * @param fieldName
     * @param fieldValue
     * @return Returns the index of the field if found, otherwise -1.
     */
    private fun lookupFieldSelector(arrayNode: XMPNode?, fieldName: String, fieldValue: String): Int {

        var result = -1
        var index = 1

        while (index <= arrayNode!!.getChildrenLength() && result < 0) {

            val currItem = arrayNode.getChild(index)

            if (!currItem.options.isStruct())
                throw XMPException("Field selector must be used on array of struct", XMPError.BADXPATH)

            @Suppress("LoopWithTooManyJumpStatements")
            for (childIndex in 1..currItem.getChildrenLength()) {

                val currField = currItem.getChild(childIndex)

                if (fieldName != currField.name)
                    continue

                if (fieldValue == currField.value) {
                    result = index
                    break
                }
            }

            index++
        }

        return result
    }

    /**
     * Searches for a qualifier selector in a node:
     * [?qualName="value"] - an element in an array, chosen by a qualifier value.
     * No implicit nodes are created for qualifier selectors, except for an alias to an x-default item.
     */
    private fun lookupQualSelector(
        arrayNode: XMPNode,
        qualName: String,
        qualValue: String,
        aliasForm: Int
    ): Int {

        return if (XMPConst.XML_LANG == qualName) {

            val normalizedQualValue = normalizeLangValue(qualValue)

            val index = lookupLanguageItem(arrayNode, normalizedQualValue)

            if (index < 0 && aliasForm and AliasOptions.PROP_ARRAY_ALT_TEXT > 0) {

                val langNode = XMPNode(XMPConst.ARRAY_ITEM_NAME, null)

                val xdefault = XMPNode(XMPConst.XML_LANG, XMPConst.X_DEFAULT)

                langNode.addQualifier(xdefault)

                arrayNode.addChild(1, langNode)

                1

            } else {
                index
            }

        } else {

            for (index in 1 until arrayNode.getChildrenLength()) {

                val currItem = arrayNode.getChild(index)

                for (qualifier in currItem.getQualifier())
                    if (qualName == qualifier.name && qualValue == qualifier.value)
                        return index
            }

            -1
        }
    }

    /**
     * Make sure the x-default item is first. Touch up &quot;single value&quot;
     * arrays that have a default plus one real language. This case should have
     * the same value for both items. Older Adobe apps were hardwired to only
     * use the &quot;x-default&quot; item, so we copy that value to the other
     * item.
     *
     * @param arrayNode an alt text array node
     */
    fun normalizeLangArray(arrayNode: XMPNode) {

        if (!arrayNode.options.isArrayAltText())
            return

        // check if node with x-default qual is first place
        for (index in 2..arrayNode.getChildrenLength()) {

            val child = arrayNode.getChild(index)

            if (child.hasQualifier() && XMPConst.X_DEFAULT == child.getQualifier(1).value) {

                // move node to first place
                arrayNode.removeChild(index)
                arrayNode.addChild(1, child)

                if (index == 2)
                    arrayNode.getChild(2).value = child.value

                break
            }
        }
    }

    /**
     * See if an array is an alt-text array. If so, make sure the x-default item
     * is first.
     *
     * @param arrayNode the array node to check if its an alt-text array
     */
    fun detectAltText(arrayNode: XMPNode) {

        if (arrayNode.options.isArrayAlternate() && arrayNode.hasChildren()) {

            var isAltText = false

            for (child in arrayNode.getChildren()) {
                if (child.options.hasLanguage()) {
                    isAltText = true
                    break
                }
            }

            if (isAltText) {
                arrayNode.options.setArrayAltText(true)
                normalizeLangArray(arrayNode)
            }
        }
    }

    /**
     * Appends a language item to an alt text array.
     */
    @kotlin.jvm.JvmStatic
    fun appendLangItem(arrayNode: XMPNode, itemLang: String?, itemValue: String?) {

        val newItem = XMPNode(XMPConst.ARRAY_ITEM_NAME, itemValue)
        val langQual = XMPNode(XMPConst.XML_LANG, itemLang)

        newItem.addQualifier(langQual)

        if (XMPConst.X_DEFAULT != langQual.value)
            arrayNode.addChild(newItem)
        else
            arrayNode.addChild(1, newItem)
    }

    /**
     *  1. Look for an exact match with the specific language.
     *  1. If a generic language is given, look for partial matches.
     *  1. Look for an "x-default"-item.
     *  1. Choose the first item.
     */
    @kotlin.jvm.JvmStatic
    fun chooseLocalizedText(arrayNode: XMPNode, genericLang: String?, specificLang: String): Array<Any?> {

        // See if the array has the right form. Allow empty alt arrays, that is what parsing returns.

        if (!arrayNode.options.isArrayAltText())
            throw XMPException("Localized text array is not alt-text", XMPError.BADXPATH)
        else if (!arrayNode.hasChildren())
            return arrayOf(CLT_NO_VALUES, null)

        var foundGenericMatches = 0
        var resultNode: XMPNode? = null
        var xDefault: XMPNode? = null

        // Look for the first partial match with the generic language.
        val it = arrayNode.iterateChildren()

        while (it.hasNext()) {

            val currItem = it.next()

            // perform some checks on the current item
            if (currItem.options.isCompositeProperty())
                throw XMPException("Alt-text array item is not simple", XMPError.BADXPATH)
            else if (!currItem.hasQualifier() || XMPConst.XML_LANG != currItem.getQualifier(1).name)
                throw XMPException("Alt-text array item has no language qualifier", XMPError.BADXPATH)

            val currLang = currItem.getQualifier(1).value

            // Look for an exact match with the specific language.
            when {

                specificLang == currLang ->
                    return arrayOf(CLT_SPECIFIC_MATCH, currItem)

                genericLang != null && currLang!!.startsWith(genericLang) -> {

                    if (resultNode == null)
                        resultNode = currItem

                    // ! Don't return/break, need to look for other matches.
                    foundGenericMatches++
                }

                XMPConst.X_DEFAULT == currLang ->
                    xDefault = currItem
            }
        }

        // evaluate loop
        return when {

            foundGenericMatches == 1 ->
                arrayOf(CLT_SINGLE_GENERIC, resultNode)

            foundGenericMatches > 1 ->
                arrayOf(CLT_MULTIPLE_GENERIC, resultNode)

            xDefault != null ->
                arrayOf(CLT_XDEFAULT, xDefault)

            else -> // Everything failed, choose the first item.
                arrayOf(CLT_FIRST_ITEM, arrayNode.getChild(1))
        }
    }

    /**
     * Looks for the appropriate language item in a text alternative array.item
     * Returns the index if the language has been found, -1 otherwise.
     */
    fun lookupLanguageItem(arrayNode: XMPNode?, language: String): Int {

        if (!arrayNode!!.options.isArray())
            throw XMPException("Language item must be used on array", XMPError.BADXPATH)

        for (index in 1..arrayNode.getChildrenLength()) {

            val child = arrayNode.getChild(index)

            if (!child.hasQualifier() || XMPConst.XML_LANG != child.getQualifier(1).name)
                continue
            else if (language == child.getQualifier(1).value)
                return index
        }

        return -1
    }
}
