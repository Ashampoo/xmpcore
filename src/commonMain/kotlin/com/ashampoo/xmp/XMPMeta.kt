// =================================================================================================
// ADOBE SYSTEMS INCORPORATED
// Copyright 2006 Adobe Systems Incorporated
// All Rights Reserved
//
// NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
// of the Adobe license agreement accompanying it.
// =================================================================================================
package com.ashampoo.xmp

import com.ashampoo.xmp.options.IteratorOptions
import com.ashampoo.xmp.options.ParseOptions
import com.ashampoo.xmp.options.PropertyOptions
import com.ashampoo.xmp.properties.XMPProperty

/**
 * This class represents the set of XMP metadata as a DOM representation. It has methods to read and
 * modify all kinds of properties, create an iterator over all properties and serialize the metadata
 * to a String, byte-array or `OutputStream`.
 */
@Suppress("ComplexInterface", "TooManyFunctions")
interface XMPMeta {

    // ---------------------------------------------------------------------------------------------
    // Basic property manipulation functions

    /**
     * The property value getter-methods all take a property specification: the first two parameters
     * are always the top level namespace URI (the &quot;schema&quot; namespace) and the basic name
     * of the property being referenced. See the introductory discussion of path expression usage
     * for more information.
     *
     * All the functions return an object inherited from `PropertyBase` or
     * `null` if the property does not exist. The result object contains the value of
     * the property and option flags describing the property. Arrays and the non-leaf levels of
     * nodes do not have values.
     *
     * See [PropertyOptions] for detailed information about the options.
     *
     * This is the simplest property getter, mainly for top level simple properties or after using
     * the path composition functions in XMPPathFactory.
     *
     * @param schemaNS The namespace URI for the property.
     *                 The URI must be for a registered namespace.
     * @param propName The name of the property. May be a general path expression,
     *                 must not be `null` or the empty string.
     *                 Using a namespace prefix on the first component is optional.
     *                 If present without a schemaNS value then the prefix specifies the namespace.
     *                 The prefix must be for a registered namespace.
     *                 If both a schemaNS URI and propName prefix are present,
     *                 they must be corresponding parts of a registered namespace.
     * @return Returns a `XMPProperty` containing the value and the options or `null`
     *         if the property does not exist.
     */
    fun getProperty(schemaNS: String, propName: String): XMPProperty?

    /**
     * Provides access to items within an array. The index is passed as an integer, you need not
     * worry about the path string syntax for array items, convert a loop index to a string, etc.
     *
     * @param schemaNS  The namespace URI for the array. Has the same usage as in `getProperty()`.
     * @param arrayName The name of the array. May be a general path expression,
     *                  must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in `getProperty()`.
     * @param itemIndex The index of the desired item. Arrays in XMP are indexed from 1.
     *                  The constant [XMPConst.ARRAY_LAST_ITEM] always refers to the last
     *                  existing array item.
     * @return Returns a `XMPProperty` containing the value and the options or
     *         `null` if the property does not exist.
     */
    fun getArrayItem(schemaNS: String, arrayName: String, itemIndex: Int): XMPProperty?

    /**
     * Returns the number of items in the array.
     *
     * @param schemaNS  The namespace URI for the array. Has the same usage as in getProperty.
     * @param arrayName The name of the array. May be a general path expression,
     *                  must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in `getProperty()`.
     * @return Returns the number of items in the array.
     */
    fun countArrayItems(schemaNS: String, arrayName: String): Int

    /**
     * Provides access to fields within a nested structure. The namespace for the field is passed as
     * a URI, you need not worry about the path string syntax.
     *
     * The names of fields should be XML qualified names, that is within an XML namespace. The path
     * syntax for a qualified name uses the namespace prefix. This is unreliable since the prefix is
     * never guaranteed. The URI is the formal name, the prefix is just a local shorthand in a given
     * sequence of XML text.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in getProperty.
     * @param structName The name of the struct.
     *                   May be a general path expression, must not be `null` or the empty string.
     *                   Has the same namespace prefix usage as propName in `getProperty()`.
     * @param fieldNS    The namespace URI for the field. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param fieldName  The name of the field. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * structName parameter.
     * @return Returns a `XMPProperty` containing the value and the options or
     * `null` if the property does not exist. Arrays and non-leaf levels of
     * structs do not have values.
     */
    fun getStructField(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String
    ): XMPProperty?

