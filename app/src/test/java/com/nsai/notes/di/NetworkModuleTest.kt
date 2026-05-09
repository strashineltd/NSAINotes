package com.nsai.notes.di
import org.junit.Test
import org.junit.Assert.*
class NetworkModuleTest {
    @Test fun `client created`() { assertNotNull(NetworkModule.provideOkHttpClient()) }
}

