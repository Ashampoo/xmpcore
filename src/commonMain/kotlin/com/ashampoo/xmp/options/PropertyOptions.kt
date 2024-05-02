// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp.options

import com.ashampoo.xmp.XMPErrorConst
import com.ashampoo.xmp.XMPException

/**
 * The property flags are used when properties are fetched from the `XMPMeta`-object
 * and provide more detailed information about the property.
 */
public class PropertyOptions : Options {

    /**
     * Default constructor
     */
    constructor()

    /**
     * Intialization constructor
     *
     * @param options the initialization options
     *
     */
    constructor(options: Int) : super(options)

    /**
     * @return Return whether the property value is a URI. It is serialized to RDF using the
     * <tt>rdf:resource</tt> attribute. Not mandatory for URIs, but considered RDF-savvy.
     */
    fun isURI(): Boolean = getOption(URI)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setURI(value: Boolean): PropertyOptions {
        setOption(URI, value)
        return this
    }

    /**
     * @return Return whether the property has qualifiers. These could be an <tt>xml:lang</tt>
     * attribute, an <tt>rdf:type</tt> property, or a general qualifier. See the
     * introductory discussion of qualified properties for more information.
     */
    fun hasQualifiers(): Boolean =
        getOption(HAS_QUALIFIERS)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setHasQualifiers(value: Boolean): PropertyOptions {
        setOption(HAS_QUALIFIERS, value)
        return this
    }

    /**
     * @return Return whether this property is a qualifier for some other property. Note that if the
     * qualifier itself has a structured value, this flag is only set for the top node of
     * the qualifier's subtree. Qualifiers may have arbitrary structure, and may even have
     * qualifiers.
     */
    fun isQualifier(): Boolean =
        getOption(QUALIFIER)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setQualifier(value: Boolean): PropertyOptions {
        setOption(QUALIFIER, value)
        return this
    }

    /**
     * @return Return whether this property has an <tt>xml:lang</tt> qualifier.
     */
    fun hasLanguage(): Boolean =
        getOption(HAS_LANGUAGE)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setHasLanguage(value: Boolean): PropertyOptions {
        setOption(HAS_LANGUAGE, value)
        return this
    }

    /**
     * @return Return whether this property has an <tt>rdf:type</tt> qualifier.
     */
    fun hasType(): Boolean =
        getOption(HAS_TYPE)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setHasType(value: Boolean): PropertyOptions {
        setOption(HAS_TYPE, value)
        return this
    }

    /**
     * @return Return whether this property contains nested fields.
     */
    fun isStruct(): Boolean =
        getOption(STRUCT)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setStruct(value: Boolean): PropertyOptions {
        setOption(STRUCT, value)
        return this
    }

    /**
     * @return Return whether this property is an array. By itself this indicates a general
     * unordered array. It is serialized using an <tt>rdf:Bag</tt> container.
     */
    fun isArray(): Boolean =
        getOption(ARRAY)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setArray(value: Boolean): PropertyOptions {
        setOption(ARRAY, value)
        return this
    }

    /**
     * @return Return whether this property is an ordered array. Appears in conjunction with
     * getPropValueIsArray(). It is serialized using an <tt>rdf:Seq</tt> container.
     */
    fun isArrayOrdered(): Boolean =
        getOption(ARRAY_ORDERED)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setArrayOrdered(value: Boolean): PropertyOptions {
        setOption(ARRAY_ORDERED, value)
        return this
    }

    /**
     * @return Return whether this property is an alternative array. Appears in conjunction with
     * getPropValueIsArray(). It is serialized using an <tt>rdf:Alt</tt> container.
     */
    fun isArrayAlternate(): Boolean =
        getOption(ARRAY_ALTERNATE)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setArrayAlternate(value: Boolean): PropertyOptions {
        setOption(ARRAY_ALTERNATE, value)
        return this
    }

    /**
     * @return Return whether this property is an alt-text array. Appears in conjunction with
     * getPropArrayIsAlternate(). It is serialized using an <tt>rdf:Alt</tt> container.
     * Each array element is a simple property with an <tt>xml:lang</tt> attribute.
     */
    fun isArrayAltText(): Boolean =
        getOption(ARRAY_ALT_TEXT)

    /**
     * @param value the value to set
     * @return Returns this to enable cascaded options.
     */
    fun setArrayAltText(value: Boolean): PropertyOptions {
        setOption(ARRAY_ALT_TEXT, value)
        return this
    }