    /**
     * Provides access to a qualifier attached to a property. The namespace for the qualifier is
     * passed as a URI, you need not worry about the path string syntax. In many regards qualifiers
     * are like struct fields. See the introductory discussion of qualified properties for more
     * information.
     *
     * The names of qualifiers should be XML qualified names, that is within an XML namespace. The
     * path syntax for a qualified name uses the namespace prefix. This is unreliable since the
     * prefix is never guaranteed. The URI is the formal name, the prefix is just a local shorthand
     * in a given sequence of XML text.
     *
     * *Note:* Qualifiers are only supported for simple leaf properties.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in getProperty.
     * @param propName   The name of the struct.
     *                   May be a general path expression, must not be `null` or the empty string.
     *                   Has the same namespace prefix usage as propName in `getProperty()`.
     * @param qualNS   The namespace URI for the qualifier. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param qualName The name of the qualifier. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * propName parameter.
     * @return Returns a `XMPProperty` containing the value and the options of the
     * qualifier or `null` if the property does not exist. The name of the
     * qualifier must be a single XML name, must not be `null` or the empty
     * string. Has the same namespace prefix usage as the propName parameter.
     *
     * The value of the qualifier is only set if it has one (Arrays and non-leaf levels of
     * structs do not have values).
     */
    fun getQualifier(
        schemaNS: String,
        propName: String,
        qualNS: String,
        qualName: String
    ): XMPProperty?

    // ---------------------------------------------------------------------------------------------
    // Functions for setting property values

    /**
     * The property value `setters` all take a property specification, their
     * differences are in the form of this. The first two parameters are always the top level
     * namespace URI (the `schema` namespace) and the basic name of the property being
     * referenced. See the introductory discussion of path expression usage for more information.
     *
     * All of the functions take a string value for the property and option flags describing the
     * property. The value must be Unicode in UTF-8 encoding. Arrays and non-leaf levels of structs
     * do not have values. Empty arrays and structs may be created using appropriate option flags.
     * All levels of structs that is assigned implicitly are created if necessary. appendArayItem
     * implicitly creates the named array if necessary.
     *
     * See [PropertyOptions] for detailed information about the options.
     *
     * This is the simplest property setter, mainly for top level simple properties or after using
     * the path composition functions in [XMPPathFactory].
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in getProperty.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the value for the property (only leaf properties have a value).
     * Arrays and non-leaf levels of structs do not have values.
     * Must be `null` if the value is not relevant.
     * The value is automatically detected: Boolean, Integer, Long, Double, XMPDateTime and
     * byte[] are handled, on all other `toString()` is called.
     * @param options   Option flags describing the property. See the earlier description.
     */
    fun setProperty(
        schemaNS: String,
        propName: String,
        propValue: Any?,
        options: PropertyOptions = PropertyOptions()
    )

    /**
     * Replaces an item within an array. The index is passed as an integer, you need not worry about
     * the path string syntax for array items, convert a loop index to a string, etc. The array
     * passed must already exist. In normal usage the selected array item is modified. A new item is
     * automatically appended if the index is the array size plus 1.
     *
     * @param schemaNS  The namespace URI for the struct. Has the same usage as in getProperty.
     * @param arrayName The name of the array.
     *                  May be a general path expression, must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in getProperty.
     * @param itemIndex The index of the desired item. Arrays in XMP are indexed from 1. To address
     * the last existing item, use [XMPMeta.countArrayItems] to find
     * out the length of the array.
     * @param itemValue the new value of the array item. Has the same usage as propValue in
     * `setProperty()`.
     * @param options   the set options for the item.
     */
    fun setArrayItem(
        schemaNS: String,
        arrayName: String,
        itemIndex: Int,
        itemValue: String,
        options: PropertyOptions = PropertyOptions()
    )

