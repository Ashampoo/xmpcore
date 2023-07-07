// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.impl

import com.ashampoo.xmp.XMPConst
import com.ashampoo.xmp.XMPError
import com.ashampoo.xmp.XMPException
import com.ashampoo.xmp.XMPSchemaRegistry
import com.ashampoo.xmp.impl.Utils.isXMLNameNS
import com.ashampoo.xmp.options.AliasOptions
import com.ashampoo.xmp.properties.XMPAliasInfo

/**
 * The schema registry handles the namespaces, aliases and global options for the XMP Toolkit.
 * There is only one single instance used by the toolkit.
 */
object XMPSchemaRegistryImpl : XMPSchemaRegistry {

    /**
     * a map from a namespace URI to its registered prefix
     */
    private val namespaceToPrefixMap: MutableMap<String, String> = mutableMapOf()

    /**
     * a map from a prefix to the associated namespace URI
     */
    private val prefixToNamespaceMap: MutableMap<String, String> = mutableMapOf()

    /**
     * a map of all registered aliases.
     * The map is a relationship from a qname to an `XMPAliasInfo`-object.
     */
    private val aliasMap: MutableMap<String, XMPAliasInfo> = mutableMapOf()

    /**
     * The pattern that must not be contained in simple properties
     */
    private val simpleProperyPattern = Regex("[/*?\\[\\]]")

