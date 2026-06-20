package com.shakilclark.lgremote.tv

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException

/** A TV found on the LAN. Name is confirmed via getSystemInfo after connecting. */
data class DiscoveredTv(val address: String, val name: String = "LG webOS TV")

/**
 * Auto-discovery of LG webOS TVs on the local network. Two methods run concurrently and merge:
 *
 *  1. **SSDP** (UPnP M-SEARCH) — instant when it works, but many TVs ignore the second-screen
 *     search target and home-Wi-Fi/Android frequently drops multicast, so it can find nothing.
 *  2. **TCP sweep** of the local subnet for port **3001** (the LG secure-SSAP socket) — the
 *     dependable path: if a host accepts a connection on 3001 it's an LG webOS TV. This is what
 *     actually finds the set when SSDP comes back empty.
 *
 * Manual IP entry remains the ultimate fallback (US1). SSDP needs a multicast lock; the sweep
 * needs no special permission beyond INTERNET.
 */
class TvDiscovery(context: Context) {

    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)

    suspend fun discover(timeoutMs: Long = 3_000): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        val found = LinkedHashMap<String, DiscoveredTv>()
        coroutineScope {
            val ssdp = async { ssdpDiscover(timeoutMs) }
            val sweep = async { sweepForSsap() }
            // Sweep results first (most reliable), then any extras SSDP turned up.
            (sweep.await() + ssdp.await()).forEach { found.putIfAbsent(it.address, it) }
        }
        found.values.toList()
    }

    // --- SSDP (multicast) ---

    private fun ssdpDiscover(timeoutMs: Long): List<DiscoveredTv> {
        val lock = wifi?.createMulticastLock("lg-remote-ssdp")?.apply { setReferenceCounted(true); acquire() }
        val found = LinkedHashMap<String, DiscoveredTv>()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 700
                socket.broadcast = true
                val group = InetAddress.getByName(SSDP_ADDRESS)
                // Probe the webOS service AND ssdp:all — some sets only answer the latter. UDP drops,
                // so send each a couple of times.
                listOf(mSearch(ST_WEBOS), mSearch(ST_ALL)).forEach { msg ->
                    val bytes = msg.toByteArray()
                    repeat(2) { socket.send(DatagramPacket(bytes, bytes.size, group, SSDP_PORT)) }
                }

                val deadline = System.currentTimeMillis() + timeoutMs
                val buffer = ByteArray(2048)
                while (System.currentTimeMillis() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val response = String(packet.data, 0, packet.length)
                    val address = packet.address.hostAddress ?: continue
                    if (isLgWebosSsdp(response)) found[address] = DiscoveredTv(address = address)
                }
            }
        } catch (_: Exception) {
            // Discovery is best-effort; the sweep and manual entry remain.
        } finally {
            lock?.takeIf { it.isHeld }?.release()
        }
        return found.values.toList()
    }

    // --- TCP subnet sweep ---

    /** Sweep the local subnet for hosts with the LG SSAP port open. */
    private suspend fun sweepForSsap(perHostTimeoutMs: Int = 300): List<DiscoveredTv> = coroutineScope {
        val (localIp, prefix) = localIpv4() ?: return@coroutineScope emptyList()
        val hosts = hostsToScan(localIp, prefix)
        val gate = Semaphore(MAX_PARALLEL_PROBES)
        hosts.map { host ->
            async {
                gate.withPermit {
                    if (tcpOpen(host, SSAP_PORT, perHostTimeoutMs)) DiscoveredTv(address = host) else null
                }
            }
        }.awaitAll().filterNotNull()
    }

    /** The site-local IPv4 address + network prefix length of the active Wi-Fi interface. */
    private fun localIpv4(): Pair<String, Int>? {
        try {
            for (ni in NetworkInterface.getNetworkInterfaces()) {
                if (!ni.isUp || ni.isLoopback) continue
                for (ia in ni.interfaceAddresses) {
                    val addr = ia.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                        return (addr.hostAddress ?: continue) to ia.networkPrefixLength.toInt()
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun tcpOpen(host: String, port: Int, timeoutMs: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs) }
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val ST_WEBOS = "urn:lge-com:service:webos-second-screen:1"
        private const val ST_ALL = "ssdp:all"
        private const val SSAP_PORT = 3001
        private const val MAX_PARALLEL_PROBES = 64

        private fun mSearch(st: String) = buildString {
            append("M-SEARCH * HTTP/1.1\r\n")
            append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
            append("MAN: \"ssdp:discover\"\r\n")
            append("MX: 2\r\n")
            append("ST: $st\r\n\r\n")
        }
    }
}

/** Whether an SSDP response is from an LG webOS device. Pure + unit-tested. */
fun isLgWebosSsdp(response: String): Boolean {
    val r = response.lowercase()
    return "webos" in r || "lge-com" in r || "second-screen" in r
}

/**
 * The candidate host addresses to probe for [localIp]/[prefixLength], excluding network,
 * broadcast and [localIp] itself. Pure + unit-tested. Subnets larger than [maxHosts] usable
 * addresses (e.g. a /16) are narrowed to the local /24 so a scan stays fast.
 */
fun hostsToScan(localIp: String, prefixLength: Int, maxHosts: Int = 1022): List<String> {
    val ipInt = ipv4ToInt(localIp) ?: return emptyList()
    val prefix = prefixLength.coerceIn(0, 32)
    val hostBits = 32 - prefix
    if (hostBits < 2) return emptyList() // /31, /32: no usable host range
    val size = 1L shl hostBits
    if (size - 2 > maxHosts) return hostsToScan(localIp, 24, maxHosts) // too big → local /24
    val mask = (-1 shl hostBits)
    val network = ipInt and mask
    val result = ArrayList<String>()
    for (h in 1 until (size - 1)) {
        val addr = network + h.toInt()
        if (addr == ipInt) continue // skip self
        result.add(intToIpv4(addr))
    }
    return result
}

/** Parse dotted-quad IPv4 to a 32-bit int, or null if malformed. Pure. */
fun ipv4ToInt(ip: String): Int? {
    val parts = ip.split(".")
    if (parts.size != 4) return null
    var acc = 0
    for (p in parts) {
        val n = p.toIntOrNull() ?: return null
        if (n !in 0..255) return null
        acc = (acc shl 8) or n
    }
    return acc
}

/** Render a 32-bit int as dotted-quad IPv4. Pure. */
fun intToIpv4(value: Int): String =
    "${(value ushr 24) and 0xFF}.${(value ushr 16) and 0xFF}.${(value ushr 8) and 0xFF}.${value and 0xFF}"
