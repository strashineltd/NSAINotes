package com.nsai.notes.data.remote

/**
 * Server configuration for the backend management platform.
 *
 * Emulator: use 10.0.2.2 (maps to host machine's localhost)
 * Real device on same WiFi: use your computer's LAN IP (e.g. 192.168.x.x)
 *
 * Also update res/xml/network_security_config.xml to allow cleartext
 * HTTP to the IP address above.
 */
object ServerConfig {
    var baseUrl: String = "http://192.168.0.103:3005"
}
