// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.xpath

import com.ashampoo.xmp.Utils
import com.ashampoo.xmp.XMPError
import com.ashampoo.xmp.XMPException
import com.ashampoo.xmp.XMPMetaFactory.schemaRegistry

/**
 * Parser for XMP XPaths.
 */
object XMPPathParser {

    /**
     * Split an XMPPath expression apart at the conceptual steps, adding the
     * root namespace prefix to the first property component. The schema URI is
     * put in the first (0th) slot in the expanded XMPPath. Check if the top
     * level component is an alias, but don't resolve it.
     *
     * The logic is complicated though by shorthand for arrays, the separating
     * '/' and leading '*' are optional. These are all equivalent: array/ *[2]
     * array/[2] array*[2] array[2] All of these are broken into the 2 steps
     * "array" and "[2]".
     *
     * The value portion in the array selector forms is a string quoted by '''
     * or '"'. The value may contain any character including a doubled quoting
     * character. The value may be empty.
     *
     * The syntax isn't checked, but an XML name begins with a letter or '_',
     * and contains letters, digits, '.', '-', '_', and a bunch of special
     * non-ASCII Unicode characters. An XML qualified name is a pair of names
     * separated by a colon.
     */
    @kotlin.jvm.JvmStatic
    fun expandXPath(schemaNS: String?, path: String?): XMPPath {

        if (schemaNS == null || path == null)
            throw XMPException("Parameter must not be null", XMPError.BADPARAM)

        val expandedXPath = XMPPath()

        val pos = PathPosition()

        pos.path = path

        /*
         * Pull out the first component and do some special processing on it: add the schema
         * namespace prefix and see if it is an alias. The start must be a "qualName".
         */
        parseRootNode(schemaNS, pos, expandedXPath)

        /* Now continue to process the rest of the XMPPath string. */
        while (pos.stepEnd < path.length) {

            pos.stepBegin = pos.stepEnd

            skipPathDelimiter(path, pos)

            pos.stepEnd = pos.stepBegin

            var segment: XMPPathSegment

            segment = if (path[pos.stepBegin] != '[') {

                /* A struct field or qualifier. */
                parseStructSegment(pos)

            } else {

                /* One of the array forms. */
                parseIndexSegment(pos)
            }

            if (segment.kind == XMPPath.STRUCT_FIELD_STEP) {

                if (segment.name!![0] == '@') {

                    segment.name = "?" + segment.name!!.substring(1)

                    if ("?xml:lang" != segment.name)
                        throw XMPException("Only xml:lang allowed with '@'", XMPError.BADXPATH)
                }

                if (segment.name!![0] == '?') {

                    pos.nameStart++

                    segment.kind = XMPPath.QUALIFIER_STEP
                }

                verifyQualName(pos.path!!.substring(pos.nameStart, pos.nameEnd))

            } else if (segment.kind == XMPPath.FIELD_SELECTOR_STEP) {

                if (segment.name!![1] == '@') {

                    segment.name = "[?" + segment.name!!.substring(2)

                    if (!segment.name!!.startsWith("[?xml:lang="))
                        throw XMPException("Only xml:lang allowed with '@'", XMPError.BADXPATH)
                }

                if (segment.name!![1] == '?') {

                    pos.nameStart++

                    segment.kind = XMPPath.QUAL_SELECTOR_STEP

                    verifyQualName(pos.path!!.substring(pos.nameStart, pos.nameEnd))
                }
            }

            expandedXPath.add(segment)
        }

        return expandedXPath
    }

    private fun skipPathDelimiter(path: String, pos: PathPosition) {

        if (path[pos.stepBegin] == '/') {

            /* Skip the slash */
            pos.stepBegin++

            if (pos.stepBegin >= path.length)
                throw XMPException("Empty XMPPath segment", XMPError.BADXPATH)
        }

        if (path[pos.stepBegin] == '*') {

            /* Skip the asterisk */
            pos.stepBegin++

            if (pos.stepBegin >= path.length || path[pos.stepBegin] != '[')
                throw XMPException("Missing '[' after '*'", XMPError.BADXPATH)
        }
    }

    private fun parseStructSegment(pos: PathPosition): XMPPathSegment {

        pos.nameStart = pos.stepBegin

        while (pos.stepEnd < pos.path!!.length && "/[*".indexOf(pos.path!![pos.stepEnd]) < 0)
            pos.stepEnd++

        pos.nameEnd = pos.stepEnd

        if (pos.stepEnd == pos.stepBegin)
            throw XMPException("Empty XMPPath segment", XMPError.BADXPATH)

        return XMPPathSegment(
            pos.path!!.substring(pos.stepBegin, pos.stepEnd),
            XMPPath.STRUCT_FIELD_STEP
        )
    }

