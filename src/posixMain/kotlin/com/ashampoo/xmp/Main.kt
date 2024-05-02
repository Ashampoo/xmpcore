package com.ashampoo.xmp

import com.ashampoo.xmp.options.SerializeOptions
import platform.posix.perror

private val xmpSerializeOptionsCompact =
    SerializeOptions()
        .setOmitXmpMetaElement(false)
        .setOmitPacketWrapper(false)
        .setUseCompactFormat(true)
        .setUseCanonicalFormat(false)
        .setSort(true)

public fun main(args: Array<String>) {

    if (args.size != 1) {
        println("USAGE: Must be called with one argument.")
        return
    }

    val filePath = args.first()

    val bytes = readFileAsByteArray(filePath)

    if (bytes == null) {
        perror("File could not be read: $filePath")
        return
    }

    val xmp = bytes.decodeToString()

    val xmpMeta = XMPMetaFactory.parseFromString(xmp)

    val newXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

    println(newXmp)
}