    /**
     * Inserts an item into an array previous to the given index. The index is passed as an integer,
     * you need not worry about the path string syntax for array items, convert a loop index to a
     * string, etc. The array passed must already exist. In normal usage the selected array item is
     * modified. A new item is automatically appended if the index is the array size plus 1.
     *
     * @param schemaNS  The namespace URI for the struct. Has the same usage as in getProperty.
     * @param arrayName The name of the array.
     *                  May be a general path expression, must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in getProperty.
     * @param itemIndex The index to insert the new item. Arrays in XMP are indexed from 1. Use
     * `XMPConst.ARRAY_LAST_ITEM` to append items.
     * @param itemValue the new value of the array item. Has the same usage as
     * propValue in `setProperty()`.
     * @param options   the set options that decide about the kind of the node.
     */
    fun insertArrayItem(
        schemaNS: String,
        arrayName: String,
        itemIndex: Int,
        itemValue: String,
        options: PropertyOptions = PropertyOptions()
    )

    /**
     * Simplifies the construction of an array by not requiring that you pre-create an empty array.
     * The array that is assigned is created automatically if it does not yet exist. Each call to
     * appendArrayItem() appends an item to the array. The corresponding parameters have the same
     * use as setArrayItem(). The arrayOptions parameter is used to specify what kind of array. If
     * the array exists, it must have the specified form.
     *
     * @param schemaNS  The namespace URI for the struct. Has the same usage as in getProperty.
     * @param arrayName The name of the array.
     *                  May be a general path expression, must not be `null` or the empty string.
     *                  Has the same namespace prefix usage as propName in getProperty.
     * @param arrayOptions Option flags describing the array form. The only valid options are
     *
     *  *  [PropertyOptions.ARRAY],
     *  *  [PropertyOptions.ARRAY_ORDERED],
     *  *  [PropertyOptions.ARRAY_ALTERNATE] or
     *  *  [PropertyOptions.ARRAY_ALT_TEXT].
     *
     * *Note:* the array options only need to be provided if the array is not
     * already existing, otherwise you can set them to `null` or use [XMPMeta.appendArrayItem].
     *
     * @param itemValue    the value of the array item. Has the same usage as propValue in getProperty.
     * @param itemOptions  Option flags describing the item to append ([PropertyOptions])
     */
    fun appendArrayItem(
        schemaNS: String,
        arrayName: String,
        arrayOptions: PropertyOptions = PropertyOptions(),
        itemValue: String,
        itemOptions: PropertyOptions = PropertyOptions()
    )

    /**
     * Provides access to fields within a nested structure. The namespace for the field is passed as
     * a URI, you need not worry about the path string syntax. The names of fields should be XML
     * qualified names, that is within an XML namespace. The path syntax for a qualified name uses
     * the namespace prefix, which is unreliable because the prefix is never guaranteed. The URI is
     * the formal name, the prefix is just a local shorthand in a given sequence of XML text.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in getProperty.
     * @param structName The name of the struct. May be a general path expression, must not be null
     * or the empty string. Has the same namespace prefix usage as propName in getProperty.
     * @param fieldNS    The namespace URI for the field. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param fieldName  The name of the field. Must be a single XML name, must not be null or the
     * empty string. Has the same namespace prefix usage as the structName parameter.
     * @param fieldValue the value of thefield, if the field has a value.
     * Has the same usage as propValue in getProperty.
     * @param options    Option flags describing the field. See the earlier description.
     */
    fun setStructField(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String,
        fieldValue: String?,
        options: PropertyOptions = PropertyOptions()
    )

    /**
     * Provides access to a qualifier attached to a property. The namespace for the qualifier is
     * passed as a URI, you need not worry about the path string syntax. In many regards qualifiers
     * are like struct fields. See the introductory discussion of qualified properties for more
     * information. The names of qualifiers should be XML qualified names, that is within an XML
     * namespace. The path syntax for a qualified name uses the namespace prefix, which is
     * unreliable because the prefix is never guaranteed. The URI is the formal name, the prefix is
     * just a local shorthand in a given sequence of XML text. The property the qualifier
     * will be attached has to exist.
     *
     * @param schemaNS  The namespace URI for the struct. Has the same usage as in getProperty.
     * @param propName  The name of the property to which the qualifier is attached. Has the same
     * usage as in getProperty.
     * @param qualNS    The namespace URI for the qualifier. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param qualName  The name of the qualifier. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * propName parameter.
     * @param qualValue A pointer to the `null` terminated UTF-8 string that is the
     * value of the qualifier, if the qualifier has a value. Has the same usage as propValue
     * in getProperty.
     * @param options   Option flags describing the qualifier. See the earlier description.
     */
    fun setQualifier(
        schemaNS: String,
        propName: String,
        qualNS: String,
        qualName: String,
        qualValue: String,
        options: PropertyOptions = PropertyOptions()
    )