    /**
     * Parses an array index segment.
     */
    private fun parseIndexSegment(pos: PathPosition): XMPPathSegment {

        val segment: XMPPathSegment

        /* Look at the character after the leading '['. */
        pos.stepEnd++

        if (pos.path!![pos.stepEnd] in '0'..'9') {

            /* A numeric (decimal integer) array index. */
            while (
                pos.stepEnd < pos.path!!.length &&
                '0' <= pos.path!![pos.stepEnd] && pos.path!![pos.stepEnd] <= '9'
            )
                pos.stepEnd++

            segment = XMPPathSegment(null, XMPPath.ARRAY_INDEX_STEP)

        } else {

            /* Could be "[last()]" or one of the selector forms. Find the ']' or '='. */
            while (
                pos.stepEnd < pos.path!!.length && pos.path!![pos.stepEnd] != ']' &&
                pos.path!![pos.stepEnd] != '='
            )
                pos.stepEnd++

            if (pos.stepEnd >= pos.path!!.length)
                throw XMPException("Missing ']' or '=' for array index", XMPError.BADXPATH)

            if (pos.path!![pos.stepEnd] == ']') {

                if ("[last()" != pos.path!!.substring(pos.stepBegin, pos.stepEnd))
                    throw XMPException("Invalid non-numeric array index", XMPError.BADXPATH)

                segment = XMPPathSegment(null, XMPPath.ARRAY_LAST_STEP)

            } else {

                pos.nameStart = pos.stepBegin + 1
                pos.nameEnd = pos.stepEnd

                /* Absorb the '=', remember the quote. */
                pos.stepEnd++

                val quote = pos.path!![pos.stepEnd]

                if (quote != '\'' && quote != '"')
                    throw XMPException("Invalid quote in array selector", XMPError.BADXPATH)

                /* Absorb the leading quote */
                pos.stepEnd++

                while (pos.stepEnd < pos.path!!.length) {

                    if (pos.path!![pos.stepEnd] == quote) {

                        /* Check for escaped quote */
                        if (pos.stepEnd + 1 >= pos.path!!.length || pos.path!![pos.stepEnd + 1] != quote)
                            break

                        pos.stepEnd++
                    }

                    pos.stepEnd++
                }

                if (pos.stepEnd >= pos.path!!.length)
                    throw XMPException("No terminating quote for array selector", XMPError.BADXPATH)

                /* Absorb the trailing quote. */
                pos.stepEnd++

                /* ! Touch up later, also changing '@' to '?'. */
                segment = XMPPathSegment(null, XMPPath.FIELD_SELECTOR_STEP)
            }
        }

        if (pos.stepEnd >= pos.path!!.length || pos.path!![pos.stepEnd] != ']')
            throw XMPException("Missing ']' for array index", XMPError.BADXPATH)

        pos.stepEnd++

        segment.name = pos.path!!.substring(pos.stepBegin, pos.stepEnd)

        return segment
    }

