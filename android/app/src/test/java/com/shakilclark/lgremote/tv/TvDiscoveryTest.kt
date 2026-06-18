package com.shakilclark.lgremote.tv

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Pure SSDP-response filtering (the network I/O is verified on a device). */
class TvDiscoveryTest {

    @Test
    fun `accepts LG webOS second-screen responses`() {
        val lg = """
            HTTP/1.1 200 OK
            LOCATION: http://192.168.0.9:1754/
            SERVER: WebOS/1.0 UPnP/1.0
            ST: urn:lge-com:service:webos-second-screen:1
        """.trimIndent()
        assertTrue(isLgWebosSsdp(lg))
    }

    @Test
    fun `rejects unrelated SSDP devices`() {
        val printer = """
            HTTP/1.1 200 OK
            LOCATION: http://192.168.0.50:80/desc.xml
            SERVER: Linux/3.0 UPnP/1.0 Printer/1.0
            ST: urn:schemas-upnp-org:device:Printer:1
        """.trimIndent()
        assertFalse(isLgWebosSsdp(printer))
    }
}
