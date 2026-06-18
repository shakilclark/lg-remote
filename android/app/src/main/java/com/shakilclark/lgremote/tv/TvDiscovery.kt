package com.shakilclark.lgremote.tv

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/** A TV found on the LAN via SSDP. Name is confirmed via getSystemInfo after connecting. */
data class DiscoveredTv(val address: String, val name: String = "LG webOS TV")

/**
 * SSDP auto-discovery of LG webOS TVs on the local network (research R6). Sends an M-SEARCH for
 * the webOS second-screen service and collects responders for a few seconds. Requires a
 * multicast lock. Manual IP entry remains the fallback.
 */
class TvDiscovery(context: Context) {

    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)

    suspend fun discover(timeoutMs: Long = 3_000): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        val lock = wifi?.createMulticastLock("lg-remote-ssdp")?.apply { setReferenceCounted(true); acquire() }
        val found = LinkedHashMap<String, DiscoveredTv>()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 700
                socket.broadcast = true
                val group = InetAddress.getByName(SSDP_ADDRESS)
                val msg = MSEARCH.toByteArray()
                // Send a couple of probes — UDP can drop.
                repeat(2) { socket.send(DatagramPacket(msg, msg.size, group, SSDP_PORT)) }

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
            // Discovery is best-effort; manual entry is always available.
        } finally {
            lock?.takeIf { it.isHeld }?.release()
        }
        found.values.toList()
    }

    companion object {
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val ST = "urn:lge-com:service:webos-second-screen:1"
        private val MSEARCH = buildString {
            append("M-SEARCH * HTTP/1.1\r\n")
            append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
            append("MAN: \"ssdp:discover\"\r\n")
            append("MX: 2\r\n")
            append("ST: $ST\r\n\r\n")
        }
    }
}

/** Whether an SSDP response is from an LG webOS device. Pure + unit-tested. */
fun isLgWebosSsdp(response: String): Boolean {
    val r = response.lowercase()
    return "webos" in r || "lge-com" in r || "second-screen" in r
}
