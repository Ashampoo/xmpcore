package com.ashampoo.xmp

import com.ashampoo.xmp.Utils
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {

    @Test
    fun testReplaceControlCharsWithSpace() {

        /* Normal chars - nothing should be changed. */
        assertEquals(
            "Ashampoo GmbH & Co. KG",
            Utils.replaceControlCharsWithSpace("Ashampoo GmbH & Co. KG")
        )

        /* Random Control characters in between */
        assertEquals(
            "Ashampoo GmbH & Co. KG",
            Utils.replaceControlCharsWithSpace("Ashampoo\u0000GmbH\u001B&\nCo.\rKG")
        )

        /* Individual control characters */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0000")) // NUL
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0001")) // SOH
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0002")) // STX
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0003")) // ETX
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0004")) // EOT
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0005")) // ENQ
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0006")) // ACK
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0007")) // BEL
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0008")) // BS
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0009")) // HT
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000A")) // LF
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000B")) // VT
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000C")) // FF
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000D")) // CR
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000E")) // SO
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000F")) // SI
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0010")) // DLE
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0011")) // DC1
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0012")) // DC2
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0013")) // DC3
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0014")) // DC4
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0015")) // NAK
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0016")) // SYN
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0017")) // ETB
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0018")) // CAN
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0019")) // EM
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001A")) // SUB
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001B")) // ESC
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001C")) // FS
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001D")) // GS
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001E")) // RS
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001F")) // US
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u007F")) // DEL
    }
}
