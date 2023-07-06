package com.ashampoo.xmp

import com.ashampoo.xmp.options.SerializeOptions
import com.goncalossilva.resources.Resource
import kotlinx.io.files.Path
import kotlinx.io.files.sink
import kotlin.test.Test
import kotlin.test.fail

/**
 * Different tests where XMP is loaded and written.
 * One test loads from a source of various real-world XMP files.
 * The other tests ensure that there is no data loss on roundtrips and conversions.
 */
class RewriteXmpTest {

    private val xmpSerializeOptionsCompact =
        SerializeOptions()
            .setOmitXmpMetaElement(false)
            .setOmitPacketWrapper(false)
            .setUseCompactFormat(true)
            .setUseCanonicalFormat(false)
            .setSort(true)

    private val xmpSerializeOptionsCanonical =
        SerializeOptions()
            .setOmitXmpMetaElement(false)
            .setOmitPacketWrapper(false)
            .setUseCompactFormat(false)
            .setUseCanonicalFormat(true)
            .setSort(true)

    /**
     * Regression test based on a fixed small set of test files.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testRewriteXmp() {

        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 1..TEST_PHOTO_COUNT) {

            val originalXmp = getOriginalXmp(index)

            val xmpMeta = XMPMetaFactory.parseFromString(originalXmp)

            val actualCompactXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)
            val actualCanonicalXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCanonical)

            val expectedCompactXmp = getFormattedCompactXmp(index)
            val expectedCanonicalXmp = getFormattedCanonicalXmp(index)

            val equals = expectedCompactXmp.contentEquals(actualCompactXmp) &&
                expectedCanonicalXmp.contentEquals(actualCanonicalXmp)

            if (!equals) {

                Path("build/sample_${index}_formatted_compact.xmp").sink().use {
                    it.write(actualCompactXmp.encodeToByteArray())
                }

                Path("build/sample_${index}_formatted_canonical.xmp").sink().use {
                    it.write(actualCanonicalXmp.encodeToByteArray())
                }

                fail("XMP for sample $index looks different after rewrite.")
            }
        }
    }

    /**
     * Loading a compact formatted file and saving it should result in the same file.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testRoundtripCompact() {

        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 1..TEST_PHOTO_COUNT) {

            val originalXmp = getFormattedCompactXmp(index)

            val xmpMeta = XMPMetaFactory.parseFromString(originalXmp)

            val newXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

            val equals = originalXmp.contentEquals(newXmp)

            if (!equals) {

                Path("build/sample_${index}_roundtrip_compact.xmp").sink().use {
                    it.write(newXmp.encodeToByteArray())
                }

                fail("XMP for sample $index looks different after compact roundtrip.")
            }
        }
    }

    /**
     * Loading a compact formatted file and saving it should result in the same file.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testRoundtripCanonical() {

        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 1..TEST_PHOTO_COUNT) {

            val originalXmp = getFormattedCanonicalXmp(index)

            val xmpMeta = XMPMetaFactory.parseFromString(originalXmp)

            val newXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCanonical)

            val equals = originalXmp.contentEquals(newXmp)

            if (!equals) {

                Path("build/sample_${index}_roundtrip_canonical.xmp").sink().use {
                    it.write(newXmp.encodeToByteArray())
                }

                fail("XMP for sample $index looks different after canonical roundtrip.")
            }
        }
    }

    /**
     * Conversion from compact to canonical should be lossless.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testConvertCompactToCanonical() {

        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 1..TEST_PHOTO_COUNT) {

            val compactXmp = getFormattedCompactXmp(index)

            val xmpMeta = XMPMetaFactory.parseFromString(compactXmp)

            val newXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCanonical)

            val equals = newXmp.contentEquals(getFormattedCanonicalXmp(index))

            if (!equals) {

                Path("build/sample_${index}_compact_to_canonical.xmp").sink().use {
                    it.write(newXmp.encodeToByteArray())
                }

                fail("XMP for sample $index looks different after compact to canonical conversion.")
            }
        }
    }

    /**
     * Conversion from compact to canonical should be lossless.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testConvertCanonicalToCompact() {

        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 1..TEST_PHOTO_COUNT) {

            val canonicalXmp = getFormattedCanonicalXmp(index)

            val xmpMeta = XMPMetaFactory.parseFromString(canonicalXmp)

            val newXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

            val equals = newXmp.contentEquals(getFormattedCompactXmp(index))

            if (!equals) {

                Path("build/sample_${index}_canonical_to_compact.xmp").sink().use {
                    it.write(newXmp.encodeToByteArray())
                }

                fail("XMP for sample $index looks different after canonical to compact conversion.")
            }
        }
    }

    private fun getOriginalXmp(index: Int): String =
        Resource("$RESOURCE_PATH/sample_$index.xmp").readText()

    private fun getFormattedCompactXmp(index: Int): String =
        Resource("$RESOURCE_PATH/sample_${index}_formatted_compact.xmp").readText()

    private fun getFormattedCanonicalXmp(index: Int): String =
        Resource("$RESOURCE_PATH/sample_${index}_formatted_canonical.xmp").readText()

    companion object {

        const val TEST_PHOTO_COUNT = 100

        private const val RESOURCE_PATH: String = "src/commonTest/resources/com/ashampoo/xmp"
    }
}
