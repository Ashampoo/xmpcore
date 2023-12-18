package com.ashampoo.xmp

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

@OptIn(ExperimentalStdlibApi::class)
internal fun Path.readText(): String =
    SystemFileSystem
        .source(this)
        .buffered()
        .use { it.readByteArray() }
        .decodeToString()
