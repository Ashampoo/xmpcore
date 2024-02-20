package com.ashampoo.xmp

import nl.adaptivity.xmlutil.DomWriter
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.dom.Document
import nl.adaptivity.xmlutil.writeCurrent
import nl.adaptivity.xmlutil.xmlStreaming

object DomParser {

    fun parseDocumentFromString(input: String): Document {

        /*
         * We encountered a situation where the XMP had NUL characters at the end
         * for unknown reasons. This  caused an exception in the parser.
         * The test images IMG_0001.jpg and IMG_0002.jpg on the iOS simulator
         * exhibited this issue, indicating that it could occur in real-world
         * scenarios as well. To address this, we now trim all whitespaces and
         * ISO control characters from the XMP to ensure its proper parsing.
         */
        val trimmedInput = input.trim {
            it.isWhitespace() || it.isISOControl()
        }

        try {

            val writer = DomWriter()

            val reader = xmlStreaming.newReader(trimmedInput)

            do {
                val event = reader.next()
                reader.writeCurrent(writer)
            } while (event != EventType.END_DOCUMENT)

            return writer.target

        } catch (ex: Exception) {
            throw XMPException("Error reading the XML file: ${ex.message}", XMPError.BADSTREAM, ex)
        }
    }
}
