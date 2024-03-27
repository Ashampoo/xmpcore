import com.ashampoo.xmp.XMPException;
import com.ashampoo.xmp.XMPMeta;
import com.ashampoo.xmp.XMPMetaFactory;
import com.ashampoo.xmp.options.SerializeOptions;

public class Main {

    public static void main(String[] args) throws XMPException {

        SerializeOptions xmpSerializeOptionsCompact =
            new SerializeOptions()
                .setOmitXmpMetaElement(false)
                .setOmitPacketWrapper(false)
                .setUseCompactFormat(true)
                .setUseCanonicalFormat(false)
                .setSort(true);

        XMPMeta xmpMeta = XMPMetaFactory.INSTANCE.create();

        xmpMeta.setRating(3);

        String xmp =
            XMPMetaFactory.INSTANCE.serializeToString(xmpMeta, xmpSerializeOptionsCompact);

        System.out.println(xmp);
    }
}
