package com.ashampoo.xmp

/** As used in Iptc4xmpExt */
public data class XMPLocation(

    /**
     * Iptc4xmpExt:LocationName
     *
     * For example "Times Square"
     */
    val name: String?,

    /**
     * Iptc4xmpExt:Sublocation
     *
     * For example "Schafjückenweg 2"
     */
    val location: String?,

    /**
     * Iptc4xmpExt:City
     * photoshop:City
     *
     * For example "Rastede"
     */
    val city: String?,

    /**
     * Iptc4xmpExt:ProvinceState
     * photoshop:State
     *
     * For example "Niedersachsen"
     */
    val state: String?,

    /**
     * Iptc4xmpExt:CountryName
     * photoshop:Country
     *
     * For example "Deutschland"
     */
    val country: String?
)
