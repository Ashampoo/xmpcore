// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

/**
 * Common constants for the XMP Toolkit.
 */
object XMPConst {

    /**
     * The XML namespace for XML.
     */
    const val NS_XML: String = "http://www.w3.org/XML/1998/namespace"

    /**
     * The XML namespace for RDF.
     */
    const val NS_RDF: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

    /**
     * The XML namespace for the Dublin Core schema.
     */
    const val NS_DC: String = "http://purl.org/dc/elements/1.1/"

    /**
     * The XML namespace for the IPTC Core schema.
     */
    const val NS_IPTC_CORE: String = "http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/"

    /**
     * The XML namespace for the IPTC Extension schema.
     */
    const val NS_IPTC_EXT: String = "http://iptc.org/std/Iptc4xmpExt/2008-02-29/"

    /**
     * The XML namespace for the DICOM medical schema.
     */
    const val NS_DICOM: String = "http://ns.adobe.com/DICOM/"

    /**
     * The XML namespace for the PLUS (Picture Licensing Universal System, http://www.useplus.org)
     */
    const val NS_PLUS: String = "http://ns.useplus.org/ldf/xmp/1.0/"

    const val NS_MWG_RS: String = "http://www.metadataworkinggroup.com/schemas/regions/"

    const val NS_ACDSEE: String = "http://ns.acdsee.com/iptc/1.0/"

    // Adobe standard namespaces

    /**
     * The XML namespace Adobe XMP Metadata.
     */
    const val NS_X: String = "adobe:ns:meta/"

    const val NS_IX: String = "http://ns.adobe.com/iX/1.0/"

    /**
     * The XML namespace for the XMP "basic" schema.
     */
    const val NS_XMP: String = "http://ns.adobe.com/xap/1.0/"

    /**
     * The XML namespace for the XMP copyright schema.
     */
    const val NS_XMP_RIGHTS: String = "http://ns.adobe.com/xap/1.0/rights/"

    /**
     * The XML namespace for the XMP digital asset management schema.
     */
    const val NS_XMP_MM: String = "http://ns.adobe.com/xap/1.0/mm/"

    /**
     * The XML namespace for the job management schema.
     */
    const val NS_XMP_BJ: String = "http://ns.adobe.com/xap/1.0/bj/"

    /**
     * The XML namespace for the job management schema.
     */
    const val NS_XMP_NOTE: String = "http://ns.adobe.com/xmp/note/"

    /**
     * The XML namespace for the PDF schema.
     */
    const val NS_PDF: String = "http://ns.adobe.com/pdf/1.3/"

    /**
     * The XML namespace for the PDF schema.
     */
    const val NS_PDFX: String = "http://ns.adobe.com/pdfx/1.3/"

    const val NS_PDFX_ID: String = "http://www.npes.org/pdfx/ns/id/"

    const val NS_PDFA_SCHEMA: String = "http://www.aiim.org/pdfa/ns/schema#"

    const val NS_PDFA_PROPERTY: String = "http://www.aiim.org/pdfa/ns/property#"

    const val NS_PDFA_TYPE: String = "http://www.aiim.org/pdfa/ns/type#"

    const val NS_PDFA_FIELD: String = "http://www.aiim.org/pdfa/ns/field#"

    const val NS_PDFA_ID: String = "http://www.aiim.org/pdfa/ns/id/"

    const val NS_PDFA_EXTENSION: String = "http://www.aiim.org/pdfa/ns/extension/"

    /**
     * The XML namespace for the Photoshop custom schema.
     */
    const val NS_PHOTOSHOP: String = "http://ns.adobe.com/photoshop/1.0/"

    /**
     * The XML namespace for the Photoshop Album schema.
     */
    const val NS_PS_ALBUM: String = "http://ns.adobe.com/album/1.0/"

    /**
     * The XML namespace for Adobe's EXIF schema.
     */
    const val NS_EXIF: String = "http://ns.adobe.com/exif/1.0/"

    /**
     * NS for the CIPA XMP for Exif document v1.1
     */
    const val NS_EXIF_CIPA: String = "http://cipa.jp/exif/1.0/"

    const val NS_EXIF_AUX: String = "http://ns.adobe.com/exif/1.0/aux/"

    const val NS_TIFF: String = "http://ns.adobe.com/tiff/1.0/"

    const val NS_PNG: String = "http://ns.adobe.com/png/1.0/"

    const val NS_JPEG: String = "http://ns.adobe.com/jpeg/1.0/"

    const val NS_JP2K: String = "http://ns.adobe.com/jp2k/1.0/"

    const val NS_CAMERA_RAW: String = "http://ns.adobe.com/camera-raw-settings/1.0/"

    const val NS_ADOBE_STOCK_PHOTO: String = "http://ns.adobe.com/StockPhoto/1.0/"

    const val NS_CREATOR_ATOM: String = "http://ns.adobe.com/creatorAtom/1.0/"

    const val NS_ASF: String = "http://ns.adobe.com/asf/1.0/"

