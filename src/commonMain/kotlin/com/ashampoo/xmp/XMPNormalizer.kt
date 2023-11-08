// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.Utils.checkUUIDFormat
import com.ashampoo.xmp.xpath.XMPPathParser.expandXPath
import com.ashampoo.xmp.options.ParseOptions
import com.ashampoo.xmp.options.PropertyOptions

internal object XMPNormalizer {

    /**
     * caches the correct dc-property array forms
     */
    private val dcArrayForms: Map<String, PropertyOptions> = createDCArrays()

    /**
     * Normalizes a raw parsed XMPMeta-Object
     *
     * @param xmp     the raw metadata object
     * @param options the parsing options
     * @return Returns the normalized metadata object
     *
     */
    @kotlin.jvm.JvmStatic
    fun normalize(xmp: XMPMeta, options: ParseOptions): XMPMeta {

        val tree = xmp.root

        touchUpDataModel(xmp)
        moveExplicitAliases(tree, options)
        tweakOldXMP(tree)
        deleteEmptySchemas(tree)

        return xmp
    }

    /**
     * Tweak old XMP: Move an instance ID from rdf:about to the
     * *xmpMM:InstanceID* property. An old instance ID usually looks
     * like &quot;uuid:bac965c4-9d87-11d9-9a30-000d936b79c4&quot;, plus InDesign
     * 3.0 wrote them like &quot;bac965c4-9d87-11d9-9a30-000d936b79c4&quot;.
     *
     * If the name looks like a UUID simply move it to *xmpMM:InstanceID*,
     * don't worry about any existing *xmpMM:InstanceID*. Both will
     * only be present when a newer file with the *xmpMM:InstanceID*
     * property is updated by an old app that uses *rdf:about*.
     */
    private fun tweakOldXMP(tree: XMPNode) {

        if (tree.name != null && tree.name!!.length >= Utils.UUID_LENGTH) {

            var nameStr = tree.name!!.lowercase()

            if (nameStr.startsWith("uuid:"))
                nameStr = nameStr.substring(5)

            if (checkUUIDFormat(nameStr)) {

                // move UUID to xmpMM:InstanceID and remove it from the root node
                val path = expandXPath(XMPConst.NS_XMP_MM, "InstanceID")
                val idNode = XMPNodeUtils.findNode(tree, path, true, null)

                if (idNode == null)
                    throw XMPException("Failure creating xmpMM:InstanceID", XMPError.INTERNALFAILURE)

                idNode.options = PropertyOptions() // Clobber any existing xmpMM:InstanceID.
                idNode.value = "uuid:$nameStr"
                idNode.removeChildren()
                idNode.removeQualifiers()

                tree.name = null
            }
        }
    }

    /**
     * Visit all schemas to do general fixes and handle special cases.
     */
    private fun touchUpDataModel(xmp: XMPMeta) {

        // make sure the DC schema is existing, because it might be needed within the normalization
        // if not touched it will be removed by removeEmptySchemas
        XMPNodeUtils.findSchemaNode(xmp.root, XMPConst.NS_DC, true)

        // Do the special case fixes within each schema.
        val it = xmp.root.iterateChildren()

        while (it.hasNext()) {

            val currSchema = it.next()

            when {

                XMPConst.NS_DC == currSchema.name ->
                    normalizeDCArrays(currSchema)

                XMPConst.NS_EXIF == currSchema.name ->
                    XMPNodeUtils.findChildNode(currSchema, "exif:UserComment", false)
                        ?.let { userComment -> repairAltText(userComment) }

                XMPConst.NS_XMP_RIGHTS == currSchema.name ->
                    XMPNodeUtils.findChildNode(currSchema, "xmpRights:UsageTerms", false)
                        ?.let { usageTerms -> repairAltText(usageTerms) }
            }
        }
    }

    /**
     * Undo the denormalization performed by the XMP used in Acrobat 5.
     * If a Dublin Core array had only one item, it was serialized as a simple property.
     * The `xml:lang` attribute was dropped from an `alt-text` item if the language was `x-default`.
     *
     */
    private fun normalizeDCArrays(dcSchema: XMPNode) {

        for (index in 1..dcSchema.getChildrenLength()) {

            val currProp = dcSchema.getChild(index)

            val arrayForm = dcArrayForms[currProp.name]

            if (arrayForm == null) {

                continue

            } else if (currProp.options.isSimple()) {

                // create a new array and add the current property as child, if it was formerly simple
                val newArray = XMPNode(currProp.name, null, arrayForm)

                currProp.name = XMPConst.ARRAY_ITEM_NAME
                newArray.addChild(currProp)
                dcSchema.replaceChild(index, newArray)

                // fix language alternatives
                if (arrayForm.isArrayAltText() && !currProp.options.hasLanguage()) {

                    val newLang = XMPNode(XMPConst.XML_LANG, XMPConst.X_DEFAULT)

                    currProp.addQualifier(newLang)
                }

            } else {

                // clear array options and add corrected array form if it has been an array before
                currProp.options.setOption(
                    PropertyOptions.ARRAY or
                        PropertyOptions.ARRAY_ORDERED or
                        PropertyOptions.ARRAY_ALTERNATE or
                        PropertyOptions.ARRAY_ALT_TEXT,
                    false
                )

                currProp.options.mergeWith(arrayForm)

                // applying for "dc:description", "dc:rights", "dc:title"
                if (arrayForm.isArrayAltText())
                    repairAltText(currProp)
            }
        }
    }

