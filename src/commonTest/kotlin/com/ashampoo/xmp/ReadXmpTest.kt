package com.ashampoo.xmp

import com.ashampoo.xmp.XMPConst.XMP_DC_SUBJECT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Demonstrates how to use the library to read values.
 */
class ReadXmpTest {

    @Test
    fun testReadXmp() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:ashampoo="http://ns.ashampoo.com/xmp/1.0/"
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:exif="http://ns.adobe.com/exif/1.0/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:xmpDM="http://ns.adobe.com/xmp/1.0/DynamicMedia/"
                exif:DateTimeOriginal="1980-03-15T08:15:30"
                exif:GPSLatitude="53,13.1635N"
                exif:GPSLongitude="8,14.3797E"
                exif:GPSVersionID="2.3.0.0"
                xmpDM:pick="1"
                xmp:Rating="2">
                <ashampoo:albums>
                  <rdf:Bag>
                    <rdf:li>America trip</rdf:li>
                    <rdf:li>My wedding</rdf:li>
                  </rdf:Bag>
                </ashampoo:albums>
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
            actual = xmpMeta.getDateTimeOriginal()
        )

        assertEquals(
            expected = "53,13.1635N",
            actual = xmpMeta.getGpsLatitude()
        )

        assertEquals(
            expected = "8,14.3797E",
            actual = xmpMeta.getGpsLongitude()
        )

        assertEquals(
            expected = "2.3.0.0",
            actual = xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSVersionID")
        )

        assertEquals(
            expected = 2,
            actual = xmpMeta.getRating()
        )

        assertTrue(xmpMeta.isFlagged())

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

        assertEquals(
            expected = setOf("fox", "swiper"),
            actual = xmpMeta.getKeywords()
        )

        assertEquals(
            expected = setOf("My wedding", "America trip"),
            actual = xmpMeta.getAlbums()
        )
    }
}
