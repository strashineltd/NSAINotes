package com.nsai.notes.data.remote

/**
 * Server configuration for the backend management platform.
 *
 * Set ServerConfig.baseUrl before using LicenseService/ProductService.
 * For emulator: http://10.0.2.2:3005
 * For real device: http://<YOUR_LAN_IP>:3005
 *
 * Also add the IP to res/xml/network_security_config.xml to allow cleartext HTTP.
 */
object ServerConfig {
    var baseUrl: String = ""
}
