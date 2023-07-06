// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.properties.XMPPropertyInfo

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
interface XMPIterator : Iterator<XMPPropertyInfo> {

    /**
     * Skip the subtree below the current node when `next()` is
     * called.
     */
    fun skipSubtree()

    /**
     * Skip the subtree below and remaining siblings of the current node when
     * `next()` is called.
     */
    fun skipSiblings()

}
