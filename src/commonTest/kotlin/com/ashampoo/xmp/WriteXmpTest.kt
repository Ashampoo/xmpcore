package com.ashampoo.xmp

import com.ashampoo.xmp.options.SerializeOptions
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Demonstrates how to use the library to write values.
 */
class WriteXmpTest {

    private val xmpSerializeOptionsCompact =
        SerializeOptions()
            .setOmitXmpMetaElement(false)
            .setOmitPacketWrapper(false)
            .setUseCompactFormat(true)
            .setUseCanonicalFormat(false)
            .setSort(true)

    /**
     * Create an empty XMP file with only the required envelope.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCreateEmptyXmp() {

        val xmpMeta = XMPMetaFactory.create()

        val actualXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

        /* language=XML */
        val expectedXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="${XMPVersionInfo.VERSION_MESSAGE}">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""/>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        assertEquals(
            expected = expectedXmp,
            actual = actualXmp
        )
    }

    /**
     * Create an XMP only containing a rating.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCreateRatingXmp() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setRating(3)

        val actualXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

        /* language=XML */
        val expectedXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="${XMPVersionInfo.VERSION_MESSAGE}">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmp:Rating="3"/>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        assertEquals(
            expected = expectedXmp,
            actual = actualXmp
        )
    }

    /**
     * Create an XMP containing multiple values.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCreateNewXmp() {

        val xmpMeta = XMPMetaFactory.create()

        writeTestValues(xmpMeta)

        val actualXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

        /* language=XML */
        val expectedXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="${XMPVersionInfo.VERSION_MESSAGE}">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:MY="http://ns.mylollc.com/MyloEdit/"
                    xmlns:acdsee="http://ns.acdsee.com/iptc/1.0/"
                    xmlns:ashampoo="http://ns.ashampoo.com/xmp/1.0/"
                    xmlns:dc="http://purl.org/dc/elements/1.1/"
                    xmlns:exif="http://ns.adobe.com/exif/1.0/"
                    xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/"
                    xmlns:stDim="http://ns.adobe.com/xap/1.0/sType/Dimensions#"
                    xmlns:stArea="http://ns.adobe.com/xmp/sType/Area#"
                    xmlns:narrative="http://ns.narrative.so/narrative_select/1.0/"
                    xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                    xmlns:xmpDM="http://ns.adobe.com/xmp/1.0/DynamicMedia/"
                  MY:flag="true"
                  acdsee:tagged="True"
                  exif:DateTimeOriginal="2023-07-07T13:37:42"
                  exif:GPSLatitude="53,13.1635N"
                  exif:GPSLongitude="8,14.3797E"
                  exif:GPSVersionID="2.3.0.0"
                  narrative:Tagged="True"
                  xmp:Rating="3"
                  xmpDM:pick="1">
                  <ashampoo:albums>
                    <rdf:Bag>
                      <rdf:li>America trip</rdf:li>
                      <rdf:li>My wedding</rdf:li>
                    </rdf:Bag>
                  </ashampoo:albums>
                  <dc:subject>
                    <rdf:Bag>
                      <rdf:li>bird</rdf:li>
                      <rdf:li>cat</rdf:li>
                      <rdf:li>dog</rdf:li>
                    </rdf:Bag>
                  </dc:subject>
                  <mwg-rs:Regions rdf:parseType="Resource">
                    <mwg-rs:AppliedToDimensions
                      stDim:h="1000"
                      stDim:unit="pixel"
                      stDim:w="1500"/>
                    <mwg-rs:RegionList>
                      <rdf:Bag>
                        <rdf:li>
                          <rdf:Description
                            mwg-rs:Name="Eye Left"
                            mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.05"
                            stArea:unit="normalized"
                            stArea:w="0.033245"
                            stArea:x="0.295179"
                            stArea:y="0.27888"/>
                          </rdf:Description>
                        </rdf:li>
                        <rdf:li>
                          <rdf:Description
                            mwg-rs:Name="Eye Right"
                            mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.05"
                            stArea:unit="normalized"
                            stArea:w="0.033245"
                            stArea:x="0.81499"
                            stArea:y="0.472579"/>
                          </rdf:Description>
                        </rdf:li>
                        <rdf:li>
                          <rdf:Description
                            mwg-rs:Name="Nothing"
                            mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.05"
                            stArea:unit="normalized"
                            stArea:w="0.033245"
                            stArea:x="0.501552"
                            stArea:y="0.905484"/>
                          </rdf:Description>
                        </rdf:li>
                      </rdf:Bag>
                    </mwg-rs:RegionList>
                  </mwg-rs:Regions>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        assertEquals(
            expected = expectedXmp,
            actual = actualXmp
        )
    }

    /**
     * Update an existing XMP to new values.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testUpdateXmp() {

        /* language=XML */
        val existingXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:exif="http://ns.adobe.com/exif/1.0/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                exif:DateTimeOriginal="1980-03-15T08:15:30"
                exif:GPSLatitude="40,44.4245N"
                exif:GPSLongitude="7,70.7534E"
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

        val xmpMeta = XMPMetaFactory.parseFromString(existingXmp)

        writeTestValues(xmpMeta)

        val actualXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

        /* Since we apply the same updates it should look identical to the new.xmp */

        /* language=XML */
        val expectedXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="${XMPVersionInfo.VERSION_MESSAGE}">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:MY="http://ns.mylollc.com/MyloEdit/"
                    xmlns:acdsee="http://ns.acdsee.com/iptc/1.0/"
                    xmlns:ashampoo="http://ns.ashampoo.com/xmp/1.0/"
                    xmlns:dc="http://purl.org/dc/elements/1.1/"
                    xmlns:exif="http://ns.adobe.com/exif/1.0/"
                    xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/"
                    xmlns:stDim="http://ns.adobe.com/xap/1.0/sType/Dimensions#"
                    xmlns:stArea="http://ns.adobe.com/xmp/sType/Area#"
                    xmlns:narrative="http://ns.narrative.so/narrative_select/1.0/"
                    xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                    xmlns:xmpDM="http://ns.adobe.com/xmp/1.0/DynamicMedia/"
                  MY:flag="true"
                  acdsee:tagged="True"
                  exif:DateTimeOriginal="2023-07-07T13:37:42"
                  exif:GPSLatitude="53,13.1635N"
                  exif:GPSLongitude="8,14.3797E"
                  exif:GPSVersionID="2.3.0.0"
                  narrative:Tagged="True"
                  xmp:Rating="3"
                  xmpDM:pick="1">
                  <ashampoo:albums>
                    <rdf:Bag>
                      <rdf:li>America trip</rdf:li>
                      <rdf:li>My wedding</rdf:li>
                    </rdf:Bag>
                  </ashampoo:albums>
                  <dc:subject>
                    <rdf:Bag>
                      <rdf:li>bird</rdf:li>
                      <rdf:li>cat</rdf:li>
                      <rdf:li>dog</rdf:li>
                    </rdf:Bag>
                  </dc:subject>
                  <mwg-rs:Regions rdf:parseType="Resource">
                    <mwg-rs:AppliedToDimensions
                      stDim:h="1000"
                      stDim:unit="pixel"
                      stDim:w="1500"/>
                    <mwg-rs:RegionList>
                      <rdf:Bag>
                        <rdf:li>
                          <rdf:Description
                            mwg-rs:Name="Eye Left"
                            mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.05"
                            stArea:unit="normalized"
                            stArea:w="0.033245"
                            stArea:x="0.295179"
                            stArea:y="0.27888"/>
                          </rdf:Description>
                        </rdf:li>
                        <rdf:li>
                          <rdf:Description
                            mwg-rs:Name="Eye Right"
                            mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.05"
                            stArea:unit="normalized"
                            stArea:w="0.033245"
                            stArea:x="0.81499"
                            stArea:y="0.472579"/>
                          </rdf:Description>
                        </rdf:li>
                        <rdf:li>
                          <rdf:Description
                            mwg-rs:Name="Nothing"
                            mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.05"
                            stArea:unit="normalized"
                            stArea:w="0.033245"
                            stArea:x="0.501552"
                            stArea:y="0.905484"/>
                          </rdf:Description>
                        </rdf:li>
                      </rdf:Bag>
                    </mwg-rs:RegionList>
                  </mwg-rs:Regions>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        assertEquals(
            expected = expectedXmp,
            actual = actualXmp
        )
    }

    private fun writeTestValues(xmpMeta: XMPMeta) {

        xmpMeta.setRating(3)

        xmpMeta.setFlagged(true)

        xmpMeta.setDateTimeOriginal("2023-07-07T13:37:42")

        xmpMeta.setGpsCoordinates(
            latitudeDdm = "53,13.1635N",
            longitudeDdm = "8,14.3797E"
        )

        xmpMeta.setKeywords(setOf("bird", "cat", "dog"))

        xmpMeta.setFaces(
            faces = mapOf(
                "Eye Left" to XMPRegionArea(0.295179, 0.278880, 0.033245, 0.05),
                "Eye Right" to XMPRegionArea(0.814990, 0.472579, 0.033245, 0.05),
                "Nothing" to XMPRegionArea(0.501552, 0.905484, 0.033245, 0.05)
            ),
            widthPx = 1500,
            heightPx = 1000
        )

        xmpMeta.setAlbums(setOf("My wedding", "America trip"))
    }

    /**
     * Create an XMP only containing location info.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCreateLocationXmp() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setLocation(
            XMPLocation(
                name = "Ashampoo GmbH & Co. KG",
                location = "Schafjückenweg 2",
                city = "Rastede",
                state = "Niedersachsen",
                country = "Deutschland"
            )
        )

        val actualXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

        // FIXME
        /* language=XML */
        val expectedXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Ashampoo XMP Core 1.4.3">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:Iptc4xmpExt="http://iptc.org/std/Iptc4xmpExt/2008-02-29/">
                  <Iptc4xmpExt:LocationShown>
                    <rdf:Bag>
                      <rdf:li rdf:parseType="Resource">
                        <rdf:Description
                          Iptc4xmpExt:City="Rastede"
                          Iptc4xmpExt:Country="Deutschland"
                          Iptc4xmpExt:State="Niedersachsen"
                          Iptc4xmpExt:Sublocation="Schafjückenweg 2"/>
                      </rdf:li>
                    </rdf:Bag>
                  </Iptc4xmpExt:LocationShown>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        assertEquals(
            expected = expectedXmp,
            actual = actualXmp
        )
    }
}
