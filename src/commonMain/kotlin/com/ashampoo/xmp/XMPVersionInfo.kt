package com.ashampoo.xmp

/**
 * We ported from version 1.5.3, which was the final release available on
 * https://www.adobe.com/devnet/xmp/library/eula-xmp-library-java.html
 * under the BSD license, prior to the webpage being taken down.
 * Hence we report this as the used version.
 */
@Suppress("MagicNumber")
object XMPVersionInfo {

    const val VERSION_MESSAGE = "Adobe XMP Core 5.1.3"

    const val major: Int = 5
    const val minor: Int = 1
    const val micro: Int = 3
    const val build: Int = 0
    const val isDebug: Boolean = false
    const val message: String = VERSION_MESSAGE

}