    // ---------------------------------------------------------------------------------------------
    // Functions for deleting and detecting properties.
    // These should be obvious from the descriptions of the getters and setters.

    /**
     * Deletes the given XMP subtree rooted at the given property.
     * It is not an error if the property does not exist.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in getProperty.
     */
    fun deleteProperty(schemaNS: String, propName: String)

    /**
     * Deletes the given XMP subtree rooted at the given array item.
     * It is not an error if the array item does not exist.
     *
     * @param schemaNS  The namespace URI for the array. Has the same usage as in getProperty.
     * @param arrayName The name of the array. May be a general path expression, must not be
     * `null` or the empty string. Has the same namespace prefix usage as
     * propName in `getProperty()`.
     * @param itemIndex The index of the desired item. Arrays in XMP are indexed from 1. The
     * constant `XMPConst.ARRAY_LAST_ITEM` always refers to the last
     * existing array item.
     */
    fun deleteArrayItem(schemaNS: String, arrayName: String, itemIndex: Int)

    /**
     * Deletes the given XMP subtree rooted at the given struct field.
     * It is not an error if the field does not exist.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in `getProperty()`.
     * @param structName The name of the struct. May be a general path expression, must not be
     * `null` or the empty string. Has the same namespace prefix usage as
     * propName in getProperty.
     * @param fieldNS    The namespace URI for the field. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param fieldName  The name of the field. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * structName parameter.
     */
    fun deleteStructField(schemaNS: String, structName: String, fieldNS: String, fieldName: String)

    /**
     * Deletes the given XMP subtree rooted at the given qualifier.
     * It is not an error if the qualifier does not exist.
     *
     * @param schemaNS The namespace URI for the struct. Has the same usage as in `getProperty()`.
     * @param propName The name of the property to which the qualifier is attached. Has the same
     * usage as in getProperty.
     * @param qualNS   The namespace URI for the qualifier. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param qualName The name of the qualifier. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * propName parameter.
     */
    fun deleteQualifier(schemaNS: String, propName: String, qualNS: String, qualName: String)

    /**
     * Returns whether the property exists.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns true if the property exists.
     */
    fun doesPropertyExist(schemaNS: String, propName: String): Boolean

    /**
     * Tells if the array item exists.
     *
     * @param schemaNS  The namespace URI for the array. Has the same usage as in `getProperty()`.
     * @param arrayName The name of the array. May be a general path expression, must not be
     * `null` or the empty string. Has the same namespace prefix usage as
     * propName in `getProperty()`.
     * @param itemIndex The index of the desired item. Arrays in XMP are indexed from 1. The
     * constant `XMPConst.ARRAY_LAST_ITEM` always refers to the last
     * existing array item.
     * @return Returns `true` if the array exists, `false` otherwise.
     */
    fun doesArrayItemExist(schemaNS: String, arrayName: String, itemIndex: Int): Boolean

    /**
     * Tells if the struct field exists.
     *
     * @param schemaNS   The namespace URI for the struct. Has the same usage as in `getProperty()`.
     * @param structName The name of the struct. May be a general path expression,
     *                   must not be `null` or the empty string.
     *                   Has the same namespace prefix usage as propName in `getProperty()`.
     * @param fieldNS    The namespace URI for the field.
     *                   Has the same URI and prefix usage as the schemaNS parameter.
     * @param fieldName  The name of the field. Must be a single XML name,
     *                   must not be `null` or the empty string.
     *                   Has the same namespace prefix usage as the structName parameter.
     * @return Returns true if the field exists.
     */
    fun doesStructFieldExist(
        schemaNS: String,
        structName: String,
        fieldNS: String,
        fieldName: String
    ): Boolean

