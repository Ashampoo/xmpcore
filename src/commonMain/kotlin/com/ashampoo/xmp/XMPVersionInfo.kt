package com.ashampoo.xmp

/**
 * Since v1.0.1 we report the library no longer as "Adobe XMP Core 5.1.3"
 * (which it is based on), but under it's real name and version number.
 */
@Suppress("MagicNumber")
object XMPVersionInfo {

    const val MAJOR: Int = 1
    const val MINOR: Int = 2
    const val PATCH: Int = 1

    const val VERSION_MESSAGE = "Ashampoo XMP Core $MAJOR.$MINOR.$PATCH"

    const val DEBUG: Boolean = false

}