    /**
     * Parses the root node of an XMP Path, checks if namespace and prefix fit together
     * and resolve the property to the base property if it is an alias.
     */
    private fun parseRootNode(schemaNS: String, pos: PathPosition, expandedXPath: XMPPath) {

        while (pos.stepEnd < pos.path!!.length && "/[*".indexOf(pos.path!![pos.stepEnd]) < 0)
            pos.stepEnd++

        if (pos.stepEnd == pos.stepBegin)
            throw XMPException("Empty initial XMPPath step", XMPError.BADXPATH)

        val rootProp = verifyXPathRoot(schemaNS, pos.path!!.substring(pos.stepBegin, pos.stepEnd))
        val aliasInfo = schemaRegistry.findAlias(rootProp)

        if (aliasInfo == null) {

            /* Add schema xpath step */
            expandedXPath.add(XMPPathSegment(schemaNS, XMPPath.SCHEMA_NODE))

            val rootStep = XMPPathSegment(rootProp, XMPPath.STRUCT_FIELD_STEP)

            expandedXPath.add(rootStep)

        } else {

            /* Add schema xpath step and base step of alias */
            expandedXPath.add(XMPPathSegment(aliasInfo.getNamespace(), XMPPath.SCHEMA_NODE))

            val rootStep = XMPPathSegment(
                verifyXPathRoot(aliasInfo.getNamespace(), aliasInfo.getPropName()),
                XMPPath.STRUCT_FIELD_STEP
            )

            rootStep.isAlias = true
            rootStep.aliasForm = aliasInfo.getAliasForm().getOptions()

            expandedXPath.add(rootStep)

            if (aliasInfo.getAliasForm().isArrayAltText()) {

                val qualSelectorStep =
                    XMPPathSegment("[?xml:lang='x-default']", XMPPath.QUAL_SELECTOR_STEP)

                qualSelectorStep.isAlias = true
                qualSelectorStep.aliasForm = aliasInfo.getAliasForm().getOptions()

                expandedXPath.add(qualSelectorStep)

            } else if (aliasInfo.getAliasForm().isArray()) {

                val indexStep = XMPPathSegment("[1]", XMPPath.ARRAY_INDEX_STEP)

                indexStep.isAlias = true
                indexStep.aliasForm = aliasInfo.getAliasForm().getOptions()

                expandedXPath.add(indexStep)
            }
        }
    }

    /**
     * Verifies whether the qualifier name is not XML conformant or the
     * namespace prefix has not been registered.
     */
    private fun verifyQualName(qualName: String) {

        val colonPos = qualName.indexOf(':')

        if (colonPos > 0) {

            val prefix = qualName.substring(0, colonPos)

            if (Utils.isXMLNameNS(prefix)) {

                val regURI = schemaRegistry.getNamespaceURI(prefix)

                if (regURI != null)
                    return

                throw XMPException("Unknown namespace prefix for qualified name", XMPError.BADXPATH)
            }
        }

        throw XMPException("Ill-formed qualified name: $qualName", XMPError.BADXPATH)
    }

    /**
     * Verify if an XML name is conformant.
     */
    private fun verifySimpleXMLName(name: String) {

        if (!Utils.isXMLName(name))
            throw XMPException("Bad XML name", XMPError.BADXPATH)
    }

    /**
     * Set up the first 2 components of the expanded XMPPath. Normalizes the various cases of using
     * the full schema URI and/or a qualified root property name. Returns true for normal
     * processing. If allowUnknownSchemaNS is true and the schema namespace is not registered, false
     * is returned. If allowUnknownSchemaNS is false and the schema namespace is not registered, an
     * exception is thrown
     */
    private fun verifyXPathRoot(schemaNS: String?, rootProp: String): String {

        /* Do some basic checks on the URI and name. Try to look up the URI. See if the name is qualified. */
        if (schemaNS.isNullOrEmpty())
            throw XMPException("Schema namespace URI is required", XMPError.BADSCHEMA)

        if (rootProp[0] == '?' || rootProp[0] == '@')
            throw XMPException(
                "Top level name must not be a qualifier, but was '$rootProp'",
                XMPError.BADXPATH
            )

        if (rootProp.indexOf('/') >= 0 || rootProp.indexOf('[') >= 0)
            throw XMPException("Top level name must be simple, but was '$rootProp'", XMPError.BADXPATH)

        var prefix = schemaRegistry.getNamespacePrefix(schemaNS)
            ?: throw XMPException("Unregistered schema namespace URI: $schemaNS", XMPError.BADSCHEMA)

        /* Verify the various URI and prefix combinations. Initialize the expanded XMPPath. */
        val colonPos = rootProp.indexOf(':')

        return if (colonPos < 0) {

            /* The propName is unqualified, use the schemaURI and associated prefix. */

            /* Verify the part before any colon */
            verifySimpleXMLName(rootProp)

            prefix + rootProp

        } else {

            /*
             * The propName is qualified. Make sure the prefix is legit.
             * Use the associated URI and qualified name.
             */

            /* Verify the part before any colon */
            verifySimpleXMLName(rootProp.substring(0, colonPos))
            verifySimpleXMLName(rootProp.substring(colonPos))

            prefix = rootProp.substring(0, colonPos + 1)

            val regPrefix = schemaRegistry.getNamespacePrefix(schemaNS)
                ?: throw XMPException("Unknown schema namespace prefix", XMPError.BADSCHEMA)

            if (prefix != regPrefix)
                throw XMPException("Schema namespace URI and prefix mismatch", XMPError.BADSCHEMA)

            rootProp
        }
    }
}