    /**
     * Tells if the qualifier exists.
     *
     * @param schemaNS The namespace URI for the struct. Has the same usage as in `getProperty()`.
     * @param propName The name of the property to which the qualifier is attached. Has the same
     * usage as in `getProperty()`.
     * @param qualNS   The namespace URI for the qualifier. Has the same URI and prefix usage as the
     * schemaNS parameter.
     * @param qualName The name of the qualifier. Must be a single XML name, must not be
     * `null` or the empty string. Has the same namespace prefix usage as the
     * propName parameter.
     * @return Returns true if the qualifier exists.
     */
    fun doesQualifierExist(schemaNS: String, propName: String, qualNS: String, qualName: String): Boolean

    // ---------------------------------------------------------------------------------------------
    // Specialized Get and Set functions

    /**
     * These functions provide convenient support for localized text properties, including a number
     * of special and obscure aspects. Localized text properties are stored in alt-text arrays. They
     * allow multiple concurrent localizations of a property value, for example a document title or
     * copyright in several languages. The most important aspect of these functions is that they
     * select an appropriate array item based on one or two RFC 3066 language tags. One of these
     * languages, the "specific" language, is preferred and selected if there is an exact match. For
     * many languages it is also possible to define a "generic" language that may be used if there
     * is no specific language match. The generic language must be a valid RFC 3066 primary subtag,
     * or the empty string. For example, a specific language of "en-US" should be used in the US,
     * and a specific language of "en-UK" should be used in England. It is also appropriate to use
     * "en" as the generic language in each case. If a US document goes to England, the "en-US"
     * title is selected by using the "en" generic language and the "en-UK" specific language. It is
     * considered poor practice, but allowed, to pass a specific language that is just an RFC 3066
     * primary tag. For example "en" is not a good specific language, it should only be used as a
     * generic language. Passing "i" or "x" as the generic language is also considered poor practice
     * but allowed. Advice from the W3C about the use of RFC 3066 language tags can be found at:
     * http://www.w3.org/International/articles/language-tags/
     *
     * *Note:* RFC 3066 language tags must be treated in a case insensitive manner. The XMP
     * Toolkit does this by normalizing their capitalization:
     *
     *  *  The primary subtag is lower case, the suggested practice of ISO 639.
     *  *  All 2 letter secondary subtags are upper case, the suggested practice of ISO 3166.
     *  *  All other subtags are lower case. The XMP specification defines an artificial language,
     *  * "x-default", that is used to explicitly denote a default item in an alt-text array.
     *
     * The XMP toolkit normalizes alt-text arrays such that the x-default item is the first item.
     * The SetLocalizedText function has several special features related to the x-default item, see
     * its description for details. The selection of the array item is the same for GetLocalizedText
     * and SetLocalizedText:
     *
     *  *  Look for an exact match with the specific language.
     *  *  If a generic language is given, look for a partial match.
     *  *  Look for an x-default item.
     *  *  Choose the first item.
     *
     * A partial match with the generic language is where the start of the item's language matches
     * the generic string and the next character is '-'. An exact match is also recognized as a
     * degenerate case. It is fine to pass x-default as the specific language. In this case,
     * selection of an x-default item is an exact match by the first rule, not a selection by the
     * 3rd rule. The last 2 rules are fallbacks used when the specific and generic languages fail to
     * produce a match. `getLocalizedText` returns information about a selected item in
     * an alt-text array. The array item is selected according to the rules given above.
     *
     * @param schemaNS     The namespace URI for the alt-text array. Has the same usage as in `getProperty()`.
     * @param altTextName  The name of the alt-text array. May be a general path expression, must not
     * be `null` or the empty string. Has the same namespace prefix usage as
     * propName in `getProperty()`.
     * @param genericLang  The name of the generic language as an RFC 3066 primary subtag. May be
     * `null` or the empty string if no generic language is wanted.
     * @param specificLang The name of the specific language as an RFC 3066 tag. Must not be
     * `null` or the empty string.
     * @return Returns an `XMPProperty` containing the value, the actual language and
     * the options if an appropriate alternate collection item exists, `null`
     * if the property.
     * does not exist.
     *
     */
    fun getLocalizedText(
        schemaNS: String,
        altTextName: String,
        genericLang: String?,
        specificLang: String
    ): XMPProperty?

