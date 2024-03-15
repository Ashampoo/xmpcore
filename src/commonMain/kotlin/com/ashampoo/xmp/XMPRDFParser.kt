// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.options.ParseOptions
import com.ashampoo.xmp.options.PropertyOptions
import nl.adaptivity.xmlutil.dom.Attr
import nl.adaptivity.xmlutil.dom.Element
import nl.adaptivity.xmlutil.dom.Node
import nl.adaptivity.xmlutil.dom.NodeConsts
import nl.adaptivity.xmlutil.dom.Text
import nl.adaptivity.xmlutil.dom.attributes
import nl.adaptivity.xmlutil.dom.childNodes
import nl.adaptivity.xmlutil.dom.data
import nl.adaptivity.xmlutil.dom.length
import nl.adaptivity.xmlutil.dom.localName
import nl.adaptivity.xmlutil.dom.namespaceURI
import nl.adaptivity.xmlutil.dom.nodeName
import nl.adaptivity.xmlutil.dom.nodeType
import nl.adaptivity.xmlutil.dom.ownerElement
import nl.adaptivity.xmlutil.dom.prefix
import nl.adaptivity.xmlutil.dom.value

/**
 * Parser for "normal" XML serialisation of RDF.
 */
@Suppress("TooManyFunctions")
internal object XMPRDFParser {

    const val RDFTERM_OTHER = 0

    /**
     * Start of coreSyntaxTerms.
     */
    const val RDFTERM_RDF = 1

    const val RDFTERM_ID = 2

    const val RDFTERM_ABOUT = 3

    const val RDFTERM_PARSE_TYPE = 4

    const val RDFTERM_RESOURCE = 5

    const val RDFTERM_NODE_ID = 6

    /**
     * End of coreSyntaxTerms
     */
    const val RDFTERM_DATATYPE = 7

    /**
     * Start of additions for syntax Terms.
     */
    const val RDFTERM_DESCRIPTION = 8

    /**
     * End of of additions for syntaxTerms.
     */
    const val RDFTERM_LI = 9

    /**
     * Start of oldTerms.
     */
    const val RDFTERM_ABOUT_EACH = 10

    const val RDFTERM_ABOUT_EACH_PREFIX = 11

    /**
     * End of oldTerms.
     */
    const val RDFTERM_BAG_ID = 12

    const val RDFTERM_FIRST_CORE = RDFTERM_RDF

    const val RDFTERM_LAST_CORE = RDFTERM_DATATYPE

    /**
     * ! Yes, the syntax terms include the core terms.
     */
    const val RDFTERM_FIRST_SYNTAX = RDFTERM_FIRST_CORE

    const val RDFTERM_LAST_SYNTAX = RDFTERM_LI

    const val RDFTERM_FIRST_OLD = RDFTERM_ABOUT_EACH

    const val RDFTERM_LAST_OLD = RDFTERM_BAG_ID

    /**
     * this prefix is used for default namespaces
     */
    const val DEFAULT_PREFIX = "_dflt"

    /**
     * The main parsing method. The XML tree is walked through from the root node and and XMP tree
     * is created. This is a raw parse, the normalisation of the XMP tree happens outside.
     *
     */
    @kotlin.jvm.JvmStatic
    fun parse(xmlRoot: Node, options: ParseOptions): XMPMeta {

        val xmp = XMPMeta()

        parseRdfRoot(xmp, xmlRoot, options)

        return xmp
    }

    /**
     * Each of these parsing methods is responsible for recognizing an RDF
     * syntax production and adding the appropriate structure to the XMP tree.
     * They simply return for success, failures will throw an exception.
     */
    @Suppress("ThrowsCount", "UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    fun parseRdfRoot(xmp: XMPMeta, rdfRdfNode: Node, options: ParseOptions) {

        if (rdfRdfNode.nodeName != "rdf:RDF")
            throw XMPException("Root node should be of type rdf:RDF", XMPError.BADRDF)

        if (rdfRdfNode.nodeType != NodeConsts.ELEMENT_NODE)
            throw XMPException("Root node must be of element type.", XMPError.BADRDF)

        rdfRdfNode as Element

        if (rdfRdfNode.attributes.length == 0)
            throw XMPException("Illegal: rdf:RDF node has no attributes", XMPError.BADRDF)

        for (index in 0 until rdfRdfNode.childNodes.length) {

            val child = rdfRdfNode.childNodes.item(index)!!

            /* Filter whitespace nodes. */
            if (isWhitespaceNode(child))
                continue

            parseRdfNodeElement(xmp, xmp.root, child as Element, true, options)
        }
    }

