// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.internal.QName
import com.ashampoo.xmp.internal.XMPErrorConst
import com.ashampoo.xmp.internal.XMPNode
import com.ashampoo.xmp.internal.XMPNodeUtils.findNode
import com.ashampoo.xmp.internal.XMPNodeUtils.findSchemaNode
import com.ashampoo.xmp.options.IteratorOptions
import com.ashampoo.xmp.options.PropertyOptions
import com.ashampoo.xmp.properties.XMPPropertyInfo
import com.ashampoo.xmp.xpath.XMPPath
import com.ashampoo.xmp.xpath.XMPPathParser.expandXPath

/**
 * Interface for the `XMPMeta` iteration services.
 * `XMPIterator` provides a uniform means to iterate over the
 * schema and properties within an XMP object.
 *
 * The iteration over the schema and properties within an XMP object is very
 * complex. It is helpful to have a thorough understanding of the XMP data tree.
 * One way to learn this is to create some complex XMP and examine the output of
 * `XMPMeta#toString`. This is also described in the XMP
 * Specification, in the XMP Data Model chapter.
 *
 * The top of the XMP data tree is a single root node. This does not explicitly
 * appear in the dump and is never visited by an iterator (that is, it is never
 * returned from `XMPIterator#next()`). Beneath the root are
 * schema nodes. These are just collectors for top level properties in the same
 * namespace. They are created and destroyed implicitly. Beneath the schema
 * nodes are the property nodes. The nodes below a property node depend on its
 * type (simple, struct, or array) and whether it has qualifiers.
 *
 * An `XMPIterator` is created by XMPMeta#interator() constructor
 * defines a starting point for the iteration and options that control how it
 * proceeds. By default the iteration starts at the root and visits all nodes
 * beneath it in a depth first manner. The root node is not visited, the first
 * visited node is a schema node. You can provide a schema name or property path
 * to select a different starting node. By default this visits the named root
 * node first then all nodes beneath it in a depth first manner.
 *
 * The `XMPIterator#next()` method delivers the schema URI, path,
 * and option flags for the node being visited. If the node is simple it also
 * delivers the value. Qualifiers for this node are visited next. The fields of
 * a struct or items of an array are visited after the qualifiers of the parent.
 *
 * The options to control the iteration are:
 *
 *  * JUST_CHILDREN - Visit just the immediate children of the root. Skip
 * the root itself and all nodes below the immediate children. This omits the
 * qualifiers of the immediate children, the qualifier nodes being below what
 * they qualify, default is to visit the complete subtree.
 *  * JUST_LEAFNODES - Visit just the leaf property nodes and their
 * qualifiers.
 *  * JUST_LEAFNAME - Return just the leaf component of the node names.
 * The default is to return the full xmp path.
 *  * OMIT_QUALIFIERS - Do not visit the qualifiers.
 *  * INCLUDE_ALIASES - Adds known alias properties to the properties in the iteration.
 * *Note:* Not supported in Java XMPCore!
 *
 * `next()` returns `XMPPropertyInfo`-objects and throws
 * a `NoSuchElementException` if there are no more properties to
 * return.
 */
