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
import com.ashampoo.xmp.XMPMeta
import com.ashampoo.xmp.impl.XMPNormalizer.normalize
import com.ashampoo.xmp.options.ParseOptions
import nl.adaptivity.xmlutil.dom.Element
import nl.adaptivity.xmlutil.dom.Node
import nl.adaptivity.xmlutil.dom.ProcessingInstruction
import nl.adaptivity.xmlutil.dom.Text
import nl.adaptivity.xmlutil.dom.childNodes
import nl.adaptivity.xmlutil.dom.getData
import nl.adaptivity.xmlutil.dom.getTarget
import nl.adaptivity.xmlutil.dom.length
import nl.adaptivity.xmlutil.dom.localName
import nl.adaptivity.xmlutil.dom.namespaceURI

/**
 * This class replaces the `ExpatAdapter.cpp` and does the
 * XML-parsing and fixes the prefix. After the parsing several normalisations
 * are applied to the XMPTree.
 */
internal object XMPMetaParser {

    private val XMP_RDF = Any() // "new Object()" in Java

    /**
     * Parses the input source into an XMP metadata object, including
     * de-aliasing and normalisation.
     *
     * @param input   the XMP string
     * @param options the parse options
     * @return Returns the resulting XMP metadata object
     */
    fun parse(
        input: String,
        options: ParseOptions?
    ): XMPMeta {

        val actualOptions = options ?: ParseOptions()

        val document = XmlUtilDomParser.parseDocumentFromString(input)

        val xmpmetaRequired = actualOptions.getRequireXMPMeta()

        var result: Array<Any?>? = arrayOfNulls(3)

        result = findRootNode(document, xmpmetaRequired, result)

        return if (result != null && result[1] === XMP_RDF) {

            val xmp = XMPRDFParser.parse(result[0] as Node, actualOptions)

            xmp.setPacketHeader(result[2] as? String)

            // Check if the XMP object shall be normalized
            if (!actualOptions.getOmitNormalization())
                normalize(xmp, actualOptions)
            else
                xmp

        } else {

            /* No appropriate root node found, return empty metadata object */
            XMPMetaImpl()
        }
    }

    /**
     * Find the XML node that is the root of the XMP data tree. Generally this
     * will be an outer node, but it could be anywhere if a general XML document
     * is parsed (e.g. SVG). The XML parser counted all rdf:RDF and
     * pxmp:XMP_Packet nodes, and kept a pointer to the last one. If there is
     * more than one possible root use PickBestRoot to choose among them.
     *
     * If there is a root node, try to extract the version of the previous XMP
     * toolkit.
     *
     * Pick the first x:xmpmeta among multiple root candidates. If there aren't
     * any, pick the first bare rdf:RDF if that is allowed. The returned root is
     * the rdf:RDF child if an x:xmpmeta element was chosen. The search is
     * breadth first, so a higher level candiate is chosen over a lower level
     * one that was textually earlier in the serialized XML.
     *
     * @param root            the root of the xml document
     * @param xmpmetaRequired flag if the xmpmeta-tag is still required, might be set
     * initially to `true`, if the parse option "REQUIRE_XMP_META" is set
     * @param result          The result array that is filled during the recursive process.
     * @return Returns an array that contains the result or `null`.
     * The array contains:
     *
     *  * [0] - the rdf:RDF-node
     *  * [1] - an object that is either XMP_RDF or XMP_PLAIN (the latter is decrecated)
     *  * [2] - the body text of the xpacket-instruction.
     */
    private fun findRootNode(root: Node, xmpmetaRequired: Boolean, result: Array<Any?>?): Array<Any?>? {

        // Look among this parent's content for x:xapmeta or x:xmpmeta.
        // The recursion for x:xmpmeta is broader than the strictly defined choice,
        // but gives us smaller code.

        for (index in 0 until root.childNodes.length) {

            val child = root.childNodes.item(index)

            requireNotNull(child)

            if (child is ProcessingInstruction && XMPConst.XMP_PI == child.getTarget()) {

                // Store the processing instructions content
                if (result != null)
                    result[2] = child.getData()

            } else if (child !is Text && child !is ProcessingInstruction) {

                val childElement = child as Element

                val rootNS = childElement.namespaceURI

                val rootLocal = childElement.localName

                if (
                    (XMPConst.TAG_XMPMETA == rootLocal || XMPConst.TAG_XAPMETA == rootLocal) &&
                    XMPConst.NS_X == rootNS
                ) {

                    // by not passing the RequireXMPMeta-option, the rdf-Node will be valid
                    return findRootNode(child, false, result)
                }

                if (!xmpmetaRequired && "RDF" == rootLocal && XMPConst.NS_RDF == rootNS) {

                    if (result != null) {
                        result[0] = child
                        result[1] = XMP_RDF
                    }

                    return result
                }

                // continue searching
                val newResult = findRootNode(child, xmpmetaRequired, result)

                return newResult ?: continue
            }
        }

        // no appropriate node has been found
        return null
    }
}