    /**
     * 7.2.5 nodeElementURIs
     * anyURI - ( coreSyntaxTerms | rdf:li | oldTerms )
     *
     * 7.2.11 nodeElement
     * start-element ( URI == nodeElementURIs,
     * attributes == set ( ( idAttr | nodeIdAttr | aboutAttr )?, propertyAttr* ) )
     * propertyEltList
     * end-element()
     *
     * A node element URI is rdf:Description or anything else that is not an RDF
     * term.
     */
    private fun parseRdfNodeElement(
        xmp: XMPMeta,
        xmpParent: XMPNode,
        xmlNode: Element,
        isTopLevel: Boolean,
        options: ParseOptions
    ) {

        val nodeTerm = getRDFTermKind(xmlNode)

        if (nodeTerm != RDFTERM_DESCRIPTION && nodeTerm != RDFTERM_OTHER)
            throw XMPException("Node element must be rdf:Description or typed node", XMPError.BADRDF)

        if (isTopLevel && nodeTerm == RDFTERM_OTHER)
            throw XMPException("Top level typed node not allowed", XMPError.BADXMP)

        parseRdfNodeElementAttrs(xmp, xmpParent, xmlNode, isTopLevel)
        parseRdfPropertyElementList(xmp, xmpParent, xmlNode, isTopLevel, options)
    }

    /**
     * 7.2.7 propertyAttributeURIs
     * anyURI - ( coreSyntaxTerms | rdf:Description | rdf:li | oldTerms )
     *
     * 7.2.11 nodeElement
     * start-element ( URI == nodeElementURIs,
     * attributes == set ( ( idAttr | nodeIdAttr | aboutAttr )?, propertyAttr* ) )
     * propertyEltList
     *
     * Process the attribute list for an RDF node element. A property attribute URI is
     * anything other than an RDF term. The rdf:ID and rdf:nodeID attributes are simply ignored,
     * as are rdf:about attributes on inner nodes.
     */
    @Suppress("ThrowsCount")
    private fun parseRdfNodeElementAttrs(
        xmp: XMPMeta,
        xmpParent: XMPNode,
        xmlNode: Element,
        isTopLevel: Boolean
    ) {

        // Used to detect attributes that are mutually exclusive.
        var exclusiveAttrs = 0

        for (index in 0 until xmlNode.attributes.length) {

            val attribute = xmlNode.attributes.item(index) as Attr

            // quick hack, ns declarations do not appear in C++
            // ignore "ID" without namespace
            if ("xmlns" == attribute.prefix || attribute.prefix == null && "xmlns" == attribute.nodeName)
                continue

            val attrTerm = getRDFTermKind(attribute)

            when (attrTerm) {

                RDFTERM_ID, RDFTERM_NODE_ID, RDFTERM_ABOUT -> {

                    if (exclusiveAttrs > 0)
                        throw XMPException("Mutally exclusive about, ID, nodeID attributes", XMPError.BADRDF)

                    exclusiveAttrs++

                    if (isTopLevel && attrTerm == RDFTERM_ABOUT) {

                        // This is the rdf:about attribute on a top level node. Set
                        // the XMP tree name if
                        // it doesn't have a name yet. Make sure this name matches
                        // the XMP tree name.
                        if (xmpParent.name != null && xmpParent.name!!.isNotEmpty()) {

                            if (xmpParent.name != attribute.value)
                                throw XMPException("Mismatched top level rdf:about values", XMPError.BADXMP)

                        } else {
                            xmpParent.name = attribute.value
                        }
                    }
                }

                RDFTERM_OTHER ->
                    addChildNode(xmp, xmpParent, attribute, attribute.value, isTopLevel)

                else -> throw XMPException("Invalid nodeElement attribute", XMPError.BADRDF)
            }
        }
    }

    /**
     * 7.2.13 propertyEltList
     * ws* ( propertyElt ws* )*
     *
     * @param xmp        the xmp metadata object that is generated
     * @param xmpParent  the parent xmp node
     * @param xmlParent  the currently processed XML node
     * @param isTopLevel Flag if the node is a top-level node
     * @param options    ParseOptions to indicate the parse options provided by the client
     */
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    private fun parseRdfPropertyElementList(
        xmp: XMPMeta,
        xmpParent: XMPNode,
        xmlParent: Node?,
        isTopLevel: Boolean,
        options: ParseOptions
    ) {

        for (index in 0 until xmlParent!!.childNodes.length) {

            val currChild = xmlParent.childNodes.item(index)!!

            if (isWhitespaceNode(currChild))
                continue

            if (currChild.nodeType != NodeConsts.ELEMENT_NODE)
                throw XMPException("Expected property element node not found", XMPError.BADRDF)

            parseRdfPropertyElement(xmp, xmpParent, currChild as Element, isTopLevel, options)
        }
    }

