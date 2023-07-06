// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.properties.XMPAliasInfo

/**
 * The schema registry keeps track of all namespaces and aliases used in the XMP
 * metadata. At initialisation time, the default namespaces and default aliases
 * are automatically registered. **Namespaces** must be registered before
 * used in namespace URI parameters or path expressions. Within the XMP Toolkit
 * the registered namespace URIs and prefixes must be unique. Additional
 * namespaces encountered when parsing RDF are automatically registered. The
 * namespace URI should always end in an XML name separator such as '/' or '#'.
 * This is because some forms of RDF shorthand catenate a namespace URI with an
 * element name to form a new URI.
 *
 * **Aliases** in XMP serve the same purpose as Windows file shortcuts,
 * Macintosh file aliases, or UNIX file symbolic links. The aliases are simply
 * multiple names for the same property. One distinction of XMP aliases is that
 * they are ordered, there is an alias name pointing to an actual name. The
 * primary significance of the actual name is that it is the preferred name for
 * output, generally the most widely recognized name.
 *
 * The names that can be aliased in XMP are restricted. The alias must be a top
 * level property name, not a field within a structure or an element within an
 * array. The actual may be a top level property name, the first element within
 * a top level array, or the default element in an alt-text array. This does not
 * mean the alias can only be a simple property. It is OK to alias a top level
 * structure or array to an identical top level structure or array, or to the
 * first item of an array of structures.
 */
interface XMPSchemaRegistry {

    // ---------------------------------------------------------------------------------------------
    // Namespace Functions

    /**
     * Register a namespace URI with a suggested prefix. It is not an error if
     * the URI is already registered, no matter what the prefix is. If the URI
     * is not registered but the suggested prefix is in use, a unique prefix is
     * created from the suggested one. The actual registeed prefix is always
     * returned. The function result tells if the registered prefix is the
     * suggested one.
     *
     * Note: No checking is presently done on either the URI or the prefix.
     *
     * @param namespaceURI    The URI for the namespace. Must be a valid XML URI.
     * @param suggestedPrefix The suggested prefix to be used if the URI is not yet registered.
     *                        Must be a valid XML name.
     * @return Returns the registered prefix for this URI, is equal to the suggestedPrefix if the
     *         namespace hasn't been registered before, otherwise the existing prefix.
     */
    fun registerNamespace(namespaceURI: String, suggestedPrefix: String): String

    /**
     * Obtain the prefix for a registered namespace URI.
     *
     * It is not an error if the namespace URI is not registered.
     *
     * @param namespaceURI The URI for the namespace. Must not be null or the empty string.
     * @return Returns the prefix registered for this namespace URI or null.
     */
    fun getNamespacePrefix(namespaceURI: String): String?

    /**
     * Obtain the URI for a registered namespace prefix.
     *
     * It is not an error if the namespace prefix is not registered.
     *
     * @param namespacePrefix The prefix for the namespace. Must not be null or the empty string.
     * @return Returns the URI registered for this prefix or null.
     */
    fun getNamespaceURI(namespacePrefix: String): String?

    /**
     * @return Returns the registered prefix/namespace-pairs as map, where the keys are the
     * namespaces and the values are the prefixes.
     */
    fun getNamespaces(): Map<String, String>

    /**
     * @return Returns the registered namespace/prefix-pairs as map, where the keys are the
     * prefixes and the values are the namespaces.
     */
    fun getPrefixes(): Map<String, String>

    /**
     * Deletes a namespace from the registry.
     *
     * Does nothing if the URI is not registered, or if the namespaceURI
     * parameter is null or the empty string.
     *
     * @param namespaceURI The URI for the namespace.
     */
    fun deleteNamespace(namespaceURI: String)

    // ---------------------------------------------------------------------------------------------
    // Alias Functions

    /**
     * Determines if a name is an alias, and what it is aliased to.
     *
     * @param aliasNS   The namespace URI of the alias. Must not be `null` or the empty string.
     * @param aliasProp The name of the alias.
     *                  May be an arbitrary path expression path, must not be `null` or the empty string.
     * @return Returns the `XMPAliasInfo` for the given alias namespace and property
     *         or `null` if there is no such alias.
     */
    fun resolveAlias(aliasNS: String, aliasProp: String): XMPAliasInfo?

    /**
     * Collects all aliases that are contained in the provided namespace.
     * If nothing is found, an empty array is returned.
     *
     * @param aliasNS a schema namespace URI
     * @return Returns all alias infos from aliases that are contained in the provided namespace.
     */
    fun findAliases(aliasNS: String): Set<XMPAliasInfo>

    /**
     * Searches for registered aliases.
     *
     * @param qname an XML conform qname
     * @return Returns if an alias definition for the given qname to another
     *         schema and property is registered.
     */
    fun findAlias(qname: String): XMPAliasInfo?

    /**
     * @return Returns the registered aliases as map, where the key is the "qname" (prefix and name)
     * and the value an `XMPAliasInfo`-object.
     */
    fun getAliases(): Map<String, XMPAliasInfo>

}
