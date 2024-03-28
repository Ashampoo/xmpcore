import com.ashampoo.xmp.XMPConst;
import com.ashampoo.xmp.XMPException;
import com.ashampoo.xmp.XMPMeta;
import com.ashampoo.xmp.XMPMetaFactory;
import com.ashampoo.xmp.options.SerializeOptions;

import java.util.Set;

public class Main {

    private static final SerializeOptions xmpSerializeOptionsCompact =
        new SerializeOptions()
            .setOmitXmpMetaElement(false)
            .setOmitPacketWrapper(false)
            .setUseCompactFormat(true)
            .setUseCanonicalFormat(false)
            .setSort(true);

    public static void main(String[] args) throws XMPException {

        XMPMeta newXmpMeta = XMPMetaFactory.create();

        /*
         * Regular Adobe XMP Core API.
         * You can also use setRating(3)
         */
        newXmpMeta.setPropertyInteger(
            XMPConst.NS_XMP,
            "Rating",
            3
        );

        /* Ashampoo XMP Core convenience method. */
        newXmpMeta.setKeywords(
            Set.of("cat", "cute", "animal")
        );

        String xmp = XMPMetaFactory.serializeToString(
            newXmpMeta,
            xmpSerializeOptionsCompact
        );

        System.out.println("---");
        System.out.println(xmp);

        String oldXmp =
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
                </x:xmpmeta>""";

        XMPMeta oldXmpMeta = XMPMetaFactory.parseFromString(oldXmp);

        System.out.println("---");

        /* Regular Adobe XMP Core API. You can also use getRating() */
        System.out.println("Rating: " + oldXmpMeta.getPropertyInteger(XMPConst.NS_XMP, "Rating"));

        System.out.println("Keywords: " + oldXmpMeta.getKeywords());
    }
}
