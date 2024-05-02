// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.internal

import com.ashampoo.xmp.XMPConst
import com.ashampoo.xmp.options.PropertyOptions

/**
 * A node in the internally XMP tree, which can be a schema node, a property node, an array node,
 * an array item, a struct node or a qualifier node (without '?').
 */
internal class XMPNode(

    /**
     * name of the node, contains different information depending of the node kind
     */
    var name: String?,

    /**
     * value of the node, contains different information depending of the node kind
     */
    var value: String?,

    /**
     * options describing the kind of the node
     */
    var options: PropertyOptions = PropertyOptions()

) : Comparable<XMPNode> {

    internal var parent: XMPNode? = null
    private var children: MutableList<XMPNode>? = null
    private var qualifier: MutableList<XMPNode>? = null

    /* Internal processing options */

    public var isImplicit: Boolean = false
        internal set

    public var hasAliases: Boolean = false
        internal set

    public var isAlias: Boolean = false
        internal set

    public var hasValueChild: Boolean = false
        internal set

    internal fun clear() {
        name = null
        value = null
        options = PropertyOptions()
        children = null
        qualifier = null
    }

    /**
     * Returns the children or empty list, if there are none.
     * Will not lazily create the list!
     */
    public fun getChildren(): List<XMPNode> =
        children ?: emptyList()

    public fun getChild(index: Int): XMPNode =
        getOrCreateChildren()[index - 1]

    public fun addChild(node: XMPNode) {

        /* Ignore doubled childs. */
        if (childExists(node.name!!))
            return

        node.parent = this

        getOrCreateChildren().add(node)
    }

    public fun addChild(index: Int, node: XMPNode) {

        /* Ignore doubled childs. */
        if (childExists(node.name!!))
            return

        node.parent = this

        getOrCreateChildren().add(index - 1, node)
    }

    /**
     * Replaces a node with another one.
     */
    public fun replaceChild(index: Int, node: XMPNode) {

        node.parent = this

        getOrCreateChildren()[index - 1] = node
    }

    public fun removeChild(itemIndex: Int) {

        getOrCreateChildren().removeAt(itemIndex - 1)

        cleanupChildren()
    }

    /**
     * Removes a child node.
     * If its a schema node and doesn't have any children anymore, its deleted.
     */
    public fun removeChild(node: XMPNode) {

        getOrCreateChildren().remove(node)

        cleanupChildren()
    }

    /**
     * Removes the children list if this node has no children anymore;
     * checks if the provided node is a schema node and doesn't have any children anymore, its deleted.
     */
    private fun cleanupChildren() {

        if (children?.isEmpty() == null)
            children = null
    }

    /**
     * Removes all children from the node.
     */
    public fun removeChildren() {
        children = null
    }

    public fun getChildrenLength(): Int =
        children?.size ?: 0

    public fun findChildByName(expr: String?): XMPNode? =
        getOrCreateChildren().find { it.name == expr }

    /**
     * Returns the qualifier or empty list, if there are none.
     * Will not lazily create the list!
     */
    public fun getQualifier(): List<XMPNode> =
        qualifier ?: emptyList()

    public fun getQualifier(index: Int): XMPNode =
        getOrCreateQualifier()[index - 1]

    public fun getQualifierLength(): Int =
        qualifier?.size ?: 0

    public fun addQualifier(qualNode: XMPNode) {

        /* Ignore doubled qualifiers. */
        if (qualifierExists(qualNode.name!!))
            return

        qualNode.parent = this
        qualNode.options.setQualifier(true)

        options.setHasQualifiers(true)

        // contraints
        if (XMPConst.XML_LANG == qualNode.name) {

            // "xml:lang" is always first and the option "hasLanguage" is set
            options.setHasLanguage(true)

            getOrCreateQualifier().add(0, qualNode)

        } else if (XMPConst.RDF_TYPE == qualNode.name) {

            // "rdf:type" must be first or second after "xml:lang" and the option "hasType" is set
            options.setHasType(true)

            getOrCreateQualifier().add(
                if (!options.hasLanguage()) 0 else 1,
                qualNode
            )

        } else {

            // other qualifiers are appended
            getOrCreateQualifier().add(qualNode)
        }
    }

    /**
     * Removes one qualifier node and fixes the options.
     */
    public fun removeQualifier(qualNode: XMPNode) {

        if (XMPConst.XML_LANG == qualNode.name) {
            // if "xml:lang" is removed, remove hasLanguage-flag too
            options.setHasLanguage(false)
        } else if (XMPConst.RDF_TYPE == qualNode.name) {
            // if "rdf:type" is removed, remove hasType-flag too
            options.setHasType(false)
        }

        getOrCreateQualifier().remove(qualNode)

        if (qualifier!!.isEmpty()) {
            options.setHasQualifiers(false)
            qualifier = null
        }
    }

    /**
     * Removes all qualifiers from the node and sets the options appropriate.
     */
    public fun removeQualifiers() {

        // clear qualifier related options
        options.setHasQualifiers(false)
        options.setHasLanguage(false)
        options.setHasType(false)

        qualifier = null
    }

    public fun findQualifierByName(expr: String?): XMPNode? =
        qualifier?.find { it.name == expr }

    public fun hasChildren(): Boolean =
        children?.isNotEmpty() ?: false

    public fun iterateChildren(): Iterator<XMPNode> =
        children?.iterator() ?: emptySequence<XMPNode>().iterator()

    public fun iterateChildrenMutable(): MutableIterator<XMPNode> =
        children?.listIterator() ?: mutableListOf<XMPNode>().listIterator()

    public fun hasQualifier(): Boolean =
        qualifier?.isNotEmpty() ?: false

    public fun iterateQualifier(): Iterator<XMPNode> =
        qualifier?.listIterator() ?: emptySequence<XMPNode>().iterator()

    override fun compareTo(other: XMPNode): Int {

        if (options.isSchemaNode())
            return value!!.compareTo(other.value!!)

        return name!!.compareTo(other.name!!)
    }

    /**
     * Sorts the complete datamodel according to the following rules:
     *
     *  * Nodes at one level are sorted by name, that is prefix + local name
     *  * Starting at the root node the children and qualifier are sorted recursively,
     * which the following exceptions.
     *  * Sorting will not be used for arrays.
     *  * Within qualifier "xml:lang" and/or "rdf:type" stay at the top in that order, all others are sorted.
     */
    public fun sort() {

        // sort qualifier
        if (hasQualifier()) {

            val quals = getOrCreateQualifier().toTypedArray<XMPNode>()

            var sortFrom = 0

            while (quals.size > sortFrom &&
                (XMPConst.XML_LANG == quals[sortFrom].name || XMPConst.RDF_TYPE == quals[sortFrom].name)
            ) {
                quals[sortFrom].sort()
                sortFrom++
            }

            quals.sort(sortFrom, quals.size)

            val iterator = qualifier!!.listIterator()

            for (j in quals.indices) {
                iterator.next()
                iterator.set(quals[j])
                quals[j].sort()
            }
        }

        // sort children
        if (hasChildren()) {

            if (!options.isArray())
                children!!.sort()

            val it = iterateChildren()

            while (it.hasNext())
                it.next().sort()
        }
    }

    // ------------------------------------------------------------------------------ private methods

    private fun getOrCreateChildren(): MutableList<XMPNode> {

        if (children == null)
            children = mutableListOf()

        return children!!
    }

    private fun getOrCreateQualifier(): MutableList<XMPNode> {

        if (qualifier == null)
            qualifier = mutableListOf()

        return qualifier!!
    }

    private fun childExists(childName: String): Boolean =
        XMPConst.ARRAY_ITEM_NAME != childName && findChildByName(childName) != null

    private fun qualifierExists(qualifierName: String): Boolean =
        XMPConst.ARRAY_ITEM_NAME != qualifierName && findQualifierByName(qualifierName) != null
}
