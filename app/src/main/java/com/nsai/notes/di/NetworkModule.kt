package com.nsai.notes.di

import com.nsai.notes.data.remote.ai.CertificatePins
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val pinner = CertificatePinner.Builder()
        CertificatePins.pins.forEach { pin ->
            pinner.add(pin.hostname, pin.sha256Hash)
        }
        CertificatePins.backupPins.forEach { pin ->
            pinner.add(pin.hostname, pin.sha256Hash)
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .certificatePinner(pinner.build())
            .build()
    }
}
