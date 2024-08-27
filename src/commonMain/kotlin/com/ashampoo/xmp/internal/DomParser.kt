package com.ashampoo.xmp.internal

import com.ashampoo.xmp.XMPException
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.writeCurrent
import nl.adaptivity.xmlutil.xmlStreaming

internal object DomParser {

    private const val RDF_RDF_END = "</rdf:RDF>"

    @OptIn(XmlUtilInternal::class, ExperimentalXmlUtilApi::class)
    fun parseDocumentFromString(input: String): Document {

        /*
         * We encountered a situation where NUL characters at the end of XMP
         * caused an exception in the parser for unknown reasons. This issue was
         * observed in the test images IMG_0001.jpg and IMG_0002.jpg on the iOS simulator,
         * suggesting the possibility of real-world scenarios.
         *
         * Additionally, we identified some corrupted files with random junk after the
         * first </rdf:RDF>, potentially caused by faulty writers.
         *
         * Since "xpacket" and "xmpmeta" are skippable at the user's discretion,
         * the only required tags are within the RDF part. Therefore, we trim it down to this.
         */

        val rdfStartPos = input.indexOf("<rdf:RDF")
        val rfdEndPos = input.indexOf(RDF_RDF_END)

        val trimmedInput = input.substring(
            rdfStartPos until rfdEndPos + RDF_RDF_END.length
        )

        try {

            val document = xmlStreaming.genericDomImplementation.createDocument()

            val writer = xmlStreaming.newWriter(document)

            val reader = xmlStreaming.newReader(trimmedInput)

            do {
                val event = reader.next()
                reader.writeCurrent(writer)
            } while (event != EventType.END_DOCUMENT)

            return document

        } catch (ex: Exception) {
            throw XMPException("Error reading the XML file: ${ex.message}", XMPErrorConst.BADSTREAM, ex)
        }
    }
}
