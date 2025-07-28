package com.ashampoo.xmp

/**
 * Since v1.0.1 we report the library no longer as "Adobe XMP Core 5.1.3"
 * (which it is based on), but under it's real name and version number.
 */
@Suppress("MagicNumber")
public object XMPVersionInfo {

    public const val MAJOR: Int = 1
    public const val MINOR: Int = 6
    public const val PATCH: Int = 0

    public const val VERSION_MESSAGE: String =
        "Ashampoo XMP Core $MAJOR.$MINOR.$PATCH"

    public const val DEBUG: Boolean = false

}
