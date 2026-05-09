package com.nsai.notes.data.local.security
import org.junit.Test
import org.junit.Assert.*
class BiometricAuthManagerTest {
    @Test fun `Success type`() { assertTrue(BiometricResult.Success is BiometricResult) }
    @Test fun `Error carries msg`() { assertEquals("test", BiometricResult.Error("test").message) }
    @Test fun `enum 4 cases`() { assertEquals(4, BiometricError.entries.size) }
    @Test fun `manager instantiated`() { assertNotNull(BiometricAuthManager()) }
}

