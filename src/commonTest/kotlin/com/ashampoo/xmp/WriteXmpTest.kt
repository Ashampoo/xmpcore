package com.ashampoo.xmp

import com.ashampoo.xmp.options.PropertyOptions
import com.ashampoo.xmp.options.SerializeOptions
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.Test
import kotlin.test.fail

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

        val expectedXmp = getXmp("empty.xmp")

        val equals = actualXmp.contentEquals(expectedXmp)

        if (!equals) {

            SystemFileSystem
                .sink(Path("build/empty.xmp"))
                .buffered()
                .use {
                    it.write(actualXmp.encodeToByteArray())
                }

            fail("XMP empty.xmp looks different.")
        }
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

        val expectedXmp = getXmp("rating.xmp")

        val equals = actualXmp.contentEquals(expectedXmp)

        if (!equals) {

            SystemFileSystem
                .sink(Path("build/rating.xmp"))
                .buffered()
                .use {
                    it.write(actualXmp.encodeToByteArray())
                }

            fail("XMP rating.xmp looks different.")
        }
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

        val expectedXmp = getXmp("new.xmp")

        val equals = actualXmp.contentEquals(expectedXmp)

        if (!equals) {

            SystemFileSystem
                .sink(Path("build/new.xmp"))
                .buffered()
                .use {
                    it.write(actualXmp.encodeToByteArray())
                }

            fail("XMP new.xmp looks different.")
        }
    }

    /**
     * Update an existing XMP to new values.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testUpdateXmp() {

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
        val expectedXmp = getXmp("new.xmp")

        val equals = actualXmp.contentEquals(expectedXmp)

        if (!equals) {

            SystemFileSystem
                .sink(Path("build/updated.xmp"))
                .buffered()
                .use {
                    it.write(actualXmp.encodeToByteArray())
                }

            fail("XMP updated.xmp looks different.")
        }
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

    private fun getXmp(name: String): String =
        Path(getPathForResource("$RESOURCE_PATH/$name")).readText()

    companion object {
        private const val RESOURCE_PATH: String = "src/commonTest/resources/com/ashampoo/xmp"
    }
}
