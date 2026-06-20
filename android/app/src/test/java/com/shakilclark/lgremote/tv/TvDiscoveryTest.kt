package com.shakilclark.lgremote.tv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Pure SSDP filtering + subnet-enumeration math (the network I/O is verified on a device). */
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

    // --- IPv4 conversion ---

    @Test
    fun `ipv4 round-trips through int`() {
        listOf("0.0.0.0", "192.168.0.9", "10.1.2.3", "255.255.255.255").forEach {
            assertEquals(it, intToIpv4(ipv4ToInt(it)!!))
        }
    }

    @Test
    fun `ipv4ToInt rejects malformed input`() {
        listOf("192.168.0", "192.168.0.256", "a.b.c.d", "1.2.3.4.5", "").forEach {
            assertNull(ipv4ToInt(it), "expected null for '$it'")
        }
    }

    // --- host enumeration ---

    @Test
    fun `slash 24 yields 253 hosts excluding network, broadcast and self`() {
        val hosts = hostsToScan("192.168.0.6", 24)
        assertEquals(253, hosts.size) // 254 usable minus self
        assertTrue("192.168.0.9" in hosts) // the TV
        assertFalse("192.168.0.6" in hosts) // self excluded
        assertFalse("192.168.0.0" in hosts) // network excluded
        assertFalse("192.168.0.255" in hosts) // broadcast excluded
        assertTrue("192.168.0.1" in hosts && "192.168.0.254" in hosts)
    }

    @Test
    fun `large subnets are narrowed to the local slash 24`() {
        // A /16 (65k hosts) must collapse to the local /24 so the sweep stays fast.
        val hosts = hostsToScan("10.0.5.7", 16)
        assertEquals(253, hosts.size)
        assertTrue(hosts.all { it.startsWith("10.0.5.") })
        assertTrue("10.0.5.42" in hosts)
        assertFalse("10.0.6.1" in hosts)
    }

    @Test
    fun `tiny and malformed subnets yield nothing`() {
        assertTrue(hostsToScan("192.168.0.1", 31).isEmpty()) // no usable hosts
        assertTrue(hostsToScan("192.168.0.1", 32).isEmpty())
        assertTrue(hostsToScan("not-an-ip", 24).isEmpty())
    }
}
