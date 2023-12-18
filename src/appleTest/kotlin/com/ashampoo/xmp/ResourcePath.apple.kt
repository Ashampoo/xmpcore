package com.ashampoo.xmp

import platform.Foundation.NSBundle

actual fun getPathForResource(path: String): String {

    val pathForResource = NSBundle.mainBundle.pathForResource(
        path.substringBeforeLast("."),
        path.substringAfterLast(".")
    ).toString()

    return pathForResource
}