    const val NS_WAV: String = "http://ns.adobe.com/xmp/wav/1.0/"

    /**
     * BExt Schema
     */
    const val NS_BWF: String = "http://ns.adobe.com/bwf/bext/1.0/"

    /**
     * RIFF Info Schema
     */
    const val NS_RIFF_INFO: String = "http://ns.adobe.com/riff/info/"

    const val NS_SCRIPT: String = "http://ns.adobe.com/xmp/1.0/Script/"

    /**
     * Transform XMP
     */
    const val NS_TRANSFORM_XMP: String = "http://ns.adobe.com/TransformXMP/"

    /**
     * Adobe Flash SWF
     */
    const val NS_SWF: String = "http://ns.adobe.com/swf/1.0/"

    // XMP namespaces that are Adobe private

    const val NS_DM: String = "http://ns.adobe.com/xmp/1.0/DynamicMedia/"

    const val NS_TRANSIENT: String = "http://ns.adobe.com/xmp/transient/1.0/"

    /**
     * legacy Dublin Core NS, will be converted to NS_DC
     */
    const val NS_DC_DEPRECATED: String = "http://purl.org/dc/1.1/"

    // XML namespace constants for qualifiers and structured property fields.

    /**
     * The XML namespace for qualifiers of the xmp:Identifier property.
     */
    const val TYPE_IDENTIFIERQUAL: String = "http://ns.adobe.com/xmp/Identifier/qual/1.0/"

    /**
     * The XML namespace for fields of the Dimensions type.
     */
    const val TYPE_DIMENSIONS: String = "http://ns.adobe.com/xap/1.0/sType/Dimensions#"

    const val TYPE_AREA: String = "http://ns.adobe.com/xmp/sType/Area#"

    const val TYPE_TEXT: String = "http://ns.adobe.com/xap/1.0/t/"

    const val TYPE_PAGED_FILE: String = "http://ns.adobe.com/xap/1.0/t/pg/"

    const val TYPE_GRAPHICS: String = "http://ns.adobe.com/xap/1.0/g/"

    /**
     * The XML namespace for fields of a graphical image. Used for the Thumbnail type.
     */
    const val TYPE_IMAGE: String = "http://ns.adobe.com/xap/1.0/g/img/"

    const val TYPE_FONT: String = "http://ns.adobe.com/xap/1.0/sType/Font#"

    /**
     * The XML namespace for fields of the ResourceEvent type.
     */
    const val TYPE_RESOURCE_EVENT: String = "http://ns.adobe.com/xap/1.0/sType/ResourceEvent#"

    /**
     * The XML namespace for fields of the ResourceRef type.
     */
    const val TYPE_RESOURCE_REF: String = "http://ns.adobe.com/xap/1.0/sType/ResourceRef#"

    /**
     * The XML namespace for fields of the Version type.
     */
    const val TYPE_ST_VERSION: String = "http://ns.adobe.com/xap/1.0/sType/Version#"

    /**
     * The XML namespace for fields of the JobRef type.
     */
    const val TYPE_ST_JOB: String = "http://ns.adobe.com/xap/1.0/sType/Job#"

    const val TYPE_MANIFEST_ITEM: String = "http://ns.adobe.com/xap/1.0/sType/ManifestItem#"

    // ---------------------------------------------------------------------------------------------
    // Basic types and constants

    /**
     * The canonical true string value for Booleans in serialized XMP. Code that converts from the
     * string to a bool should be case-insensitive, and even allow "1".
     */
    const val TRUE_STRING: String = "True"

    /**
     * The canonical false string value for Booleans in serialized XMP. Code that converts from the
     * string to a bool should be case-insensitive, and even allow "0".
     */
    const val FALSE_STRING: String = "False"

    /**
     * Index that has the meaning to be always the last item in an array.
     */
    const val ARRAY_LAST_ITEM: Int = -1

    /**
     * Node name of an array item.
     */
    const val ARRAY_ITEM_NAME: String = "[]"

    /**
     * The x-default string for localized properties
     */
    const val X_DEFAULT: String = "x-default"

    /**
     * xml:lang qualfifier
     */
    const val XML_LANG: String = "xml:lang"

    /**
     * rdf:type qualfifier
     */
    const val RDF_TYPE: String = "rdf:type"

    /**
     * Processing Instruction (PI) for xmp packet
     */
    const val XMP_PI: String = "xpacket"

    /**
     * XMP meta tag version new
     */
    const val TAG_XMPMETA: String = "xmpmeta"

    /**
     * XMP meta tag version old
     */
    const val TAG_XAPMETA: String = "xapmeta"

    /** GPSVersionID as written by default by ExifTool. */
    const val DEFAULT_GPS_VERSION_ID = "2.3.0.0"

    const val XMP_DC_SUBJECT: String = "subject"

    const val XMP_ACDSEE_KEYWORDS: String = "keywords"

    const val XMP_IPTC_EXT_PERSON_IN_IMAGE: String = "PersonInImage"

    const val XMP_MWG_RS_TYPE_FACE: String = "Face"

}