    /**
     * 7.2.14 propertyElt
     *
     * resourcePropertyElt | literalPropertyElt | parseTypeLiteralPropertyElt |
     * parseTypeResourcePropertyElt | parseTypeCollectionPropertyElt |
     * parseTypeOtherPropertyElt | emptyPropertyElt
     *
     * 7.2.15 resourcePropertyElt
     * start-element ( URI == propertyElementURIs, attributes == set ( idAttr? ) )
     * ws* nodeElement ws*
     * end-element()
     *
     * 7.2.16 literalPropertyElt
     * start-element (
     * URI == propertyElementURIs, attributes == set ( idAttr?, datatypeAttr?) )
     * text()
     * end-element()
     *
     * 7.2.17 parseTypeLiteralPropertyElt
     * start-element (
     * URI == propertyElementURIs, attributes == set ( idAttr?, parseLiteral ) )
     * literal
     * end-element()
     *
     * 7.2.18 parseTypeResourcePropertyElt
     * start-element (
     * URI == propertyElementURIs, attributes == set ( idAttr?, parseResource ) )
     * propertyEltList
     * end-element()
     *
     * 7.2.19 parseTypeCollectionPropertyElt
     * start-element (
     * URI == propertyElementURIs, attributes == set ( idAttr?, parseCollection ) )
     * nodeElementList
     * end-element()
     *
     * 7.2.20 parseTypeOtherPropertyElt
     * start-element ( URI == propertyElementURIs, attributes == set ( idAttr?, parseOther ) )
     * propertyEltList
     * end-element()
     *
     * 7.2.21 emptyPropertyElt
     * start-element ( URI == propertyElementURIs,
     * attributes == set ( idAttr?, ( resourceAttr | nodeIdAttr )?, propertyAttr* ) )
     * end-element()
     *
     * The various property element forms are not distinguished by the XML element name,
     * but by their attributes for the most part. The exceptions are resourcePropertyElt and
     * literalPropertyElt. They are distinguished by their XML element content.
     *
     * NOTE: The RDF syntax does not explicitly include the xml:lang attribute although it can
     * appear in many of these. We have to allow for it in the attibute counts below.
     */
    @Suppress("NestedBlockDepth", "UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    private fun parseRdfPropertyElement(
        xmp: XMPMeta,
        xmpParent: XMPNode,
        xmlNode: Element,
        isTopLevel: Boolean,
        options: ParseOptions
    ) {

        val nodeTerm = getRDFTermKind(xmlNode)

        if (!isPropertyElementName(nodeTerm))
            throw XMPException("Invalid property element name", XMPError.BADRDF)

        // remove the namespace-definitions from the list
        val attributes = xmlNode.attributes

        var nsAttrs: MutableList<String>? = null

        for (index in 0 until attributes.length) {

            val attribute = attributes.item(index) as Attr

            if ("xmlns" == attribute.prefix || attribute.prefix == null && "xmlns" == attribute.nodeName) {

                if (nsAttrs == null)
                    nsAttrs = mutableListOf<String>()

                nsAttrs.add(attribute.nodeName)
            }
        }

        if (nsAttrs != null) {

            val it = nsAttrs.iterator()

            while (it.hasNext())
                attributes.removeNamedItem(it.next())
        }

        if (attributes.length > 3) {

            // Only an emptyPropertyElt can have more than 3 attributes.
            parseEmptyPropertyElement(xmp, xmpParent, xmlNode, isTopLevel)

        } else {

            // Look through the attributes for one that isn't rdf:ID or xml:lang,
            // it will usually tell what we should be dealing with.
            // The called routines must verify their specific syntax!
            for (index in 0 until attributes.length) {

                val attribute = attributes.item(index) as Attr

                val attrValue = attribute.value

                val condition = XMPConst.XML_LANG == attribute.nodeName &&
                    !("ID" == attribute.localName && XMPConst.NS_RDF == attribute.namespaceURI)

                if (!condition) {

                    when {

                        "datatype" == attribute.localName && XMPConst.NS_RDF == attribute.namespaceURI ->
                            parseRdfLiteralPropertyElement(xmp, xmpParent, xmlNode, isTopLevel)

                        !("parseType" == attribute.localName && XMPConst.NS_RDF == attribute.namespaceURI) ->
                            parseEmptyPropertyElement(xmp, xmpParent, xmlNode, isTopLevel)

                        "Literal" == attrValue ->
                            throw XMPException("Literal property element not allowed", XMPError.BADXMP)

                        "Resource" == attrValue ->
                            parseTypeResourcePropertyElement(xmp, xmpParent, xmlNode, isTopLevel, options)

                        "Collection" == attrValue ->
                            throw XMPException("Collection property element forbidden", XMPError.BADXMP)

                        else ->
                            throw XMPException("Other property element not allowed", XMPError.BADXMP)
                    }

                    return
                }
            }

            // Only rdf:ID and xml:lang, could be a resourcePropertyElt, a literalPropertyElt,
            // or an emptyPropertyElt. Look at the child XML nodes to decide which.
            if (xmlNode.childNodes.length > 0) {

                for (index in 0 until xmlNode.childNodes.length) {

                    val currentChild = xmlNode.childNodes.item(index)

                    if (currentChild?.nodeType != NodeConsts.TEXT_NODE) {

                        parseRdfResourcePropertyElement(xmp, xmpParent, xmlNode, isTopLevel, options)

                        return
                    }
                }

                parseRdfLiteralPropertyElement(xmp, xmpParent, xmlNode, isTopLevel)

            } else {

                parseEmptyPropertyElement(xmp, xmpParent, xmlNode, isTopLevel)
            }
        }
    }

