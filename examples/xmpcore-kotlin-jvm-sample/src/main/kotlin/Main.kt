import com.ashampoo.xmp.XMPConst
import com.ashampoo.xmp.XMPMetaFactory.create
import com.ashampoo.xmp.XMPMetaFactory.parseFromString
import com.ashampoo.xmp.XMPMetaFactory.serializeToString
import com.ashampoo.xmp.options.ParseOptions
import com.ashampoo.xmp.options.PropertyOptions
import com.ashampoo.xmp.options.SerializeOptions

private val xmpSerializeOptionsCompact = SerializeOptions()
    .setOmitXmpMetaElement(false)
    .setOmitPacketWrapper(false)
    .setUseCompactFormat(true)
    .setUseCanonicalFormat(false)
    .setSort(true)

private val xmpParseOptions = ParseOptions()
    .setRequireXMPMeta(false)

fun main() {

    val newXmpMeta = create()

    /*
     * Regular Adobe XMP Core API.
     * You can also use setRating(3)
     */
    newXmpMeta.setPropertyInteger(
        XMPConst.NS_XMP,
        "Rating",
        3,
        PropertyOptions()
    )

    /* Ashampoo XMP Core convenience method. */
    newXmpMeta.setKeywords(
        setOf("cat", "cute", "animal")
    )

    val xmp = serializeToString(
        newXmpMeta,
        xmpSerializeOptionsCompact
    )

    println("---")
    println(xmp)

    val oldXmp =
        """
                  <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="">
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about=""
                        xmlns:dc="http://purl.org/dc/elements/1.1/"
                        xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                      <xmp:Rating>5</xmp:Rating>
                      <dc:subject>
                        <rdf:Bag>
                          <rdf:li>animal</rdf:li>
                          <rdf:li>bird</rdf:li>
                        </rdf:Bag>
                      </dc:subject>
                    </rdf:Description>
                  </rdf:RDF>
                </x:xmpmeta>
                """.trimIndent()

    val oldXmpMeta =
        parseFromString(oldXmp, xmpParseOptions)

    println("---")

    /* Regular Adobe XMP Core API. You can also use getRating() */
    println("Rating: " + oldXmpMeta.getPropertyInteger(XMPConst.NS_XMP, "Rating"))

    println("Keywords: " + oldXmpMeta.getKeywords())
}