    /**
     * Performs the initialisation of the registry with the default namespaces, aliases and global
     * options.
     */
    init {
        try {

            registerStandardNamespaces()
            registerStandardAliases()

        } catch (ex: XMPException) {
            throw IllegalStateException("The XMPSchemaRegistry cannot be initialized!", ex)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Namespace Functions

    override fun registerNamespace(namespaceURI: String, suggestedPrefix: String): String {

        var suggestedPrefix = suggestedPrefix

        if (namespaceURI.isEmpty())
            throw XMPException("Empty schema namespace URI", XMPError.BADPARAM)

        if (suggestedPrefix.isEmpty())
            throw XMPException("Empty prefix", XMPError.BADPARAM)

        if (suggestedPrefix[suggestedPrefix.length - 1] != ':')
            suggestedPrefix += ':'

        if (!isXMLNameNS(suggestedPrefix.substring(0, suggestedPrefix.length - 1)))
            throw XMPException("The prefix is a bad XML name", XMPError.BADXML)

        val registeredPrefix = namespaceToPrefixMap[namespaceURI]
        val registeredNS = prefixToNamespaceMap[suggestedPrefix]

        // Return the actual prefix
        if (registeredPrefix != null)
            return registeredPrefix

        if (registeredNS != null) {

            // the namespace is new, but the prefix is already engaged,
            // we generate a new prefix out of the suggested
            var generatedPrefix = suggestedPrefix

            var i = 1

            while (prefixToNamespaceMap.containsKey(generatedPrefix)) {
                generatedPrefix =
                    suggestedPrefix.substring(0, suggestedPrefix.length - 1) + "_" + i + "_:"
                i++
            }

            suggestedPrefix = generatedPrefix
        }

        prefixToNamespaceMap[suggestedPrefix] = namespaceURI
        namespaceToPrefixMap[namespaceURI] = suggestedPrefix

        // Return the suggested prefix
        return suggestedPrefix
    }

    override fun deleteNamespace(namespaceURI: String) {

        val prefixToDelete = getNamespacePrefix(namespaceURI) ?: return

        namespaceToPrefixMap.remove(namespaceURI)
        prefixToNamespaceMap.remove(prefixToDelete)
    }

    override fun getNamespacePrefix(namespaceURI: String): String? =
        namespaceToPrefixMap[namespaceURI]

    override fun getNamespaceURI(namespacePrefix: String): String? {

        var namespacePrefix = namespacePrefix

        if (!namespacePrefix.endsWith(":"))
            namespacePrefix += ":"

        return prefixToNamespaceMap[namespacePrefix]
    }

    override fun getNamespaces(): Map<String, String> =
        namespaceToPrefixMap

    override fun getPrefixes(): Map<String, String> =
        prefixToNamespaceMap

    /**
     * Register the standard namespaces of schemas and types that are included in the XMP
     * Specification and some other Adobe private namespaces.
     * Note: This method is not lock because only called by the constructor.
     */
    private fun registerStandardNamespaces() {

        // register standard namespaces
        registerNamespace(XMPConst.NS_XML, "xml")
        registerNamespace(XMPConst.NS_RDF, "rdf")
        registerNamespace(XMPConst.NS_DC, "dc")
        registerNamespace(XMPConst.NS_IPTC_CORE, "Iptc4xmpCore")
        registerNamespace(XMPConst.NS_IPTC_EXT, "Iptc4xmpExt")
        registerNamespace(XMPConst.NS_DICOM, "DICOM")
        registerNamespace(XMPConst.NS_PLUS, "plus")

        // register other common schemas
        registerNamespace(XMPConst.NS_MWG_RS, "mwg-rs")
        registerNamespace(XMPConst.NS_ACDSEE, "acdsee")

        // register Adobe standard namespaces
        registerNamespace(XMPConst.NS_X, "x")
        registerNamespace(XMPConst.NS_IX, "iX")
        registerNamespace(XMPConst.NS_XMP, "xmp")
        registerNamespace(XMPConst.NS_XMP_RIGHTS, "xmpRights")
        registerNamespace(XMPConst.NS_XMP_MM, "xmpMM")
        registerNamespace(XMPConst.NS_XMP_BJ, "xmpBJ")
        registerNamespace(XMPConst.NS_XMP_NOTE, "xmpNote")
        registerNamespace(XMPConst.NS_PDF, "pdf")
        registerNamespace(XMPConst.NS_PDFX, "pdfx")
        registerNamespace(XMPConst.NS_PDFX_ID, "pdfxid")
        registerNamespace(XMPConst.NS_PDFA_SCHEMA, "pdfaSchema")
        registerNamespace(XMPConst.NS_PDFA_PROPERTY, "pdfaProperty")
        registerNamespace(XMPConst.NS_PDFA_TYPE, "pdfaType")
        registerNamespace(XMPConst.NS_PDFA_FIELD, "pdfaField")
        registerNamespace(XMPConst.NS_PDFA_ID, "pdfaid")
        registerNamespace(XMPConst.NS_PDFA_EXTENSION, "pdfaExtension")
        registerNamespace(XMPConst.NS_PHOTOSHOP, "photoshop")
        registerNamespace(XMPConst.NS_PS_ALBUM, "album")
        registerNamespace(XMPConst.NS_EXIF, "exif")
        registerNamespace(XMPConst.NS_EXIF_CIPA, "exifEX")
        registerNamespace(XMPConst.NS_EXIF_AUX, "aux")
        registerNamespace(XMPConst.NS_TIFF, "tiff")
        registerNamespace(XMPConst.NS_PNG, "png")
        registerNamespace(XMPConst.NS_JPEG, "jpeg")
        registerNamespace(XMPConst.NS_JP2K, "jp2k")
        registerNamespace(XMPConst.NS_CAMERA_RAW, "crs")
        registerNamespace(XMPConst.NS_ADOBE_STOCK_PHOTO, "bmsp")
        registerNamespace(XMPConst.NS_CREATOR_ATOM, "creatorAtom")
        registerNamespace(XMPConst.NS_ASF, "asf")
        registerNamespace(XMPConst.NS_WAV, "wav")
        registerNamespace(XMPConst.NS_BWF, "bext")
        registerNamespace(XMPConst.NS_RIFF_INFO, "riffinfo")
        registerNamespace(XMPConst.NS_SCRIPT, "xmpScript")
        registerNamespace(XMPConst.NS_TRANSFORM_XMP, "txmp")
        registerNamespace(XMPConst.NS_SWF, "swf")

        // register Adobe private namespaces
        registerNamespace(XMPConst.NS_DM, "xmpDM")
        registerNamespace(XMPConst.NS_TRANSIENT, "xmpx")

        // register Adobe standard type namespaces
        registerNamespace(XMPConst.TYPE_TEXT, "xmpT")
        registerNamespace(XMPConst.TYPE_PAGED_FILE, "xmpTPg")
        registerNamespace(XMPConst.TYPE_GRAPHICS, "xmpG")
        registerNamespace(XMPConst.TYPE_IMAGE, "xmpGImg")
        registerNamespace(XMPConst.TYPE_FONT, "stFnt")
        registerNamespace(XMPConst.TYPE_DIMENSIONS, "stDim")
        registerNamespace(XMPConst.TYPE_RESOURCE_EVENT, "stEvt")
        registerNamespace(XMPConst.TYPE_RESOURCE_REF, "stRef")
        registerNamespace(XMPConst.TYPE_ST_VERSION, "stVer")
        registerNamespace(XMPConst.TYPE_ST_JOB, "stJob")
        registerNamespace(XMPConst.TYPE_MANIFEST_ITEM, "stMfs")
        registerNamespace(XMPConst.TYPE_IDENTIFIERQUAL, "xmpidq")
    }

    // ---------------------------------------------------------------------------------------------
    // Alias Functions

    override fun resolveAlias(aliasNS: String, aliasProp: String): XMPAliasInfo? {

        val aliasPrefix = getNamespacePrefix(aliasNS) ?: return null

        return aliasMap[aliasPrefix + aliasProp]
    }

    override fun findAlias(qname: String): XMPAliasInfo? =
        aliasMap[qname]

    override fun findAliases(aliasNS: String): Set<XMPAliasInfo> {

        val prefix = getNamespacePrefix(aliasNS)

        if (prefix == null) return emptySet()

        val result = mutableSetOf<XMPAliasInfo>()

        for (qname in aliasMap.keys) {

            if (qname.startsWith(prefix)) {

                val alias = findAlias(qname) ?: continue

                result.add(alias)
            }
        }

        return result
    }

    /**
     * Associates an alias name with an actual name.
     *
     * Define a alias mapping from one namespace/property to another. Both
     * property names must be simple names. An alias can be a direct mapping,
     * where the alias and actual have the same data type. It is also possible
     * to map a simple alias to an item in an array. This can either be to the
     * first item in the array, or to the 'x-default' item in an alt-text array.
     * Multiple alias names may map to the same actual, as long as the forms
     * match. It is a no-op to reregister an alias in an identical fashion.
     * Note: This method is not locking because only called by registerStandardAliases
     * which is only called by the constructor.
     * Note2: The method is only package-private so that it can be tested with unittests
     *
     * @param aliasNS    The namespace URI for the alias. Must not be null or the empty
     * string.
     * @param aliasProp  The name of the alias. Must be a simple name, not null or the
     * empty string and not a general path expression.
     * @param actualNS   The namespace URI for the actual. Must not be null or the
     * empty string.
     * @param actualProp The name of the actual. Must be a simple name, not null or the
     * empty string and not a general path expression.
     * @param aliasForm  Provides options for aliases for simple aliases to array
     * items. This is needed to know what kind of array to create if
     * set for the first time via the simple alias. Pass
     * `XMP_NoOptions`, the default value, for all
     * direct aliases regardless of whether the actual data type is
     * an array or not (see [AliasOptions]).
     */
    fun registerAlias(
        aliasNS: String,
        aliasProp: String,
        actualNS: String,
        actualProp: String,
        aliasForm: AliasOptions?
    ) {

        if (aliasNS.isEmpty())
            throw XMPException("Empty schema namespace URI", XMPError.BADPARAM)

        if (aliasProp.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        if (actualNS.isEmpty())
            throw XMPException("Empty schema namespace URI", XMPError.BADPARAM)

        if (actualProp.isEmpty())
            throw XMPException("Empty property name", XMPError.BADPARAM)

        // Fix the alias options
        val aliasOpts = if (aliasForm != null)
            AliasOptions(
                XMPNodeUtils.verifySetOptions(
                    aliasForm.toPropertyOptions(),
                    null
                ).getOptions()
            ) else
            AliasOptions()

        if (simpleProperyPattern.matches(aliasProp) || simpleProperyPattern.matches(actualProp))
            throw XMPException("Alias and actual property names must be simple", XMPError.BADXPATH)

        // check if both namespaces are registered
        val aliasPrefix = getNamespacePrefix(aliasNS)
        val actualPrefix = getNamespacePrefix(actualNS)

        if (aliasPrefix == null)
            throw XMPException("Alias namespace is not registered", XMPError.BADSCHEMA)
        else if (actualPrefix == null)
            throw XMPException("Actual namespace is not registered", XMPError.BADSCHEMA)

        val key = aliasPrefix + aliasProp

        // check if alias is already existing
        if (aliasMap.containsKey(key))
            throw XMPException("Alias is already existing", XMPError.BADPARAM)
        else if (aliasMap.containsKey(actualPrefix + actualProp))
            throw XMPException(
                "Actual property is already an alias, use the base property", XMPError.BADPARAM
            )

        val aliasInfo: XMPAliasInfo = object : XMPAliasInfo {

            override fun getNamespace(): String = actualNS

            override fun getPrefix(): String = actualPrefix

            override fun getPropName(): String = actualProp

            override fun getAliasForm(): AliasOptions = aliasOpts

            override fun toString(): String =
                actualPrefix + actualProp + " NS(" + actualNS + "), FORM (" + getAliasForm() + ")"
        }

        aliasMap[key] = aliasInfo
    }

    override fun getAliases(): Map<String, XMPAliasInfo> =
        aliasMap

    /**
     * Register the standard aliases.
     * Note: This method is not lock because only called by the constructor.
     */
    private fun registerStandardAliases() {

        val aliasToArrayOrdered = AliasOptions().setArrayOrdered(true)
        val aliasToArrayAltText = AliasOptions().setArrayAltText(true)

        // Aliases from XMP to DC.
        registerAlias(
            XMPConst.NS_XMP,
            "Author",
            XMPConst.NS_DC,
            "creator",
            aliasToArrayOrdered
        )
        registerAlias(
            XMPConst.NS_XMP,
            "Authors",
            XMPConst.NS_DC,
            "creator",
            null
        )
        registerAlias(
            XMPConst.NS_XMP,
            "Description",
            XMPConst.NS_DC,
            "description",
            null
        )
        registerAlias(
            XMPConst.NS_XMP,
            "Format",
            XMPConst.NS_DC,
            "format",
            null
        )
        registerAlias(
            XMPConst.NS_XMP,
            "Keywords",
            XMPConst.NS_DC,
            "subject",
            null
        )
        registerAlias(
            XMPConst.NS_XMP,
            "Locale",
            XMPConst.NS_DC,
            "language",
            null
        )
        registerAlias(
            XMPConst.NS_XMP,
            "Title",
            XMPConst.NS_DC,
            "title",
            null
        )
        registerAlias(
            XMPConst.NS_XMP_RIGHTS,
            "Copyright",
            XMPConst.NS_DC,
            "rights",
            null
        )

        // Aliases from PDF to DC and XMP.
        registerAlias(
            XMPConst.NS_PDF,
            "Author",
            XMPConst.NS_DC,
            "creator",
            aliasToArrayOrdered
        )
        registerAlias(
            XMPConst.NS_PDF,
            "BaseURL",
            XMPConst.NS_XMP,
            "BaseURL",
            null
        )
        registerAlias(
            XMPConst.NS_PDF,
            "CreationDate",
            XMPConst.NS_XMP,
            "CreateDate",
            null
        )
        registerAlias(
            XMPConst.NS_PDF,
            "Creator",
            XMPConst.NS_XMP,
            "CreatorTool",
            null
        )
        registerAlias(
            XMPConst.NS_PDF,
            "ModDate",
            XMPConst.NS_XMP,
            "ModifyDate",
            null
        )
        registerAlias(
            XMPConst.NS_PDF,
            "Subject",
            XMPConst.NS_DC,
            "description",
            aliasToArrayAltText
        )
        registerAlias(
            XMPConst.NS_PDF,
            "Title",
            XMPConst.NS_DC,
            "title",
            aliasToArrayAltText
        )

        // Aliases from PHOTOSHOP to DC and XMP.
        registerAlias(
            XMPConst.NS_PHOTOSHOP,
            "Author",
            XMPConst.NS_DC,
            "creator",
            aliasToArrayOrdered
        )
        registerAlias(
            XMPConst.NS_PHOTOSHOP,
            "Caption",
            XMPConst.NS_DC,
            "description",
            aliasToArrayAltText
        )
        registerAlias(
            XMPConst.NS_PHOTOSHOP,
            "Copyright",
            XMPConst.NS_DC,
            "rights",
            aliasToArrayAltText
        )
        registerAlias(
            XMPConst.NS_PHOTOSHOP,
            "Keywords",
            XMPConst.NS_DC,
            "subject",
            null
        )
        registerAlias(
            XMPConst.NS_PHOTOSHOP,
            "Marked",
            XMPConst.NS_XMP_RIGHTS,
            "Marked",
            null
        )
        registerAlias(
            XMPConst.NS_PHOTOSHOP,
            "Title",
            XMPConst.NS_DC,
            "title",
            aliasToArrayAltText
        )
        registerAlias(
            XMPConst.NS_PHOTOSHOP,
            "WebStatement",
            XMPConst.NS_XMP_RIGHTS,
            "WebStatement",
            null
        )

        // Aliases from TIFF and EXIF to DC and XMP.
        registerAlias(
            XMPConst.NS_TIFF,
            "Artist",
            XMPConst.NS_DC,
            "creator",
            aliasToArrayOrdered
        )
        registerAlias(
            XMPConst.NS_TIFF,
            "Copyright",
            XMPConst.NS_DC,
            "rights",
            null
        )
        registerAlias(
            XMPConst.NS_TIFF,
            "DateTime",
            XMPConst.NS_XMP,
            "ModifyDate",
            null
        )
        registerAlias(
            XMPConst.NS_EXIF,
            "DateTimeDigitized",
            XMPConst.NS_XMP,
            "CreateDate",
            null
        )
        registerAlias(
            XMPConst.NS_TIFF,
            "ImageDescription",
            XMPConst.NS_DC,
            "description",
            null
        )
        registerAlias(
            XMPConst.NS_TIFF,
            "Software",
            XMPConst.NS_XMP,
            "CreatorTool",
            null
        )

        // Aliases from PNG (Acrobat ImageCapture) to DC and XMP.
        registerAlias(
            XMPConst.NS_PNG,
            "Author",
            XMPConst.NS_DC,
            "creator",
            aliasToArrayOrdered
        )
        registerAlias(
            XMPConst.NS_PNG,
            "Copyright",
            XMPConst.NS_DC,
            "rights",
            aliasToArrayAltText
        )
        registerAlias(
            XMPConst.NS_PNG,
            "CreationTime",
            XMPConst.NS_XMP,
            "CreateDate",
            null
        )
        registerAlias(
            XMPConst.NS_PNG,
            "Description",
            XMPConst.NS_DC,
            "description",
            aliasToArrayAltText
        )
        registerAlias(
            XMPConst.NS_PNG,
            "ModificationTime",
            XMPConst.NS_XMP,
            "ModifyDate",
            null
        )
        registerAlias(
            XMPConst.NS_PNG,
            "Software",
            XMPConst.NS_XMP,
            "CreatorTool",
            null
        )
        registerAlias(
            XMPConst.NS_PNG,
            "Title",
            XMPConst.NS_DC,
            "title",
            aliasToArrayAltText
        )
    }
}
