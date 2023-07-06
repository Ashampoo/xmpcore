package com.ashampoo.xmp.impl

import com.ashampoo.xmp.XMPError
import com.ashampoo.xmp.XMPException
import nl.adaptivity.xmlutil.DomWriter
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.dom.Document
import nl.adaptivity.xmlutil.writeCurrent

object XmlUtilDomParser : DomParser {

    override fun parseDocumentFromString(input: String): Document {

        try {

            val writer = DomWriter()

            val reader = XmlStreaming.newReader(input)

            do {
                val event = reader.next()
                reader.writeCurrent(writer)
            } while (event != EventType.END_DOCUMENT)

            return writer.target

        } catch (ex: Exception) {
            throw XMPException("Error reading the XML-file", XMPError.BADSTREAM, ex)
        }
    }
}
