package com.nsai.notes.di

import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.Decoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nsai.notes.performance.DeviceAdaptiveConfig
import com.nsai.notes.performance.DeviceClass
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        deviceConfig: DeviceAdaptiveConfig
    ): ImageLoader {
        val cachePercent = when (deviceConfig.deviceClass) {
            DeviceClass.HIGH -> 0.12
            DeviceClass.MID  -> 0.08
            DeviceClass.LOW  -> 0.04
        }
        val diskSize = when (deviceConfig.deviceClass) {
            DeviceClass.HIGH -> 50 * 1024 * 1024L
            DeviceClass.MID  -> 25 * 1024 * 1024L
            DeviceClass.LOW  -> 10 * 1024 * 1024L
        }
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(cachePercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("nsai_images"))
                    .maxSizeBytes(diskSize)
                    .build()
            }
            .crossfade(if (deviceConfig.deviceClass == DeviceClass.LOW) 0 else 250)
            .respectCacheHeaders(false)
            .build()
    }
}
