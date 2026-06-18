package com.shakilclark.lgremote.tv

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * The TV serves SSAP over `wss://<ip>:3001` with a self-signed certificate. A native app
 * (unlike a browser) can trust it programmatically — this is the whole reason the 001 server
 * tier is no longer needed (FR-016, research R1).
 *
 * Scope: this trusts ANY cert for the TV socket. That is acceptable for a LAN-only remote
 * controlling the user's own TV at a known address; it never touches general web traffic.
 */
object TvTrustManager {

    private val trustAll = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /** OkHttp client configured for the TV's self-signed wss socket. Reused for the pointer socket. */
    fun client(): OkHttpClient {
        val ssl = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustAll), SecureRandom())
        }
        return OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true }
            // WebSocket stays open; no read timeout, generous connect timeout.
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    /** The TV's secure SSAP endpoint. Cleartext :3000 is deprecated and not used (R1). */
    fun wssUrl(host: String): String = "wss://$host:3001"
}
