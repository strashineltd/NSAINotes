package com.nsai.notes.data.remote.ai
import org.junit.Test
import org.junit.Assert.*
class CertificatePinsTest {
    @Test fun `5 domains`() { assertEquals(5, CertificatePins.pins.size) }
    @Test fun `hostnames correct`() {
        val h = CertificatePins.hostnames()
        assertTrue(h.contains("api.deepseek.com"))
        assertTrue(h.contains("api.moonshot.cn"))
    }
}