    /**
     * Make sure that the array is well-formed AltText. Each item must be simple
     * and have an "xml:lang" qualifier. If repairs are needed, keep simple
     * non-empty items by adding the "xml:lang" with value "x-repair".
     *
     * @param arrayNode the property node of the array to repair.
     */
    private fun repairAltText(arrayNode: XMPNode?) {

        if (arrayNode == null || !arrayNode.options.isArray())
            return // Already OK or not even an array.

        // fix options
        arrayNode.options.setArrayOrdered(true).setArrayAlternate(true).setArrayAltText(true)

        val it = arrayNode.iterateChildrenMutable()

        while (it.hasNext()) {

            val currChild = it.next()

            if (currChild.options.isCompositeProperty()) {

                // Delete non-simple children.
                it.remove()

            } else if (!currChild.options.hasLanguage()) {

                val childValue = currChild.value

                if (childValue.isNullOrEmpty()) {

                    // Delete empty valued children that have no xml:lang.
                    it.remove()

                } else {

                    // Add a xml:lang qualifier with the value "x-repair".
                    val repairLang = XMPNode(XMPConst.XML_LANG, "x-repair")
                    currChild.addQualifier(repairLang)
                }
            }
        }
    }

    /**
     * Visit all the top level nodes looking for aliases. If there is
     * no base, transplant the alias subtree. If there is a base and strict
     * aliasing is on, make sure the alias and base subtrees match.
     *
     * @param tree    the root of the metadata tree
     * @param options th parsing options
     */
    private fun moveExplicitAliases(tree: XMPNode, options: ParseOptions) {

        if (!tree.hasAliases)
            return

        tree.hasAliases = false

        val strictAliasing = options.getStrictAliasing()

        val schemaIt: Iterator<XMPNode> = tree.iterateChildren()

        while (schemaIt.hasNext()) {

            val currSchema = schemaIt.next()

            if (!currSchema.hasAliases)
                continue

            val propertyIt = currSchema.iterateChildrenMutable()

            while (propertyIt.hasNext()) {

                val currProp = propertyIt.next()

                if (!currProp.isAlias)
                    continue

                currProp.isAlias = false

                // Find the base path, look for the base schema and root node.
                val info = XMPSchemaRegistryImpl.findAlias(currProp.name!!)

                if (info != null) {

                    // find or create schema
                    val baseSchema = XMPNodeUtils.findSchemaNode(
                        tree, info.getNamespace(), null, true
                    )

                    checkNotNull(baseSchema) { "SchemaNode should have been created." }

                    baseSchema.isImplicit = false

                    var baseNode = XMPNodeUtils.findChildNode(
                        baseSchema,
                        info.getPrefix() + info.getPropName(), false
                    )

                    if (baseNode == null) {

                        if (info.getAliasForm().isSimple()) {

                            // A top-to-top alias, transplant the property.
                            // change the alias property name to the base name
                            val qname = info.getPrefix() + info.getPropName()

                            currProp.name = qname

                            baseSchema.addChild(currProp)

                            // remove the alias property
                            propertyIt.remove()

                        } else {

                            // An alias to an array item,
                            // create the array and transplant the property.
                            baseNode = XMPNode(
                                name = info.getPrefix() + info.getPropName(),
                                value = null,
                                options = info.getAliasForm().toPropertyOptions()
                            )

                            baseSchema.addChild(baseNode)

                            transplantArrayItemAlias(propertyIt, currProp, baseNode)
                        }

                    } else if (info.getAliasForm().isSimple()) {

                        // The base node does exist and this is a top-to-top alias.
                        // Check for conflicts if strict aliasing is on.
                        // Remove and delete the alias subtree.
                        if (strictAliasing)
                            compareAliasedSubtrees(currProp, baseNode, true)

                        propertyIt.remove()

                    } else {

                        // This is an alias to an array item and the array exists.
                        // Look for the aliased item.
                        // Then transplant or check & delete as appropriate.
                        var itemNode: XMPNode? = null

                        if (info.getAliasForm().isArrayAltText()) {

                            val xdIndex = XMPNodeUtils.lookupLanguageItem(baseNode, XMPConst.X_DEFAULT)

                            if (xdIndex != -1)
                                itemNode = baseNode.getChild(xdIndex)

                        } else if (baseNode.hasChildren()) {

                            itemNode = baseNode.getChild(1)
                        }

                        if (itemNode == null) {

                            transplantArrayItemAlias(propertyIt, currProp, baseNode)

                        } else {

                            if (strictAliasing)
                                compareAliasedSubtrees(currProp, itemNode, true)

                            propertyIt.remove()
                        }
                    }
                }
            }

            currSchema.hasAliases = false
        }
    }

