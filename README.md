# XMP Core for Kotlin Multiplatform

[![Kotlin](https://img.shields.io/badge/kotlin-2.2.0-blue.svg?logo=kotlin)](httpw://kotlinlang.org)
![JVM](https://img.shields.io/badge/-JVM-gray.svg?style=flat)
![Android](https://img.shields.io/badge/-Android-gray.svg?style=flat)
![iOS](https://img.shields.io/badge/-iOS-gray.svg?style=flat)
![Windows](https://img.shields.io/badge/-Windows-gray.svg?style=flat)
![Linux](https://img.shields.io/badge/-Linux-gray.svg?style=flat)
![macOS](https://img.shields.io/badge/-macOS-gray.svg?style=flat)
![JS](https://img.shields.io/badge/-JS-gray.svg?style=flat)
![WASM](https://img.shields.io/badge/-WASM-gray.svg?style=flat)
![WASI](https://img.shields.io/badge/-WASI-gray.svg?style=flat)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ashampoo/xmpcore/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ashampoo/xmpcore)

This library is a port of Adobe's XMP SDK to Kotlin Multiplatform by Ashampoo.

It's part of [Ashampoo Photo Organizer](https://ashampoo.com/photo-organizer).

## Installation

```
implementation("com.ashampoo:xmpcore:1.6.0")
```

## How to use

The library has been designed as a drop-in replacement for users who previously
employed XMP Core Java. Therefore, all the documentation applicable to the
Java SDK also pertains to this library.
However, please note that we have made the decision to remove the functionality for reading
from and writing to ByteArray and InputStreams, as we believe it is unnecessary.

### Sample code

```
val originalXmp: String = "... your XMP ..."

val xmpMeta: XMPMeta = XMPMetaFactory.parseFromString(originalXmp)

val xmpSerializeOptions =
    SerializeOptions()
        .setOmitXmpMetaElement(false)
        .setOmitPacketWrapper(false)
        .setUseCompactFormat(true)
        .setSort(true)

val newXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptions)
```

Check out the [Kotlin JVM example project](examples/xmpcore-kotlin-jvm-sample).

For usage in Java projects check out the [Java example project](examples/xmpcore-java-sample).

Also see the unit tests `ReadXmpTest` and `WriteXmpTest` to learn more about reading and manipulating data.

### Migration hint

If you have previously utilized the official XMP Core Java library available on
Maven Central, please make sure to update your imports from `com.adobe.internal.xmp`
to `com.ashampoo.xmp`.

## Contributions

Contributions to this project are welcome! If you encounter any issues,
have suggestions for improvements, or would like to contribute new features,
please feel free to submit a pull request.

## Acknowledgements

* JetBrains for making [Kotlin](https://kotlinlang.org).
* Adobe for making the XMP Core Java SDK.
* Paul de Vrieze for making [XmlUtil](https://github.com/pdvrieze/xmlutil).

## License

The same [BSD license](original_source/original_license.txt) applies to this project as to Adobe's open source XMP SDK,
from which it is derived.

Note: The original license page went offline, but you can still find it on
[archive.org](https://web.archive.org/web/20210616112605/https://www.adobe.com/devnet/xmp/library/eula-xmp-library-java.html).