    /**
     * 7.2.15 resourcePropertyElt
     * start-element ( URI == propertyElementURIs, attributes == set ( idAttr? ) )
     * ws* nodeElement ws*
     * end-element()
     *
     * This handles structs using an rdf:Description node,
     * arrays using rdf:Bag/Seq/Alt, and typedNodes. It also catches and cleans up qualified
     * properties written with rdf:Description and rdf:value.
     */
    private fun parseRdfResourcePropertyElement(
        xmp: XMPMeta,
        xmpParent: XMPNode,
        xmlNode: Element,
        isTopLevel: Boolean,
        options: ParseOptions
    ) {

        // Strip old "punchcard" chaff which has on the prefix "iX:".
        if (isTopLevel && "iX:changes" == xmlNode.nodeName)
            return

        val newCompound = addChildNode(xmp, xmpParent, xmlNode, "", isTopLevel)

        // walk through the attributes
        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 0 until xmlNode.attributes.length) {

            val attribute = xmlNode.attributes.item(index) as Attr

            if ("xmlns" == attribute.prefix || attribute.prefix == null && "xmlns" == attribute.nodeName)
                continue

            if (XMPConst.XML_LANG == attribute.nodeName)
                addQualifierNode(newCompound, XMPConst.XML_LANG, attribute.value)
            else if ("ID" == attribute.localName && XMPConst.NS_RDF == attribute.namespaceURI)
                continue
            else
                throw XMPException("Invalid attribute for resource property element", XMPError.BADRDF)
        }

        // walk through the children
        var found = false

        for (index in 0 until xmlNode.childNodes.length) {

            val currentChild = xmlNode.childNodes.item(index)!!

            if (!isWhitespaceNode(currentChild)) {

                if (currentChild.nodeType == NodeConsts.ELEMENT_NODE && !found) {

                    currentChild as Element

                    val isRDF = XMPConst.NS_RDF == currentChild.namespaceURI

                    val localName = currentChild.localName

                    when {

                        isRDF && "Bag" == localName ->
                            newCompound.options.setArray(true)

                        isRDF && "Seq" == localName ->
                            newCompound.options.setArray(true).setArrayOrdered(true)

                        isRDF && "Alt" == localName ->
                            newCompound.options.setArray(true).setArrayOrdered(true).setArrayAlternate(true)

                        else -> {

                            newCompound.options.setStruct(true)

                            if (!isRDF && "Description" != localName) {

                                var typeName = currentChild.namespaceURI
                                    ?: throw XMPException(
                                        "All XML elements must be in a namespace", XMPError.BADXMP
                                    )

                                typeName += ":$localName"

                                addQualifierNode(newCompound, XMPConst.RDF_TYPE, typeName)
                            }
                        }
                    }

                    parseRdfNodeElement(xmp, newCompound, currentChild, false, options)

                    if (newCompound.hasValueChild)
                        fixupQualifiedNode(newCompound)
                    else if (newCompound.options.isArrayAlternate())
                        XMPNodeUtils.detectAltText(newCompound)

                    found = true

                } else if (found) {
                    // found second child element
                    throw XMPException("Invalid child of resource property element", XMPError.BADRDF)
                } else {
                    throw XMPException(
                        "Children of resource property element must be XML elements", XMPError.BADRDF
                    )
                }
            }
        }