    /**
     * Moves an alias node of array form to another schema into an array
     *
     * @param propertyIt the property iterator of the old schema (used to delete the property)
     * @param childNode  the node to be moved
     * @param baseArray  the base array for the array item
     *
     */
    private fun transplantArrayItemAlias(
        propertyIt: MutableIterator<XMPNode>,
        childNode: XMPNode,
        baseArray: XMPNode
    ) {

        if (baseArray.options.isArrayAltText()) {

            // *** Allow x-default.
            if (childNode.options.hasLanguage())
                throw XMPException("Alias to x-default already has a language qualifier", XMPError.BADXMP)

            val langQual = XMPNode(XMPConst.XML_LANG, XMPConst.X_DEFAULT)

            childNode.addQualifier(langQual)
        }

        propertyIt.remove()

        childNode.name = XMPConst.ARRAY_ITEM_NAME

        baseArray.addChild(childNode)
    }

    /**
     * Remove all empty schemas from the metadata tree that were generated during the rdf parsing.
     *
     * @param tree the root of the metadata tree
     */
    private fun deleteEmptySchemas(tree: XMPNode) {

        // Delete empty schema nodes. Do this last, other cleanup can make empty schema.

        val it = tree.iterateChildrenMutable()

        while (it.hasNext()) {

            val schema = it.next()

            if (!schema.hasChildren())
                it.remove()
        }
    }

    /**
     * The outermost call is special. The names almost certainly differ. The
     * qualifiers (and hence options) will differ for an alias to the x-default
     * item of a langAlt array.
     *
     * @param aliasNode the alias node
     * @param baseNode  the base node of the alias
     * @param outerCall marks the outer call of the recursion
     *
     */
    private fun compareAliasedSubtrees(
        aliasNode: XMPNode,
        baseNode: XMPNode,
        outerCall: Boolean
    ) {

        if (aliasNode.value != baseNode.value || aliasNode.getChildrenLength() != baseNode.getChildrenLength())
            throw XMPException("Mismatch between alias and base nodes", XMPError.BADXMP)

        if (!outerCall &&
            (
                aliasNode.name != baseNode.name ||
                    aliasNode.options != baseNode.options ||
                    aliasNode.getQualifierLength() != baseNode.getQualifierLength()
                )
        )
            throw XMPException("Mismatch between alias and base nodes", XMPError.BADXMP)

        run {
            val an = aliasNode.iterateChildren()
            val bn = baseNode.iterateChildren()

            while (an.hasNext() && bn.hasNext()) {
                val aliasChild = an.next()
                val baseChild = bn.next()
                compareAliasedSubtrees(aliasChild, baseChild, false)
            }
        }

        val an = aliasNode.iterateQualifier()
        val bn = baseNode.iterateQualifier()

        while (an.hasNext() && bn.hasNext()) {

            val aliasQual = an.next()
            val baseQual = bn.next()

            compareAliasedSubtrees(aliasQual, baseQual, false)
        }
    }

    /**
     * Initializes the map that contains the known arrays, that are fixed by
     * [XMPNormalizer.normalizeDCArrays].
     */
    private fun createDCArrays(): Map<String, PropertyOptions> {

        val dcArrayForms = mutableMapOf<String, PropertyOptions>()

        // Properties supposed to be a "Bag".
        val bagForm = PropertyOptions()
        bagForm.setArray(true)
        dcArrayForms["dc:contributor"] = bagForm
        dcArrayForms["dc:language"] = bagForm
        dcArrayForms["dc:publisher"] = bagForm
        dcArrayForms["dc:relation"] = bagForm
        dcArrayForms["dc:subject"] = bagForm
        dcArrayForms["dc:type"] = bagForm

        // Properties supposed to be a "Seq".
        val seqForm = PropertyOptions()
        seqForm.setArray(true)
        seqForm.setArrayOrdered(true)
        dcArrayForms["dc:creator"] = seqForm
        dcArrayForms["dc:date"] = seqForm

        // Properties supposed to be an "Alt" in alternative-text form.
        val altTextForm = PropertyOptions()
        altTextForm.setArray(true)
        altTextForm.setArrayOrdered(true)
        altTextForm.setArrayAlternate(true)
        altTextForm.setArrayAltText(true)
        dcArrayForms["dc:description"] = altTextForm
        dcArrayForms["dc:rights"] = altTextForm
        dcArrayForms["dc:title"] = altTextForm

        return dcArrayForms
    }
}
