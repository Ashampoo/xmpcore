package com.ashampoo.xmp.impl

import nl.adaptivity.xmlutil.dom.Document

fun interface DomParser {

    fun parseDocumentFromString(input: String): Document

}