    /**
     * @return Returns whether the SCHEMA_NODE option is set.
     */
    fun isSchemaNode(): Boolean =
        getOption(SCHEMA_NODE)

    /**
     * @param value the option DELETE_EXISTING to set
     * @return Returns this to enable cascaded options.
     */
    fun setSchemaNode(value: Boolean): PropertyOptions {
        setOption(SCHEMA_NODE, value)
        return this
    }

    /**
     * @return Returns whether the property is of composite type - an array or a struct.
     */
    fun isCompositeProperty(): Boolean =
        getOptions() and (ARRAY or STRUCT) > 0

    /**
     * @return Returns whether the property is of composite type - an array or a struct.
     */
    fun isSimple(): Boolean =
        getOptions() and (ARRAY or STRUCT) == 0

    /**
     * Compares two options set for array compatibility.
     *
     * @param options other options
     * @return Returns true if the array options of the sets are equal.
     */
    fun equalArrayTypes(options: PropertyOptions): Boolean =
        isArray() == options.isArray() &&
            isArrayOrdered() == options.isArrayOrdered() &&
            isArrayAlternate() == options.isArrayAlternate() &&
            isArrayAltText() == options.isArrayAltText()

    /**
     * Merges the set options of a another options object with this.
     * If the other options set is null, this objects stays the same.
     *
     * @param options other options
     */
    fun mergeWith(options: PropertyOptions) {
        setOptions(getOptions() or options.getOptions())
    }

    /**
     * @return Returns true if only array options are set.
     */
    fun isOnlyArrayOptions(): Boolean =
        getOptions() and (ARRAY or ARRAY_ORDERED or ARRAY_ALTERNATE or ARRAY_ALT_TEXT).inv() == 0

    /**
     * @see Options.getValidOptions
     */
    override fun getValidOptions(): Int =
        URI or HAS_QUALIFIERS or QUALIFIER or HAS_LANGUAGE or HAS_TYPE or STRUCT or ARRAY or
            ARRAY_ORDERED or ARRAY_ALTERNATE or ARRAY_ALT_TEXT or DELETE_EXISTING or SCHEMA_NODE

    /**
     * @see Options.defineOptionName
     */
    override fun defineOptionName(option: Int): String? {
        return when (option) {
            URI -> "URI"
            HAS_QUALIFIERS -> "HAS_QUALIFIER"
            QUALIFIER -> "QUALIFIER"
            HAS_LANGUAGE -> "HAS_LANGUAGE"
            HAS_TYPE -> "HAS_TYPE"
            STRUCT -> "STRUCT"
            ARRAY -> "ARRAY"
            ARRAY_ORDERED -> "ARRAY_ORDERED"
            ARRAY_ALTERNATE -> "ARRAY_ALTERNATE"
            ARRAY_ALT_TEXT -> "ARRAY_ALT_TEXT"
            SCHEMA_NODE -> "SCHEMA_NODE"
            else -> null
        }
    }

    /**
     * Checks that a node not a struct and array at the same time;
     * and URI cannot be a struct.
     *
     * @param options the bitmask to check.
     *
     */
    public override fun assertConsistency(options: Int) {

        if (options and STRUCT > 0 && options and ARRAY > 0)
            throw XMPException("IsStruct and IsArray options are mutually exclusive", XMPErrorConst.BADOPTIONS)
        else if (options and URI > 0 && options and (ARRAY or STRUCT) > 0)
            throw XMPException("Structs and arrays can't have \"value\" options", XMPErrorConst.BADOPTIONS)
    }

    companion object {

        /**
         *
         */
        const val NO_OPTIONS = 0x00000000

        /**
         *
         */
        const val URI = 0x00000002

        /**
         *
         */
        const val HAS_QUALIFIERS = 0x00000010

        /**
         *
         */
        const val QUALIFIER = 0x00000020

        /**
         *
         */
        const val HAS_LANGUAGE = 0x00000040

        /**
         *
         */
        const val HAS_TYPE = 0x00000080

        /**
         *
         */
        const val STRUCT = 0x00000100

        /**
         *
         */
        const val ARRAY = 0x00000200

        /**
         *
         */
        const val ARRAY_ORDERED = 0x00000400

        /**
         *
         */
        const val ARRAY_ALTERNATE = 0x00000800

        /**
         *
         */
        const val ARRAY_ALT_TEXT = 0x00001000

        /**
         *
         */
        const val SCHEMA_NODE = -0x80000000

        /**
         * may be used in the future
         */
        const val DELETE_EXISTING = 0x20000000
    }
}
