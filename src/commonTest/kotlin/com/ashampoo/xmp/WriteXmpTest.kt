package com.ashampoo.xmp

import com.ashampoo.xmp.XMPConst.DEFAULT_GPS_VERSION_ID
import com.ashampoo.xmp.XMPConst.XMP_DC_SUBJECT
import com.ashampoo.xmp.options.PropertyOptions
import com.ashampoo.xmp.options.SerializeOptions
import com.goncalossilva.resources.Resource
import kotlinx.io.files.Path
import kotlinx.io.files.sink
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

    private val arrayOptions =
        PropertyOptions().setArray(true)

    /**
     * Create an empty XMP file with only the required envelope.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun createEmptyXmp() {

        val xmpMeta = XMPMetaFactory.create()

        val actualXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

        val expectedXmp = getXmp("empty.xmp")

        val equals = actualXmp.contentEquals(expectedXmp)

        if (!equals) {

            Path("build/empty.xmp").sink().use {
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
    fun createRatingXmp() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setPropertyInteger(XMPConst.NS_XMP, "Rating", 3)

        val actualXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

        val expectedXmp = getXmp("rating.xmp")

        val equals = actualXmp.contentEquals(expectedXmp)

        if (!equals) {

            Path("build/rating.xmp").sink().use {
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
    fun createNewXmp() {

        val xmpMeta = XMPMetaFactory.create()

        writeTestValues(xmpMeta)

        val actualXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

        val expectedXmp = getXmp("new.xmp")

        val equals = actualXmp.contentEquals(expectedXmp)

        if (!equals) {

            Path("build/new.xmp").sink().use {
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
    fun updateXmp() {

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

            Path("build/updated.xmp").sink().use {
                it.write(actualXmp.encodeToByteArray())
            }

            fail("XMP updated.xmp looks different.")
        }
    }

    private fun writeTestValues(xmpMeta: XMPMeta) {

        /* Write rating. */
        xmpMeta.setPropertyInteger(XMPConst.NS_XMP, "Rating", 3)

        /* Write taken date. */
        xmpMeta.setProperty(XMPConst.NS_EXIF, "DateTimeOriginal", "2023-07-07T13:37:42")

        /* Write GPS coordinates. */
        xmpMeta.setProperty(XMPConst.NS_EXIF, "GPSVersionID", DEFAULT_GPS_VERSION_ID)
        xmpMeta.setProperty(XMPConst.NS_EXIF, "GPSLatitude", "53,13.1635N")
        xmpMeta.setProperty(XMPConst.NS_EXIF, "GPSLongitude", "8,14.3797E")

        /* Create a new array property for keywords. */
        xmpMeta.setProperty(XMPConst.NS_DC, XMP_DC_SUBJECT, null, arrayOptions)

        /* Fill the new array with keywords. */
        for (keyword in listOf("bird", "cat", "dog"))
            xmpMeta.appendArrayItem(
                schemaNS = XMPConst.NS_DC,
                arrayName = XMP_DC_SUBJECT,
                itemValue = keyword
            )
    }

    private fun getXmp(name: String): String =
        Resource("$RESOURCE_PATH/$name").readText()

    companion object {
        private const val RESOURCE_PATH: String = "src/commonTest/resources/com/ashampoo/xmp"
    }
}