    /**
     * Modifies the value of a selected item in an alt-text array. Creates an appropriate array item
     * if necessary, and handles special cases for the x-default item. If the selected item is from
     * a match with the specific language, the value of that item is modified. If the existing value
     * of that item matches the existing value of the x-default item, the x-default item is also
     * modified. If the array only has 1 existing item (which is not x-default), an x-default item
     * is added with the given value. If the selected item is from a match with the generic language
     * and there are no other generic matches, the value of that item is modified. If the existing
     * value of that item matches the existing value of the x-default item, the x-default item is
     * also modified. If the array only has 1 existing item (which is not x-default), an x-default
     * item is added with the given value. If the selected item is from a partial match with the
     * generic language and there are other partial matches, a new item is created for the specific
     * language. The x-default item is not modified. If the selected item is from the last 2 rules
     * then a new item is created for the specific language. If the array only had an x-default
     * item, the x-default item is also modified. If the array was empty, items are created for the
     * specific language and x-default.
     *
     * @param schemaNS     The namespace URI for the alt-text array. Has the same usage as in `getProperty()`.
     * @param altTextName  The name of the alt-text array. May be a general path expression,
     *                     must not be `null` or the empty string.
     *                     Has the same namespace prefix usage as propName in `getProperty()`.
     * @param genericLang  The name of the generic language as an RFC 3066 primary subtag.
     *                     May be `null` or the empty string if no generic language is wanted.
     * @param specificLang The name of the specific language as an RFC 3066 tag.
     *                     Must not be `null` or the empty string.
     * @param itemValue    A pointer to the `null` terminated UTF-8 string that is the new
     *                     value for the appropriate array item.
     * @param options      Option flags, none are defined at present.
     */
    fun setLocalizedText(
        schemaNS: String,
        altTextName: String,
        genericLang: String?,
        specificLang: String,
        itemValue: String,
        options: PropertyOptions = PropertyOptions()
    )

    // ---------------------------------------------------------------------------------------------
    // Functions accessing properties as binary values.

    /**
     * These are very similar to `getProperty()` and `SetProperty()` above,
     * but the value is returned or provided in a literal form instead of as a UTF-8 string.
     * The path composition functions in `XMPPathFactory` may be used to compose an path
     * expression for fields in nested structures, items in arrays, or qualifiers.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `Boolean` value or `null` if the property does not exist.
     */
    fun getPropertyBoolean(schemaNS: String, propName: String): Boolean?

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns an `Integer` value or `null` if the property does not exist.
     */
    fun getPropertyInteger(schemaNS: String, propName: String): Int?

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `Long` value or `null` if the property does not exist.
     */
    fun getPropertyLong(schemaNS: String, propName: String): Long?

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `Double` value or `null` if the property does not exist.
     */
    fun getPropertyDouble(schemaNS: String, propName: String): Double?

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `byte[]`-array contained the decoded base64 value or `null` if the property does
     * not exist.
     */
    fun getPropertyBase64(schemaNS: String, propName: String): ByteArray?

    /**
     * Convenience method to retrieve the literal value of a property.
     *
     * *Note:* There is no `setPropertyString()`,
     * because `setProperty()` sets a string value.
     *
     * @param schemaNS The namespace URI for the property. Has the same usage as in `getProperty()`.
     * @param propName The name of the property. Has the same usage as in `getProperty()`.
     * @return Returns a `String` value or `null` if the property does not exist.
     */
    fun getPropertyString(schemaNS: String, propName: String): String?

    /**
     * Convenience method to set a property to a literal `boolean` value.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as `boolean`.
     * @param options   options of the property to set (optional).
     */
    fun setPropertyBoolean(
        schemaNS: String,
        propName: String,
        propValue: Boolean,
        options: PropertyOptions = PropertyOptions()
    )

