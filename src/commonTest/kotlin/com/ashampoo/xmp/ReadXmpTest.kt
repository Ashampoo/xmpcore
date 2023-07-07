package com.ashampoo.xmp

import com.ashampoo.xmp.XMPConst.XMP_DC_SUBJECT
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Demonstrates how to use the library to read values.
 */
class ReadXmpTest {

    @Test
    fun readXmp() {

        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:exif="http://ns.adobe.com/exif/1.0/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                exif:DateTimeOriginal="1980-03-15T08:15:30"
                exif:GPSLatitude="53,13.1635N"
                exif:GPSLongitude="8,14.3797E"
                exif:GPSVersionID="2.3.0.0"
                xmp:Rating="2">
                <dc:subject>
                  <rdf:Bag>
                    <rdf:li>fox</rdf:li>
                    <rdf:li>swiper</rdf:li>
                  </rdf:Bag>
                </dc:subject>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "1980-03-15T08:15:30",
            actual = xmpMeta.getPropertyString(XMPConst.NS_EXIF, "DateTimeOriginal")
        )

        assertEquals(
            expected = "53,13.1635N",
            actual = xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSLatitude")
        )

        assertEquals(
            expected = "8,14.3797E",
            actual = xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSLongitude")
        )

        assertEquals(
            expected = "2.3.0.0",
            actual = xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSVersionID")
        )

        assertEquals(
            expected = "2",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP, "Rating")
        )

        assertEquals(
            expected = 2,
            actual = xmpMeta.countArrayItems(XMPConst.NS_DC, XMP_DC_SUBJECT)
        )

        assertEquals(
            expected = "fox",
            actual = xmpMeta.getPropertyString(XMPConst.NS_DC, "$XMP_DC_SUBJECT[1]")
        )

        assertEquals(
            expected = "swiper",
            actual = xmpMeta.getPropertyString(XMPConst.NS_DC, "$XMP_DC_SUBJECT[2]")
        )
    }
}
