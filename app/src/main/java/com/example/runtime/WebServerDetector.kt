package com.example.runtime

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.regex.Pattern

data class DetectedServer(
    val port: Int,
    val localUrl: String,
    val lanUrl: String?
)

object WebServerDetector {

    private val SERVER_PATTERNS = listOf(
        Pattern.compile("https?://(?:127\\.0\\.0\\.1|localhost|0\\.0\\.0\\.0):(\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("running on (?:all addresses \\()?https?://(?:0\\.0\\.0\\.0|127\\.0\\.0\\.1|localhost)?:?(\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Serving HTTP on [^ ]+ port (\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Uvicorn running on https?://[^:]+:(\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("listening at https?://[^:]+:(\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Bottle server starting on https?://[^:]+:(\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Tornado server running on (?:port )?(\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("port[= :](\\d{4,5})", Pattern.CASE_INSENSITIVE)
    )

    fun detectServer(line: String): DetectedServer? {
        for (pattern in SERVER_PATTERNS) {
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                val portStr = matcher.group(1)
                val port = portStr?.toIntOrNull()
                if (port != null && port in 1024..65535) {
                    val lanIp = getLanIpAddress()
                    val localUrl = "http://127.0.0.1:$port"
                    val lanUrl = lanIp?.let { "http://$it:$port" }
                    return DetectedServer(port = port, localUrl = localUrl, lanUrl = lanUrl)
                }
            }
        }
        return null
    }

    fun getLanIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (host != null && (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172."))) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore network inspection error
        }
        return null
    }
}
