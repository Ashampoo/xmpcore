package com.ashampoo.xmp

import com.ashampoo.xmp.XMPConst.XMP_DC_SUBJECT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

        assertNull(xmpMeta.getLocation())
    }

    @Test
    fun testReadXmpWithIptcLocation() {

        /* language=XML */
        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="XMP Core 6.0.0">
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about=""
                        xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                        xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/"
                        xmlns:Iptc4xmpCore="http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/"
                        xmlns:Iptc4xmpExt="http://iptc.org/std/Iptc4xmpExt/2008-02-29/"
                        xmlns:exif="http://ns.adobe.com/exif/1.0/"
                        xmlns:dc="http://purl.org/dc/elements/1.1/"
                        photoshop:Country="Deutschland"
                        photoshop:State="Niedersachsen"
                        photoshop:City="Rastede"
                        Iptc4xmpCore:Location="Schafjückenweg 2">
                        <Iptc4xmpExt:LocationShown>
                            <rdf:Bag>
                                <rdf:li>
                                    <rdf:Description
                                        Iptc4xmpExt:CountryName="Deutschland"
                                        Iptc4xmpExt:ProvinceState="Niedersachsen"
                                        Iptc4xmpExt:City="Rastede"
                                        Iptc4xmpExt:Sublocation="Schafjückenweg 2">
                                        <Iptc4xmpExt:LocationName>
                                            <rdf:Alt>
                                                <rdf:li xml:lang="x-default">Ashampoo GmbH &amp; Co. KG</rdf:li>
                                            </rdf:Alt>
                                        </Iptc4xmpExt:LocationName>
                                    </rdf:Description>
                                </rdf:li>
                            </rdf:Bag>
                        </Iptc4xmpExt:LocationShown>
                    </rdf:Description>
                </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = XMPLocation(
                name = "Ashampoo GmbH & Co. KG",
                location = "Schafjückenweg 2",
                city = "Rastede",
                state = "Niedersachsen",
                country = "Deutschland"
            ),
            actual = xmpMeta.getLocation()
        )
    }

    @Test
    fun testReadXmpWithPhotoshopLocation() {

        /* language=XML */
        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="XMP Core 6.0.0">
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about=""
                        xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                        xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/"
                        xmlns:Iptc4xmpCore="http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/"
                        xmlns:exif="http://ns.adobe.com/exif/1.0/"
                        xmlns:dc="http://purl.org/dc/elements/1.1/"
                        photoshop:Country="Deutschland"
                        photoshop:State="Niedersachsen"
                        photoshop:City="Rastede"
                        Iptc4xmpCore:Location="Schafjückenweg 2">
                    </rdf:Description>
                </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = XMPLocation(
                name = null,
                location = "Schafjückenweg 2",
                city = "Rastede",
                state = "Niedersachsen",
                country = "Deutschland"
            ),
            actual = xmpMeta.getLocation()
        )
    }

    @Test
    fun readXmpWithTitleAndDescription() {

        /* language=XML */
        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Ashampoo XMP Core 1.5.0">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <dc:description>
                    <rdf:Alt>
                      <rdf:li xml:lang="x-default">Vögel auf dem Wasser.</rdf:li>
                    </rdf:Alt>
                  </dc:description>
                  <dc:title>
                    <rdf:Alt>
                      <rdf:li xml:lang="x-default">Süße Vögelchen</rdf:li>
                    </rdf:Alt>
                  </dc:title>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        println(testXmp)

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Süße Vögelchen",
            actual = xmpMeta.getTitle()
        )

        assertEquals(
            expected = "Vögel auf dem Wasser.",
            actual = xmpMeta.getDescription()
        )
    }
}