        if (!found)
            throw XMPException("Missing child of resource property element", XMPError.BADRDF)
    }

    /**
     * 7.2.16 literalPropertyElt
     * start-element ( URI == propertyElementURIs,
     * attributes == set ( idAttr?, datatypeAttr?) )
     * text()
     * end-element()
     *
     * Add a leaf node with the text value and qualifiers for the attributes.
     */
    private fun parseRdfLiteralPropertyElement(
        xmp: XMPMeta,
        xmpParent: XMPNode,
        xmlNode: Element,
        isTopLevel: Boolean
    ) {

        val newChild = addChildNode(xmp, xmpParent, xmlNode, null, isTopLevel)

        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 0 until xmlNode.attributes.length) {

            val attribute = xmlNode.attributes.item(index) as Attr

            if ("xmlns" == attribute.prefix || attribute.prefix == null && "xmlns" == attribute.nodeName)
                continue

            if (XMPConst.XML_LANG == attribute.nodeName)
                addQualifierNode(newChild, XMPConst.XML_LANG, attribute.value)
            else if (
                XMPConst.NS_RDF == attribute.namespaceURI &&
                ("ID" == attribute.localName || "datatype" == attribute.localName)
            )
                continue
            else
                throw XMPException("Invalid attribute for literal property element", XMPError.BADRDF)
        }

        var textValue = ""

        for (index in 0 until xmlNode.childNodes.length) {

            val child = xmlNode.childNodes.item(index)

            if (child?.nodeType != NodeConsts.TEXT_NODE)
                throw XMPException("Invalid child of literal property element", XMPError.BADRDF)

            child as Text

            textValue += child.data
        }

        newChild.value = textValue
    }

    /**
     * 7.2.18 parseTypeResourcePropertyElt
     * start-element ( URI == propertyElementURIs,
     * attributes == set ( idAttr?, parseResource ) )
     * propertyEltList
     * end-element()
     *
     * Add a new struct node with a qualifier for the possible rdf:ID attribute.
     * Then process the XML child nodes to get the struct fields.
     */
    private fun parseTypeResourcePropertyElement(
        xmp: XMPMeta,
        xmpParent: XMPNode,
        xmlNode: Element,
        isTopLevel: Boolean,
        options: ParseOptions
    ) {

        val newStruct = addChildNode(xmp, xmpParent, xmlNode, "", isTopLevel)

        newStruct.options.setStruct(true)

        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 0 until xmlNode.attributes.length) {

            val attribute = xmlNode.attributes.item(index) as Attr

            if ("xmlns" == attribute.prefix || attribute.prefix == null && "xmlns" == attribute.nodeName)
                continue

            if (XMPConst.XML_LANG == attribute.nodeName) {
                addQualifierNode(newStruct, XMPConst.XML_LANG, attribute.value)
            } else if (
                XMPConst.NS_RDF == attribute.namespaceURI &&
                ("ID" == attribute.localName || "parseType" == attribute.localName)
            ) {
                continue // The caller ensured the value is "Resource". Ignore all rdf:ID attributes.
            } else {
                throw XMPException(
                    "Invalid attribute for ParseTypeResource property element", XMPError.BADRDF
                )
            }
        }

        parseRdfPropertyElementList(xmp, newStruct, xmlNode, false, options)

        if (newStruct.hasValueChild)
            fixupQualifiedNode(newStruct)
    }

    /**
     * 7.2.21 emptyPropertyElt
     * start-element ( URI == propertyElementURIs,
     * attributes == set (idAttr?, ( resourceAttr | nodeIdAttr )?, propertyAttr* ) ) end-element()
     *
     * <ns:Prop1></ns:Prop1>
     * <ns:Prop2 rdf:resource="http: *www.adobe.com/"></ns:Prop2>
     * <ns:Prop3 rdf:value="..." ns:Qual="..."></ns:Prop3>
     * <ns:Prop4 ns:Field1="..." ns:Field2="..."></ns:Prop4>
     *
     * An emptyPropertyElt is an element with no contained content, just a possibly empty set of
     * attributes. An emptyPropertyElt can represent three special cases of simple XMP properties: a
     * simple property with an empty value (ns:Prop1), a simple property whose value is a URI
     * (ns:Prop2), or a simple property with simple qualifiers (ns:Prop3).
     * An emptyPropertyElt can also represent an XMP struct whose fields are all simple and
     * unqualified (ns:Prop4).
     *
     * It is an error to use both rdf:value and rdf:resource - that can lead to invalid  RDF in the
     * verbose form written using a literalPropertyElt.
     *
     * The XMP mapping for an emptyPropertyElt is a bit different from generic RDF, partly for
     * design reasons and partly for historical reasons. The XMP mapping rules are:
     *
     *  1. If there is an rdf:value attribute then this is a simple property with a text value.
     *     All other attributes are qualifiers.
     *  2. If there is an rdf:resource attribute then this is a simple property with a URI value.
     *     All other attributes are qualifiers.
     *  3. If there are no attributes other than xml:lang, rdf:ID, or rdf:nodeID then this is a simple
     *     property with an empty value.
     *  4. Otherwise this is a struct, the attributes other than xml:lang, rdf:ID, or rdf:nodeID are fields.
     */
    private fun parseEmptyPropertyElement(
        xmp: XMPMeta,
        xmpParent: XMPNode,
        xmlNode: Element,
        isTopLevel: Boolean
    ) {

        var hasPropertyAttrs = false
        var hasResourceAttr = false
        var hasNodeIDAttr = false
        var hasValueAttr = false
        var valueNode: Node? = null // ! Can come from rdf:value or rdf:resource.

        if (xmlNode.childNodes.length > 0)
            throw XMPException(
                "Nested content not allowed with rdf:resource or property attributes", XMPError.BADRDF
            )

        /* First figure out what XMP this maps to and remember the XML node for a simple value. */
        for (index in 0 until xmlNode.attributes.length) {

            val attribute = xmlNode.attributes.item(index) as Attr

            if ("xmlns" == attribute.prefix || attribute.prefix == null && "xmlns" == attribute.nodeName)
                continue

            val attrTerm = getRDFTermKind(attribute)

            when (attrTerm) {

                /*
                 * Do nothing.
                 */
                RDFTERM_ID -> continue

                /*
                 * sample_52.xmp has an <rdf:li rdf:about=''/> we want to skip.
                 */
                RDFTERM_ABOUT -> continue

                RDFTERM_RESOURCE -> {

                    if (hasNodeIDAttr) {
                        throw XMPException(
                            "Empty property element can't have both rdf:resource and rdf:nodeID",
                            XMPError.BADRDF
                        )
                    } else if (hasValueAttr) {
                        throw XMPException(
                            "Empty property element can't have both rdf:value and rdf:resource",
                            XMPError.BADXMP
                        )
                    }

                    hasResourceAttr = true

                    if (!hasValueAttr)
                        valueNode = attribute
                }

                RDFTERM_NODE_ID -> {

                    if (hasResourceAttr) {
                        throw XMPException(
                            "Empty property element can't have both rdf:resource and rdf:nodeID",
                            XMPError.BADRDF
                        )
                    }

                    hasNodeIDAttr = true
                }

                RDFTERM_OTHER -> {

                    if (attribute.localName == "value" && attribute.namespaceURI == XMPConst.NS_RDF) {

                        if (hasResourceAttr) {
                            throw XMPException(
                                "Empty property element can't have both rdf:value and rdf:resource",
                                XMPError.BADXMP
                            )
                        }

                        hasValueAttr = true
                        valueNode = attribute

                    } else if (XMPConst.XML_LANG != attribute.nodeName) {

                        hasPropertyAttrs = true
                    }
                }

                /* Fail on unknown elements. */
                else ->
                    throw XMPException(
                        "Unrecognized attribute of empty property element: $attrTerm",
                        XMPError.BADRDF
                    )
            }
        }

        // Create the right kind of child node and visit the attributes again
        // to add the fields or qualifiers.
        // ! Because of implementation vagaries,
        //   the xmpParent is the tree root for top level properties.
        // ! The schema is found, created if necessary, by addChildNode.
        val childNode = addChildNode(xmp, xmpParent, xmlNode, "", isTopLevel)

        var childIsStruct = false

        if (hasValueAttr || hasResourceAttr) {

            val valueNodeValue = when {
                valueNode == null -> null
                valueNode.nodeType == NodeConsts.ATTRIBUTE_NODE -> (valueNode as Attr).value
                else -> throw XMPException("Unknown node type ${xmlNode.nodeType}", XMPError.BADXMP)
            }

            childNode.value = valueNodeValue ?: ""

            // ! Might have both rdf:value and rdf:resource.
            if (!hasValueAttr)
                childNode.options.setURI(true)

        } else if (hasPropertyAttrs) {
            childNode.options.setStruct(true)
            childIsStruct = true
        }

        for (index in 0 until xmlNode.attributes.length) {

            val attribute = xmlNode.attributes.item(index) as Attr

            if (
                attribute === valueNode || "xmlns" == attribute.prefix ||
                attribute.prefix == null && "xmlns" == attribute.nodeName
            )
                continue // Skip the rdf:value or rdf:resource attribute holding the value.

            val attrTerm = getRDFTermKind(attribute)

            when (attrTerm) {

                /* Do nothing with IDs. */
                RDFTERM_ID, RDFTERM_NODE_ID -> continue

                /*
                 * sample_52.xmp has an <rdf:li rdf:about=''/> we want to skip.
                 */
                RDFTERM_ABOUT -> continue

                RDFTERM_RESOURCE ->
                    addQualifierNode(childNode, "rdf:resource", attribute.value)

                RDFTERM_OTHER -> {

                    if (!childIsStruct)
                        addQualifierNode(childNode, attribute.nodeName, attribute.value)
                    else if (XMPConst.XML_LANG == attribute.nodeName)
                        addQualifierNode(childNode, XMPConst.XML_LANG, attribute.value)
                    else
                        addChildNode(xmp, childNode, attribute, attribute.value, false)
                }

                else -> throw XMPException(
                    "Unrecognized attribute of empty property element: $attrTerm",
                    XMPError.BADRDF
                )
            }
        }
    }

    private fun addChildNode(
        xmp: XMPMeta,
        xmpParent: XMPNode,
        xmlNode: Node,
        value: String?,
        isTopLevel: Boolean
    ): XMPNode {

        var namespace = when {
            xmlNode.nodeType == NodeConsts.ELEMENT_NODE -> (xmlNode as Element).namespaceURI
            xmlNode.nodeType == NodeConsts.ATTRIBUTE_NODE -> (xmlNode as Attr).namespaceURI
            else -> throw XMPException("Unknown node type ${xmlNode.nodeType}", XMPError.BADXMP)
        }

        if (namespace.isNullOrEmpty())
            throw XMPException(
                "XML namespace required for all elements and attributes: $xmlNode",
                XMPError.BADRDF
            )

        /* Fix a legacy DC namespace */
        if (XMPConst.NS_DC_DEPRECATED == namespace)
            namespace = XMPConst.NS_DC

        var prefix = XMPSchemaRegistry.getNamespacePrefix(namespace)

        if (prefix == null) {

            val xmlNodePrefix = when {
                xmlNode.nodeType == NodeConsts.ELEMENT_NODE -> (xmlNode as Element).prefix
                xmlNode.nodeType == NodeConsts.ATTRIBUTE_NODE -> (xmlNode as Attr).prefix
                else -> throw XMPException("Unknown node type ${xmlNode.nodeType}", XMPError.BADXMP)
            }

            prefix = if (xmlNodePrefix != null)
                xmlNodePrefix
            else
                DEFAULT_PREFIX

            prefix = XMPSchemaRegistry.registerNamespace(namespace, prefix)
        }

        val xmlNodeLocalName = when {
            xmlNode.nodeType == NodeConsts.ELEMENT_NODE -> (xmlNode as Element).localName
            xmlNode.nodeType == NodeConsts.ATTRIBUTE_NODE -> (xmlNode as Attr).localName
            else -> throw XMPException("Unknown node type ${xmlNode.nodeType}", XMPError.BADXMP)
        }

        val childName = prefix + xmlNodeLocalName

        // create schema node if not already there
        val childOptions = PropertyOptions()

        var isAlias = false

        var actualXmpParent = xmpParent

        if (isTopLevel) {

            // Lookup the schema node, adjust the XMP parent pointer.
            // Incoming parent must be the tree root.
            val schemaNode = XMPNodeUtils.findSchemaNode(
                xmp.root, namespace,
                DEFAULT_PREFIX, true
            )

            checkNotNull(schemaNode) { "SchemaNode should have been created." }

            schemaNode.isImplicit = false // Clear the implicit node bit.

            // *** Should use "opt &= ~flag" (no conditional),
            // need runtime check for proper 32 bit code.
            actualXmpParent = schemaNode

            // If this is an alias set the alias flag in the node
            // and the hasAliases flag in the tree.
            if (XMPSchemaRegistry.findAlias(childName) != null) {
                isAlias = true
                xmp.root.hasAliases = true
                schemaNode.hasAliases = true
            }
        }

        // Make sure that this is not a duplicate of a named node.
        val isArrayItem = isNumberedArrayItemName(childName)
        val isValueNode = "rdf:value" == childName

        // Create XMP node and so some checks
        val newChild = XMPNode(childName, value, childOptions)

        newChild.isAlias = isAlias

        // Add the new child to the XMP parent node, a value node first.
        if (!isValueNode)
            actualXmpParent.addChild(newChild)
        else
            actualXmpParent.addChild(1, newChild)

        if (isValueNode) {

            if (isTopLevel || !actualXmpParent.options.isStruct())
                throw XMPException("Misplaced rdf:value element", XMPError.BADRDF)

            actualXmpParent.hasValueChild = true
        }

        val isParentArray = actualXmpParent.options.isArray()

        when {

            isParentArray && isArrayItem ->
                newChild.name = XMPConst.ARRAY_ITEM_NAME

            !isParentArray && isArrayItem ->
                throw XMPException("Misplaced rdf:li element", XMPError.BADRDF)

            isParentArray && !isArrayItem ->
                throw XMPException("Arrays cannot have arbitrary child names", XMPError.BADRDF)
        }

        return newChild
    }

    private fun addQualifierNode(xmpParent: XMPNode, name: String, value: String): XMPNode {

        val isLang = XMPConst.XML_LANG == name

        // normalize value of language qualifiers
        val normalizedValue = if (isLang)
            Utils.normalizeLangValue(value)
        else
            value

        val newQualifier = XMPNode(name, normalizedValue)

        xmpParent.addQualifier(newQualifier)

        return newQualifier
    }

    /**
     * The parent is an RDF pseudo-struct containing an rdf:value field. Fix the
     * XMP data model. The rdf:value node must be the first child, the other
     * children are qualifiers. The form, value, and children of the rdf:value
     * node are the real ones. The rdf:value node's qualifiers must be added to
     * the others.
     */
    private fun fixupQualifiedNode(xmpParent: XMPNode) {

        require(xmpParent.options.isStruct() && xmpParent.hasChildren())

        val valueNode = xmpParent.getChild(1)

        require("rdf:value" == valueNode.name)

        // Move the qualifiers on the value node to the parent.
        // Make sure an xml:lang qualifier stays at the front.
        // Check for duplicate names between the value node's qualifiers and the parent's children.
        // The parent's children are about to become qualifiers. Check here, between the groups.
        // Intra-group duplicates are caught by XMPNode#addChild(...).

        if (valueNode.options.hasLanguage()) {

            if (xmpParent.options.hasLanguage())
                throw XMPException("Redundant xml:lang for rdf:value element", XMPError.BADXMP)

            val langQual = valueNode.getQualifier(1)

            valueNode.removeQualifier(langQual)

            xmpParent.addQualifier(langQual)
        }

        // Start the remaining copy after the xml:lang qualifier.
        for (index in 1..valueNode.getQualifierLength()) {

            val qualifier = valueNode.getQualifier(index)

            xmpParent.addQualifier(qualifier)
        }

        // Change the parent's other children into qualifiers.
        // This loop starts at 1, child 0 is the rdf:value node.
        for (index in 2..xmpParent.getChildrenLength()) {

            val qualifier = xmpParent.getChild(index)

            xmpParent.addQualifier(qualifier)
        }

        check(xmpParent.options.isStruct() || xmpParent.hasValueChild)

        xmpParent.hasValueChild = false
        xmpParent.options.setStruct(false)
        xmpParent.options.mergeWith(valueNode.options)
        xmpParent.value = valueNode.value
        xmpParent.removeChildren()

        for (child in valueNode.getChildren())
            xmpParent.addChild(child)
    }

    /**
     * Checks if the node is a white space.
     *
     * @param node an XML-node
     * @return Returns whether the node is a whitespace node, i.e. a text node that contains only whitespaces.
     */
    private fun isWhitespaceNode(node: Node): Boolean {

        if (node.nodeType != NodeConsts.TEXT_NODE)
            return false

        val value = (node as Text).data

        for (index in 0 until value.length)
            if (!value[index].isWhitespace())
                return false

        return true
    }

    /**
     * 7.2.6 propertyElementURIs
     * anyURI - ( coreSyntaxTerms | rdf:Description | oldTerms )
     */
    private fun isPropertyElementName(term: Int): Boolean {

        if (term == RDFTERM_DESCRIPTION || isOldTerm(term))
            return false

        return !isCoreSyntaxTerm(term)
    }

    /**
     * 7.2.4 oldTerms<br></br>
     * rdf:aboutEach | rdf:aboutEachPrefix | rdf:bagID
     *
     * @param term the term id
     * @return Returns true if the term is an old term.
     */
    private fun isOldTerm(term: Int): Boolean =
        RDFTERM_FIRST_OLD <= term && term <= RDFTERM_LAST_OLD

    /**
     * 7.2.2 coreSyntaxTerms<br></br>
     * rdf:RDF | rdf:ID | rdf:about | rdf:parseType | rdf:resource | rdf:nodeID |
     * rdf:datatype
     *
     * @param term the term id
     * @return Return true if the term is a core syntax term
     */
    private fun isCoreSyntaxTerm(term: Int): Boolean =
        RDFTERM_FIRST_CORE <= term && term <= RDFTERM_LAST_CORE

    /**
     * Determines the ID for a certain RDF Term.
     * Arranged to hopefully minimize the parse time for large XMP.
     *
     * @param node an XML node
     * @return Returns the term ID.
     */
    private fun getRDFTermKind(node: Node): Int {

        val namespace = when {
            node.nodeType == NodeConsts.ELEMENT_NODE -> (node as Element).namespaceURI
            node.nodeType == NodeConsts.ATTRIBUTE_NODE -> (node as Attr).namespaceURI
            else -> throw XMPException("Unknown Node ${node.nodeType}", XMPError.BADXMP)
        }

        /*
         * This code handles the fact that sometimes "rdf:about" and "rdf:ID"
         * come without the prefix.
         *
         * Note that the check for the namespace must be for NULL or EMPTY, because depending
         * on the used XML parser implementation the resulting namespace may be an empty string.
         */
        @Suppress("ComplexCondition")
        val mustBeRdfNamespace = namespace.isNullOrEmpty() &&
            ("about" == node.nodeName || "ID" == node.nodeName) &&
            node.nodeType == NodeConsts.ATTRIBUTE_NODE &&
            XMPConst.NS_RDF == (node as Attr).ownerElement?.namespaceURI

        if (mustBeRdfNamespace || namespace == XMPConst.NS_RDF) {

            when (node.nodeName) {

                "rdf:li" ->
                    return RDFTERM_LI

                "parseType" ->
                    return RDFTERM_PARSE_TYPE

                "rdf:Description" ->
                    return RDFTERM_DESCRIPTION

                "rdf:about", "about" ->
                    return RDFTERM_ABOUT

                "resource" ->
                    return RDFTERM_RESOURCE

                "rdf:RDF" ->
                    return RDFTERM_RDF

                "rdf:ID", "ID" ->
                    return RDFTERM_ID

                "nodeID" ->
                    return RDFTERM_NODE_ID

                "datatype" ->
                    return RDFTERM_DATATYPE

                "aboutEach" ->
                    return RDFTERM_ABOUT_EACH

                "aboutEachPrefix" ->
                    return RDFTERM_ABOUT_EACH_PREFIX

                "bagID" ->
                    return RDFTERM_BAG_ID
            }
        }

        return RDFTERM_OTHER
    }

    private fun isNumberedArrayItemName(nodeName: String): Boolean {

        var result = "rdf:li" == nodeName

        if (nodeName.startsWith("rdf:_")) {

            result = true

            for (i in 5 until nodeName.length)
                result = result && nodeName[i] >= '0' && nodeName[i] <= '9'
        }

        return result
    }
}
