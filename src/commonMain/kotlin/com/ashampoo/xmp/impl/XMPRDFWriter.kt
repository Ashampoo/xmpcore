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
import com.ashampoo.xmp.XMPMetaFactory.schemaRegistry
import com.ashampoo.xmp.XMPMetaFactory.versionInfo
import com.ashampoo.xmp.impl.Utils.escapeXML
import com.ashampoo.xmp.options.SerializeOptions

/**
 * Serializes the `XMPMeta`-object using the standard RDF serialization format.
 * The output is a XMP String according to the `SerializeOptions`.
 */
@Suppress("TooManyFunctions")
internal class XMPRDFWriter(
    val xmp: XMPMetaImpl,
    val options: SerializeOptions
) {

    private val sb: StringBuilder = StringBuilder()

    /**
     * The actual serialization.
     */
    fun serialize(): String {

        try {

            sb.clear()

            serializeAsRDF()

            return sb.toString()

        } catch (ex: Exception) {
            throw XMPException("Error writing the XMP", XMPError.UNKNOWN, ex)
        }
    }

    /**
     * Writes the (optional) packet header and the outer rdf-tags.
     */
    private fun serializeAsRDF() {

        var level = 0

        // Write the packet header PI.
        if (!options.getOmitPacketWrapper()) {
            writeIndent(level)
            write(PACKET_HEADER)
            writeNewline()
        }

        // Write the x:xmpmeta element's start tag.
        if (!options.getOmitXmpMetaElement()) {

            writeIndent(level)
            write(RDF_XMPMETA_START)
            write(versionInfo.message)
            write("\">")
            writeNewline()

            level++
        }

        // Write the rdf:RDF start tag.
        writeIndent(level)
        write(RDF_RDF_START)
        writeNewline()

        // Write all of the properties.
        if (options.getUseCanonicalFormat())
            serializeCanonicalRDFSchemas(level)
        else
            serializeCompactRDFSchemas(level)

        // Write the rdf:RDF end tag.
        writeIndent(level)
        write(RDF_RDF_END)
        writeNewline()

        // Write the xmpmeta end tag.
        if (!options.getOmitXmpMetaElement()) {

            level--

            writeIndent(level)
            write(RDF_XMPMETA_END)
            writeNewline()
        }

        // Write the packet trailer PI into the tail string as UTF-8.
        var tailStr = ""

        if (!options.getOmitPacketWrapper()) {

            level = 0

            while (level > 0) {
                tailStr += XMP_DEFAULT_INDENT
                level--
            }

            tailStr += PACKET_TRAILER
            tailStr += if (options.getReadOnlyPacket()) 'r' else 'w'
            tailStr += PACKET_TRAILER2
        }

        write(tailStr)
    }

    /**
     * Serializes the metadata in pretty-printed manner.
     *
     * @param level indent level
     */
    private fun serializeCanonicalRDFSchemas(level: Int) {

        if (xmp.root.hasChildren()) {

            startOuterRDFDescription(xmp.root, level)

            for (schema in xmp.root.getChildren())
                serializeCanonicalRDFSchema(schema, level)

            endOuterRDFDescription(level)

        } else {

            writeIndent(level + 1)
            write(RDF_SCHEMA_START) // Special case an empty XMP object.
            writeTreeName()
            write("/>")
            writeNewline()
        }
    }

    private fun writeTreeName() {

        write('"')

        val name = xmp.root.name

        if (name != null)
            appendNodeValue(name, true)

        write('"')
    }

    /**
     * Serializes the metadata in compact manner.
     *
     * @param level indent level to start with
     */
    private fun serializeCompactRDFSchemas(level: Int) {

        // Begin the rdf:Description start tag.
        writeIndent(level + 1)
        write(RDF_SCHEMA_START)
        writeTreeName()

        // Write all necessary xmlns attributes.
        val usedPrefixes: MutableSet<String> = mutableSetOf()
        usedPrefixes.add("xml")
        usedPrefixes.add("rdf")

        for (schema in xmp.root.getChildren())
            declareUsedNamespaces(schema, usedPrefixes, level + 3)

        // Write the top level "attrProps" and close the rdf:Description start tag.
        var allAreAttrs = true

        for (schema in xmp.root.getChildren())
            allAreAttrs = allAreAttrs and serializeCompactRDFAttrProps(schema, level + 2)

        if (!allAreAttrs) {

            write('>')
            writeNewline()

        } else {

            write("/>")
            writeNewline()
            return // ! Done if all properties in all schema are written as attributes.
        }

        // Write the remaining properties for each schema.
        for (schema in xmp.root.getChildren())
            serializeCompactRDFElementProps(schema, level + 2)

        // Write the rdf:Description end tag.
        // *** Elide the end tag if everything (all props in all schema) is an attr.
        writeIndent(level + 1)
        write(RDF_SCHEMA_END)
        writeNewline()
    }

    /**
     * Write each of the parent's simple unqualified properties as an attribute. Returns true if all
     * of the properties are written as attributes.
     *
     * @param parentNode the parent property node
     * @param indent     the current indent level
     * @return Returns true if all properties can be rendered as RDF attribute.
     */
    private fun serializeCompactRDFAttrProps(parentNode: XMPNode, indent: Int): Boolean {

        var allAreAttrs = true

        for (prop in parentNode.getChildren()) {

            if (canBeRDFAttrProp(prop)) {

                writeNewline()
                writeIndent(indent)
                write(prop.name!!)
                write("=\"")
                appendNodeValue(prop.value, true)
                write('"')

            } else {

                allAreAttrs = false
            }
        }

        return allAreAttrs
    }

    /**
     * Recursively handles the "value" for a node that must be written as an RDF
     * property element. It does not matter if it is a top level property, a
     * field of a struct, or an item of an array. The indent is that for the
     * property element. The patterns bwlow ignore attribute qualifiers such as
     * xml:lang, they don't affect the output form.
     *
     * @param parentNode the parent node
     * @param indent     the current indent level
     */
    private fun serializeCompactRDFElementProps(parentNode: XMPNode, indent: Int) {

        for (node in parentNode.getChildren()) {

            if (canBeRDFAttrProp(node))
                continue

            var emitEndTag = true
            var indentEndTag = true

            // Determine the XML element name, write the name part of the start tag. Look over the
            // qualifiers to decide on "normal" versus "rdf:value" form. Emit the attribute
            // qualifiers at the same time.
            var elemName = node.name

            if (XMPConst.ARRAY_ITEM_NAME == elemName)
                elemName = "rdf:li"

            writeIndent(indent)
            write('<')
            write(elemName!!)

            var hasGeneralQualifiers = false
            var hasRDFResourceQual = false

            for (qualifier in node.getQualifier()) {

                if (!RDF_ATTR_QUALIFIER.contains(qualifier.name)) {

                    hasGeneralQualifiers = true

                } else {

                    hasRDFResourceQual = "rdf:resource" == qualifier.name
                    write(' ')
                    write(qualifier.name!!)
                    write("=\"")
                    appendNodeValue(qualifier.value, true)
                    write('"')
                }
            }

            // Process the property according to the standard patterns.
            if (hasGeneralQualifiers) {

                serializeCompactRDFGeneralQualifier(indent, node)

            } else {

                // This node has only attribute qualifiers. Emit as a property element.
                if (!node.options.isCompositeProperty()) {

                    val result = serializeCompactRDFSimpleProp(node)

                    emitEndTag = result[0] as Boolean
                    indentEndTag = result[1] as Boolean

                } else if (node.options.isArray()) {

                    serializeCompactRDFArrayProp(node, indent)

                } else {

                    emitEndTag = serializeCompactRDFStructProp(node, indent, hasRDFResourceQual)
                }
            }

            // Emit the property element end tag.
            if (emitEndTag) {

                if (indentEndTag)
                    writeIndent(indent)

                write("</")
                write(elemName)
                write('>')
                writeNewline()
            }
        }
    }

    /**
     * Serializes a simple property.
     *
     * @param node an XMPNode
     * @return Returns an array containing the flags emitEndTag and indentEndTag.
     */
    private fun serializeCompactRDFSimpleProp(node: XMPNode): Array<Any> {

        // This is a simple property.
        var emitEndTag = true
        var indentEndTag = true

        if (node.options.isURI()) {

            write(" rdf:resource=\"")
            appendNodeValue(node.value, true)
            write("\"/>")
            writeNewline()
            emitEndTag = false

        } else if (node.value == null || node.value?.length == 0) {

            write("/>")
            writeNewline()
            emitEndTag = false

        } else {

            write('>')
            appendNodeValue(node.value, false)
            indentEndTag = false
        }

        return arrayOf(emitEndTag, indentEndTag)
    }

    /**
     * Serializes an array property.
     *
     * @param node   an XMPNode
     * @param indent the current indent level
     */
    private fun serializeCompactRDFArrayProp(node: XMPNode, indent: Int) {

        // This is an array.
        write('>')
        writeNewline()
        emitRDFArrayTag(node, true, indent + 1)

        if (node.options.isArrayAltText())
            XMPNodeUtils.normalizeLangArray(node)

        serializeCompactRDFElementProps(node, indent + 2)
        emitRDFArrayTag(node, false, indent + 1)
    }

    /**
     * Serializes a struct property.
     *
     * @param node               an XMPNode
     * @param indent             the current indent level
     * @param hasRDFResourceQual Flag if the element has resource qualifier
     * @return Returns true if an end flag shall be emitted.
     */
    private fun serializeCompactRDFStructProp(
        node: XMPNode,
        indent: Int,
        hasRDFResourceQual: Boolean
    ): Boolean {

        // This must be a struct.
        var hasAttrFields = false
        var hasElemFields = false
        var emitEndTag = true

        for (field in node.getChildren()) {

            if (canBeRDFAttrProp(field))
                hasAttrFields = true
            else
                hasElemFields = true

            if (hasAttrFields && hasElemFields)
                break // No sense looking further.
        }

        if (hasRDFResourceQual && hasElemFields)
            throw XMPException("Can't mix rdf:resource qualifier and element fields", XMPError.BADRDF)

        when {

            !node.hasChildren() -> {

                // Catch an empty struct as a special case. The case
                // below would emit an empty
                // XML element, which gets reparsed as a simple property
                // with an empty value.
                write(" rdf:parseType=\"Resource\"/>")
                writeNewline()
                emitEndTag = false
            }

            !hasElemFields -> {

                // All fields can be attributes, use the
                // emptyPropertyElt form.
                serializeCompactRDFAttrProps(node, indent + 1)
                write("/>")
                writeNewline()
                emitEndTag = false
            }

            !hasAttrFields -> {

                // All fields must be elements, use the
                // parseTypeResourcePropertyElt form.
                write(" rdf:parseType=\"Resource\">")
                writeNewline()
                serializeCompactRDFElementProps(node, indent + 1)
            }

            else -> {

                // Have a mix of attributes and elements, use an inner rdf:Description.
                write('>')
                writeNewline()
                writeIndent(indent + 1)
                write(RDF_STRUCT_START)
                serializeCompactRDFAttrProps(node, indent + 2)
                write(">")
                writeNewline()
                serializeCompactRDFElementProps(node, indent + 1)
                writeIndent(indent + 1)
                write(RDF_STRUCT_END)
                writeNewline()
            }
        }

        return emitEndTag
    }

    /**
     * Serializes the general qualifier.
     *
     * @param indent the current indent level
     * @param node   the root node of the subtree
     */
    private fun serializeCompactRDFGeneralQualifier(indent: Int, node: XMPNode) {

        // The node has general qualifiers, ones that can't be
        // attributes on a property element.
        // Emit using the qualified property pseudo-struct form. The
        // value is output by a call
        // to SerializePrettyRDFProperty with emitAsRDFValue set.

        // *** We're losing compactness in the calls to SerializePrettyRDFProperty.
        // *** Should refactor to have SerializeCompactRDFProperty that does one node.
        write(" rdf:parseType=\"Resource\">")
        writeNewline()
        serializeCanonicalRDFProperty(node, false, true, indent + 1)

        for (qualifier in node.getQualifier())
            serializeCanonicalRDFProperty(qualifier, false, false, indent + 1)
    }

    /**
     * Serializes one schema with all contained properties in pretty-printed
     * manner.
     *
     * Each schema's properties are written to a single
     * rdf:Description element. All of the necessary namespaces are declared in
     * the rdf:Description element. The baseIndent is the base level for the
     * entire serialization, that of the x:xmpmeta element. An xml:lang
     * qualifier is written as an attribute of the property start tag, not by
     * itself forcing the qualified property form.
     */
    private fun serializeCanonicalRDFSchema(schemaNode: XMPNode, level: Int) {

        // Write each of the schema's actual properties.

        for (propNode in schemaNode.getChildren())
            serializeCanonicalRDFProperty(propNode, options.getUseCanonicalFormat(), false, level + 2)
    }

    /**
     * Writes all used namespaces of the subtree in node to the output.
     * The subtree is recursivly traversed.
     */
    private fun declareUsedNamespaces(node: XMPNode, usedPrefixes: MutableSet<String>, indent: Int) {

        if (node.options.isSchemaNode()) {

            // The schema node name is the URI, the value is the prefix.
            val prefix = node.value!!.substring(0, node.value!!.length - 1)
            declareNamespace(prefix, node.name, usedPrefixes, indent)

        } else if (node.options.isStruct()) {

            for (field in node.getChildren())
                declareNamespace(field.name!!, null, usedPrefixes, indent)
        }

        for (child in node.getChildren())
            declareUsedNamespaces(child, usedPrefixes, indent)

        for (qualifier in node.getQualifier()) {

            declareNamespace(qualifier.name!!, null, usedPrefixes, indent)
            declareUsedNamespaces(qualifier, usedPrefixes, indent)
        }
    }

    /**
     * Writes one namespace declaration to the output.
     *
     * @param prefix       a namespace prefix (without colon) or a complete qname (when namespace == null)
     * @param namespace    the a namespace
     * @param usedPrefixes a set containing currently used prefixes
     * @param indent       the current indent level
     */
    private fun declareNamespace(
        prefix: String,
        namespace: String?,
        usedPrefixes: MutableSet<String>,
        indent: Int
    ) {

        var prefix = prefix
        var namespace = namespace

        if (namespace == null) {

            // prefix contains qname, extract prefix and lookup namespace with prefix
            val qname = QName(prefix)

            if (!qname.hasPrefix())
                return

            prefix = qname.prefix!!

            // add colon for lookup
            namespace = schemaRegistry.getNamespaceURI("$prefix:")

            // prefix w/o colon
            declareNamespace(prefix, namespace, usedPrefixes, indent)
        }

        if (!usedPrefixes.contains(prefix)) {

            writeNewline()
            writeIndent(indent)
            write("xmlns:")
            write(prefix)
            write("=\"")
            write(namespace!!)
            write('"')

            usedPrefixes.add(prefix)
        }
    }

    /**
     * Start the outer rdf:Description element, including all needed xmlns attributes.
     * Leave the element open so that the compact form can add property attributes.
     */
    private fun startOuterRDFDescription(schemaNode: XMPNode, level: Int) {

        writeIndent(level + 1)
        write(RDF_SCHEMA_START)
        writeTreeName()

        val usedPrefixes: MutableSet<String> = mutableSetOf()
        usedPrefixes.add("xml")
        usedPrefixes.add("rdf")

        declareUsedNamespaces(schemaNode, usedPrefixes, level + 3)

        write('>')
        writeNewline()
    }

    /**
     * Write the  end tag.
     */
    private fun endOuterRDFDescription(level: Int) {

        writeIndent(level + 1)
        write(RDF_SCHEMA_END)
        writeNewline()
    }

    /**
     * Recursively handles the "value" for a node. It does not matter if it is a
     * top level property, a field of a struct, or an item of an array. The
     * indent is that for the property element. An xml:lang qualifier is written
     * as an attribute of the property start tag, not by itself forcing the
     * qualified property form. The patterns below mostly ignore attribute
     * qualifiers like xml:lang. Except for the one struct case, attribute
     * qualifiers don't affect the output form.
     *
     * @param node            the property node
     * @param emitAsRDFValue  property shall be rendered as attribute rather than tag
     * @param useCanonicalRDF use canonical form with inner description tag or
     * the compact form with rdf:ParseType=&quot;resource&quot; attribute.
     * @param indent          the current indent level
     */
    private fun serializeCanonicalRDFProperty(
        node: XMPNode,
        useCanonicalRDF: Boolean,
        emitAsRDFValue: Boolean,
        indent: Int
    ) {

        var indent = indent
        var emitEndTag = true
        var indentEndTag = true

        // Determine the XML element name. Open the start tag with the name and
        // attribute qualifiers.
        var elemName = node.name

        if (emitAsRDFValue)
            elemName = "rdf:value"
        else if (XMPConst.ARRAY_ITEM_NAME == elemName)
            elemName = "rdf:li"

        writeIndent(indent)
        write('<')
        write(elemName!!)

        var hasGeneralQualifiers = false
        var hasRDFResourceQual = false

        val it = node.iterateQualifier()

        while (it.hasNext()) {

            val qualifier = it.next()

            if (!RDF_ATTR_QUALIFIER.contains(qualifier.name)) {

                hasGeneralQualifiers = true

            } else {

                hasRDFResourceQual = "rdf:resource" == qualifier.name

                if (!emitAsRDFValue) {

                    write(' ')
                    write(qualifier.name!!)
                    write("=\"")
                    appendNodeValue(qualifier.value, true)
                    write('"')
                }
            }
        }

        // Process the property according to the standard patterns.
        if (hasGeneralQualifiers && !emitAsRDFValue) {

            // This node has general, non-attribute, qualifiers. Emit using the
            // qualified property form.
            // ! The value is output by a recursive call ON THE SAME NODE with
            // emitAsRDFValue set.
            if (hasRDFResourceQual)
                throw XMPException("Can't mix rdf:resource and general qualifiers", XMPError.BADRDF)

            // Change serialization to canonical format with inner rdf:Description-tag
            // depending on option
            if (useCanonicalRDF) {

                write(">")
                writeNewline()
                indent++
                writeIndent(indent)
                write(RDF_STRUCT_START)
                write(">")

            } else {
                write(" rdf:parseType=\"Resource\">")
            }

            writeNewline()

            serializeCanonicalRDFProperty(node, useCanonicalRDF, true, indent + 1)

            for (qualifier in node.getQualifier())
                if (!RDF_ATTR_QUALIFIER.contains(qualifier.name))
                    serializeCanonicalRDFProperty(qualifier, useCanonicalRDF, false, indent + 1)

            if (useCanonicalRDF) {

                writeIndent(indent)
                write(RDF_STRUCT_END)
                writeNewline()
                indent--
            }

        } else {

            // This node has no general qualifiers. Emit using an unqualified form.
            when {

                !node.options.isCompositeProperty() -> {

                    // This is a simple property.
                    if (node.options.isURI()) {

                        write(" rdf:resource=\"")
                        appendNodeValue(node.value, true)
                        write("\"/>")
                        writeNewline()

                        emitEndTag = false

                    } else if (node.value == null || "" == node.value) {

                        write("/>")
                        writeNewline()

                        emitEndTag = false

                    } else {

                        write('>')
                        appendNodeValue(node.value, false)

                        indentEndTag = false
                    }

                }

                node.options.isArray() -> {

                    // This is an array.
                    write('>')
                    writeNewline()
                    emitRDFArrayTag(node, true, indent + 1)

                    if (node.options.isArrayAltText())
                        XMPNodeUtils.normalizeLangArray(node)

                    for (child in node.getChildren())
                        serializeCanonicalRDFProperty(child, useCanonicalRDF, false, indent + 2)

                    emitRDFArrayTag(node, false, indent + 1)
                }

                !hasRDFResourceQual -> {

                    // This is a "normal" struct, use the rdf:parseType="Resource" form.
                    if (!node.hasChildren()) {

                        // Change serialization to canonical format with inner rdf:Description-tag
                        // if option is set
                        if (useCanonicalRDF) {

                            write(">")
                            writeNewline()
                            writeIndent(indent + 1)
                            write(RDF_EMPTY_STRUCT)

                        } else {

                            write(" rdf:parseType=\"Resource\"/>")

                            emitEndTag = false
                        }

                        writeNewline()

                    } else {

                        // Change serialization to canonical format with inner rdf:Description-tag
                        // if option is set
                        if (useCanonicalRDF) {

                            write(">")
                            writeNewline()
                            indent++
                            writeIndent(indent)
                            write(RDF_STRUCT_START)
                            write(">")

                        } else {

                            write(" rdf:parseType=\"Resource\">")
                        }

                        writeNewline()

                        for (child in node.getChildren())
                            serializeCanonicalRDFProperty(child, useCanonicalRDF, false, indent + 1)

                        if (useCanonicalRDF) {
                            writeIndent(indent)
                            write(RDF_STRUCT_END)
                            writeNewline()
                            indent--
                        }
                    }

                }

                else -> {

                    // This is a struct with an rdf:resource attribute, use the "empty property element" form.

                    for (child in node.getChildren()) {

                        if (!canBeRDFAttrProp(child))
                            throw XMPException("Can't mix rdf:resource and complex fields", XMPError.BADRDF)

                        writeNewline()
                        writeIndent(indent + 1)
                        write(' ')
                        write(child.name!!)
                        write("=\"")
                        appendNodeValue(child.value, true)
                        write('"')
                    }

                    write("/>")
                    writeNewline()

                    emitEndTag = false
                }
            }
        }

        // Emit the property element end tag.
        if (emitEndTag) {

            if (indentEndTag)
                writeIndent(indent)

            write("</")
            write(elemName)
            write('>')
            writeNewline()
        }
    }

    /**
     * Writes the array start and end tags.
     *
     * @param arrayNode  an array node
     * @param isStartTag flag if its the start or end tag
     * @param indent     the current indent level
     */
    private fun emitRDFArrayTag(arrayNode: XMPNode, isStartTag: Boolean, indent: Int) {

        if (isStartTag || arrayNode.hasChildren()) {

            writeIndent(indent)

            write(if (isStartTag) "<rdf:" else "</rdf:")

            if (arrayNode.options.isArrayAlternate())
                write("Alt")
            else if (arrayNode.options.isArrayOrdered())
                write("Seq")
            else
                write("Bag")

            if (isStartTag && !arrayNode.hasChildren())
                write("/>")
            else
                write(">")

            writeNewline()
        }
    }

    /**
     * Serializes the node value in XML encoding. Its used for tag bodies and
     * attributes. *Note:* The attribute is always limited by quotes,
     * thats why `&apos;` is never serialized. *Note:*
     * Control chars are written unescaped, but if the user uses others than tab, LF
     * and CR the resulting XML will become invalid.
     *
     * @param value        the value of the node
     * @param forAttribute flag if value is an attribute value
     *
     */
    private fun appendNodeValue(value: String?, forAttribute: Boolean) =
        write(escapeXML(value ?: "", forAttribute, true))

    /**
     * A node can be serialized as RDF-Attribute, if it meets the following conditions:
     *
     *  * is not array item
     *  * don't has qualifier
     *  * is no URI
     *  * is no composite property
     *
     * @param node an XMPNode
     * @return Returns true if the node serialized as RDF-Attribute
     */
    private fun canBeRDFAttrProp(node: XMPNode): Boolean =
        !node.hasQualifier() && !node.options.isURI() && !node.options.isCompositeProperty() &&
            XMPConst.ARRAY_ITEM_NAME != node.name

    private fun writeIndent(times: Int) =
        repeat(times) { sb.append(XMP_DEFAULT_INDENT) }

    private fun write(c: Char) =
        sb.append(c)

    private fun write(str: String) =
        sb.append(str)

    /**
     * Writes a newline.
     */
    private fun writeNewline() {
        sb.append(XMP_DEFAULT_NEWLINE)
    }

    companion object {

        /** linefeed (U+000A) is the standard XML line terminator. XMP defaults to it. */
        const val XMP_DEFAULT_NEWLINE = "\n"

        /** Two ASCII spaces (U+0020) are the default indent for XMP files. */
        const val XMP_DEFAULT_INDENT = "  "

        private const val PACKET_HEADER = "<?xpacket begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>"

        /**
         * The w/r is missing inbetween
         */
        private const val PACKET_TRAILER = "<?xpacket end=\""

        private const val PACKET_TRAILER2 = "\"?>"

        private const val RDF_XMPMETA_START = "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\""

        private const val RDF_XMPMETA_END = "</x:xmpmeta>"

        private const val RDF_RDF_START =
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"

        private const val RDF_RDF_END = "</rdf:RDF>"

        private const val RDF_SCHEMA_START = "<rdf:Description rdf:about="

        private const val RDF_SCHEMA_END = "</rdf:Description>"

        private const val RDF_STRUCT_START = "<rdf:Description"

        private const val RDF_STRUCT_END = "</rdf:Description>"

        private const val RDF_EMPTY_STRUCT = "<rdf:Description/>"

        /**
         * a set of all rdf attribute qualifier
         */
        val RDF_ATTR_QUALIFIER: Set<String> = setOf(
            XMPConst.XML_LANG, "rdf:resource", "rdf:ID", "rdf:bagID", "rdf:nodeID"
        )
    }
}