public class XMPIterator(
    xmp: XMPMeta,
    schemaNS: String?,
    propPath: String?,
    options: IteratorOptions?
) : Iterator<XMPPropertyInfo> {

    private val options: IteratorOptions

    /**
     * the base namespace of the property path, will be changed during the iteration
     */
    private var baseNS: String? = null

    /**
     * flag to indicate that skipSiblings() has been called.
     */
    private var skipSiblings = false

    /**
     * flag to indicate that skipSubtree() has been called.
     */
    private var skipSubtree = false

    /**
     * the node iterator doing the work
     */
    private var nodeIterator: Iterator<XMPPropertyInfo>? = null

    /**
     * Constructor with optionsl initial values. If `propName` is provided,
     * `schemaNS` has also be provided.
     *
     * @param xmp      the iterated metadata object.
     * @param schemaNS the iteration is reduced to this schema (optional)
     * @param propPath the iteration is redurce to this property within the `schemaNS`
     * @param options  advanced iteration options, see [IteratorOptions]
     *
     */
    init {

        // make sure that options is defined at least with defaults
        this.options = options ?: IteratorOptions()

        // the start node of the iteration depending on the schema and property filter
        var startNode: XMPNode?
        var initialPath: String? = null
        val baseSchema = !schemaNS.isNullOrEmpty()
        val baseProperty = !propPath.isNullOrEmpty()

        when {

            !baseSchema && !baseProperty -> {

                // complete tree will be iterated
                startNode = xmp.root
            }

            baseSchema && baseProperty -> {

                // Schema and property node provided

                val path = expandXPath(schemaNS, propPath)

                // base path is the prop path without the property leaf
                val basePath = XMPPath()

                for (i in 0 until path.size() - 1)
                    basePath.add(path.getSegment(i))

                startNode = findNode(xmp.root, path, false, null)
                baseNS = schemaNS
                initialPath = basePath.toString()
            }

            baseSchema && !baseProperty -> {

                // Only Schema provided
                startNode = findSchemaNode(xmp.root, schemaNS, false)
            }

            else -> {

                // !baseSchema  &&  baseProperty
                // No schema but property provided -> error
                throw XMPException("Schema namespace URI is required", XMPErrorConst.BADSCHEMA)
            }
        }

        // create iterator
        if (startNode != null) {

            if (!this.options.isJustChildren())
                nodeIterator = NodeIterator(startNode, initialPath, 1)
            else
                nodeIterator = NodeIteratorChildren(startNode, initialPath)

        } else {

            nodeIterator = emptySequence<XMPPropertyInfo>().iterator()
        }
    }

    /**
     * Skip the subtree below the current node when `next()` is
     * called.
     */
    public fun skipSubtree() {
        skipSubtree = true
    }

    /**
     * Skip the subtree below and remaining siblings of the current node when
     * `next()` is called.
     */
    public fun skipSiblings() {
        skipSubtree()
        skipSiblings = true
    }

    override fun hasNext(): Boolean =
        nodeIterator!!.hasNext()

    override fun next(): XMPPropertyInfo =
        nodeIterator!!.next()

    /**
     * The `XMPIterator` implementation.
     * It first returns the node itself, then recursivly the children and qualifier of the node.
     */
    private open inner class NodeIterator : Iterator<XMPPropertyInfo> {

        /**
         * the state of the iteration
         */
        private var state = ITERATE_NODE

        /**
         * the currently visited node
         */
        private var visitedNode: XMPNode? = null

        /**
         * the recursively accumulated path
         */
        private var path: String? = null

        /**
         * the iterator that goes through the children and qualifier list
         */
        protected var childrenIterator: Iterator<XMPNode>? = null

        /**
         * index of node with parent, only interesting for arrays
         */
        private var index = 0

        /**
         * the iterator for each child
         */
        private var subIterator = emptySequence<XMPPropertyInfo>().iterator()

        /**
         * the cached `PropertyInfo` to return
         */
        protected var returnProperty: XMPPropertyInfo? = null

        /**
         * Default constructor
         */
        constructor()

        /**
         * Constructor for the node iterator.
         *
         * @param visitedNode the currently visited node
         * @param parentPath  the accumulated path of the node
         * @param index       the index within the parent node (only for arrays)
         */
        constructor(visitedNode: XMPNode, parentPath: String?, index: Int) {

            this.visitedNode = visitedNode
            state = ITERATE_NODE

            if (visitedNode.options.isSchemaNode())
                baseNS = visitedNode.name

            // for all but the root node and schema nodes
            path = accumulatePath(visitedNode, parentPath, index)
        }

        /**
         * Prepares the next node to return if not already done.
         *
         * @see Iterator.hasNext
         */
        override fun hasNext(): Boolean {

            if (returnProperty != null)
                return true // hasNext has been called before

            // find next node
            return if (state == ITERATE_NODE) {

                reportNode()

            } else if (state == ITERATE_CHILDREN) {

                if (childrenIterator == null)
                    childrenIterator = visitedNode!!.iterateChildren()

                var hasNext = iterateChildren(childrenIterator!!)

                if (!hasNext && visitedNode!!.hasQualifier() && !options.isOmitQualifiers()) {
                    state = ITERATE_QUALIFIER
                    childrenIterator = null
                    hasNext = hasNext()
                }

                hasNext

            } else {

                if (childrenIterator == null)
                    childrenIterator = visitedNode!!.iterateQualifier()

                iterateChildren(childrenIterator!!)
            }
        }

        /**
         * Sets the returnProperty as next item or recurses into `hasNext()`.
         *
         * @return Returns if there is a next item to return.
         */
        protected fun reportNode(): Boolean {

            state = ITERATE_CHILDREN

            return if (visitedNode!!.parent != null &&
                (!options.isJustLeafnodes() || !visitedNode!!.hasChildren())
            ) {
                returnProperty = createPropertyInfo(visitedNode, baseNS!!, path!!)
                true
            } else {
                hasNext()
            }
        }

        /**
         * Handles the iteration of the children or qualfier
         *
         * @return Returns if there are more elements available.
         */
        private fun iterateChildren(iterator: Iterator<XMPNode>): Boolean {

            if (skipSiblings) {

                skipSiblings = false

                subIterator = emptySequence<XMPPropertyInfo>().iterator()
            }

            /*
             * Create sub iterator for every child, if its the first child
             * visited or the former child is finished
             */
            if (!subIterator.hasNext() && iterator.hasNext()) {

                val child = iterator.next()

                index++

                subIterator = NodeIterator(child, path, index)
            }

            if (subIterator.hasNext()) {

                returnProperty = subIterator.next()

                /* We have more available */
                return true
            }

            /* There are no more children - end iteration. */
            return false
        }

        /**
         * Calls hasNext() and returnes the prepared node. Afterward its set to null.
         * The existance of returnProperty indicates if there is a next node, otherwise
         * an exceptio is thrown.
         *
         * @see Iterator.next
         */
        override fun next(): XMPPropertyInfo {

            if (!hasNext())
                throw NoSuchElementException("There are no more nodes to return")

            val result = returnProperty

            returnProperty = null

            return result!!
        }

        /**
         * @param currNode     the node that will be added to the path.
         * @param parentPath   the path up to this node.
         * @param currentIndex the current array index if an arrey is traversed
         * @return Returns the updated path.
         */
        protected fun accumulatePath(currNode: XMPNode, parentPath: String?, currentIndex: Int): String? {

            val separator: String
            val segmentName: String?

            if (currNode.parent == null || currNode.options.isSchemaNode()) {
                return null
            } else if (currNode.parent!!.options.isArray()) {
                separator = ""
                segmentName = "[$currentIndex]"
            } else {
                separator = "/"
                segmentName = currNode.name
            }

            return if (parentPath.isNullOrEmpty()) {

                segmentName

            } else if (options.isJustLeafname()) {

                if (!segmentName!!.startsWith("?"))
                    segmentName
                else
                    segmentName.substring(1) // qualifier

            } else {

                parentPath + separator + segmentName
            }
        }

        /**
         * Creates a property info object from an `XMPNode`.
         *
         * @param node   an `XMPNode`
         * @param baseNS the base namespace to report
         * @param path   the full property path
         * @return Returns a `XMPProperty`-object that serves representation of the node.
         */
        protected fun createPropertyInfo(
            node: XMPNode?,
            baseNS: String,
            path: String
        ): XMPPropertyInfo {

            val value = if (node!!.options.isSchemaNode())
                null
            else
                node.value

            return object : XMPPropertyInfo {

                override fun getNamespace(): String {

                    if (node.options.isSchemaNode())
                        return baseNS

                    // determine namespace of leaf node
                    val qname = QName(node.name!!)

                    return XMPSchemaRegistry.getNamespaceURI(qname.prefix!!)!!
                }

                override fun getPath(): String = path

                override fun getValue(): String = value!!

                override fun getOptions(): PropertyOptions = node.options

                // the language is not reported
                override fun getLanguage(): String? = null
            }
        }
    }

    /**
     * This iterator is derived from the default `NodeIterator`,
     * and is only used for the option [IteratorOptions.JUST_CHILDREN].
     */
    private inner class NodeIteratorChildren(parentNode: XMPNode, parentPath: String?) : NodeIterator() {

        private val parentPath: String

        private val nodeChildrenIterator: Iterator<XMPNode>

        private var index = 0

        /**
         * Constructor
         *
         * @param parentNode the node which children shall be iterated.
         * @param parentPath the full path of the former node without the leaf node.
         */
        init {

            if (parentNode.options.isSchemaNode())
                baseNS = parentNode.name

            this.parentPath = accumulatePath(parentNode, parentPath, 1)!!

            nodeChildrenIterator = parentNode.iterateChildren()
        }

        /**
         * Prepares the next node to return if not already done.
         *
         * @see Iterator.hasNext
         */
        override fun hasNext(): Boolean {

            // hasNext has been called before
            if (returnProperty != null)
                return true

            if (skipSiblings)
                return false

            if (!nodeChildrenIterator.hasNext())
                return false

            val child = nodeChildrenIterator.next()

            index++

            var path: String? = null

            if (child.options.isSchemaNode())
                baseNS = child.name
            else if (child.parent != null)
                path = accumulatePath(child, parentPath, index)

            // report next property, skip not-leaf nodes in case options is set
            if (!options.isJustLeafnodes() || !child.hasChildren()) {
                returnProperty = createPropertyInfo(child, baseNS!!, path!!)
                return true
            }

            return hasNext()
        }
    }

    private companion object {

        /**
         * iteration state
         */
        const val ITERATE_NODE = 0

        /**
         * iteration state
         */
        const val ITERATE_CHILDREN = 1

        /**
         * iteration state
         */
        const val ITERATE_QUALIFIER = 2
    }
}