    /**
     * Convenience method to set a property to a literal `int` value.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as `int`.
     * @param options   options of the property to set (optional).
     *
     */
    fun setPropertyInteger(
        schemaNS: String,
        propName: String,
        propValue: Int,
        options: PropertyOptions = PropertyOptions()
    )

    /**
     * Convenience method to set a property to a literal `long` value.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as `long`.
     * @param options   options of the property to set (optional).
     */
    fun setPropertyLong(
        schemaNS: String,
        propName: String,
        propValue: Long,
        options: PropertyOptions = PropertyOptions()
    )

    /**
     * Convenience method to set a property to a literal `double` value.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as `double`.
     * @param options   options of the property to set (optional).
     */
    fun setPropertyDouble(
        schemaNS: String,
        propName: String,
        propValue: Double,
        options: PropertyOptions = PropertyOptions()
    )

    /**
     * Convenience method to set a property from a binary `byte[]`-array,
     * which is serialized as base64-string.
     *
     * @param schemaNS  The namespace URI for the property. Has the same usage as in `setProperty()`.
     * @param propName  The name of the property. Has the same usage as in `getProperty()`.
     * @param propValue the literal property value as byte array.
     * @param options   options of the property to set (optional).
     */
    fun setPropertyBase64(
        schemaNS: String,
        propName: String,
        propValue: ByteArray,
        options: PropertyOptions = PropertyOptions()
    )

    /**
     * Constructs an iterator for the properties within this XMP object.
     *
     * @return Returns an `XMPIterator`.
     */
    fun iterator(): XMPIterator

    /**
     * Constructs an iterator for the properties within this XMP object using some options.
     *
     * @param options Option flags to control the iteration.
     * @return Returns an `XMPIterator`.
     */
    fun iterator(options: IteratorOptions): XMPIterator

    /**
     * Construct an iterator for the properties within an XMP object. According to the parameters it iterates
     * the entire data tree, properties within a specific schema, or a subtree rooted at a specific node.
     *
     * @param schemaNS Optional schema namespace URI to restrict the iteration.
     *                 Omitted (visit all schema) by passing `null` or empty String.
     * @param propName Optional property name to restrict the iteration. May be an arbitrary path
     *                 expression. Omitted (visit all properties) by passing `null` or empty
     *                 String. If no schema URI is given, it is ignored.
     * @param options  Option flags to control the iteration. See [IteratorOptions] for details.
     * @return Returns an `XMPIterator` for this `XMPMeta`-object considering the given options.
     */
    fun iterator(
        schemaNS: String?,
        propName: String?,
        options: IteratorOptions = IteratorOptions()
    ): XMPIterator

    /**
     * This correlates to the about-attribute,
     * returns the empty String if no name is set.
     *
     * @return Returns the name of the XMP object.
     */
    fun getObjectName(): String

    /**
     * @param name Sets the name of the XMP object.
     */
    fun setObjectName(name: String)

    /**
     * @return Returns the unparsed content of the &lt;?xpacket&gt; processing instruction.
     * This contains normally the attribute-like elements 'begin="&lt;BOM&gt;"
     * id="W5M0MpCehiHzreSzNTczkc9d"' and possibly the deprecated elements 'bytes="1234"' or
     * 'encoding="XXX"'. If the parsed packet has not been wrapped into an xpacket,
     * `null` is returned.
     */
    fun getPacketHeader(): String?

    /**
     * Sorts the complete datamodel according to the following rules:
     *
     *  * Schema nodes are sorted by prefix.
     *  * Properties at top level and within structs are sorted by full name, that is prefix + local name.
     *  * Array items are not sorted, even if they have no certain order such as bags.
     *  * Qualifier are sorted, with the exception of "xml:lang" and/or "rdf:type"
     * that stay at the top of the list in that order.
     */
    fun sort()

    /**
     * Perform the normalization as a separate parsing step.
     * Normally it is done during parsing, unless the parsing option
     * [ParseOptions.OMIT_NORMALIZATION] is set to `true`.
     * *Note:* It does no harm to call this method to an already normalized xmp object.
     * It was a PDF/A requirement to get hand on the unnormalized `XMPMeta` object.
     */
    fun normalize(options: ParseOptions = ParseOptions())

    fun printAllToConsole()

}
