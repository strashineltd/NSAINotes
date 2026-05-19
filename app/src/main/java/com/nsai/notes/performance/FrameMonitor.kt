package com.nsai.notes.performance

import android.view.Choreographer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.nsai.notes.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

data class FrameMetrics(
    val droppedFrames: Int,
    val totalFrames: Int,
    val dropRate: Float
)

@Singleton
class FrameMonitor @Inject constructor() {
    private val choreographer = Choreographer.getInstance()
    @Volatile private var frameStartTime = 0L
    @Volatile private var droppedFrames = 0
    @Volatile private var totalFrames = 0
    @Volatile private var running = false
    @Volatile private var frameCount = 0
    @Volatile private var aggregateDropped = 0
    @Volatile private var aggregateTotal = 0
    @Volatile private var sampleInterval = 300

    private val recentDropRates = ArrayDeque<Float>(10)

    @Volatile private var listener: ((FrameMetrics) -> Unit)? = null

    private val _metrics = MutableSharedFlow<FrameMetrics>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val metrics: SharedFlow<FrameMetrics> = _metrics.asSharedFlow()

    private val _rollingDropRate = MutableStateFlow(0f)
    val rollingDropRate: StateFlow<Float> = _rollingDropRate.asStateFlow()

    @Synchronized
    fun setOnMetricsReport(callback: (FrameMetrics) -> Unit) {
        listener = callback
    }

    private val frameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (frameStartTime > 0) {
                val elapsedMs = (frameTimeNanos - frameStartTime) / 1_000_000
                totalFrames++
                if (elapsedMs > 22) { // 60fps target=16.6ms; 22ms allows 1-frame buffer
                    droppedFrames++
                }
                frameCount++
                if (frameCount >= sampleInterval) {
                    emitMetrics()
                    droppedFrames = 0; totalFrames = 0; frameCount = 0
                }
            }
            frameStartTime = frameTimeNanos
            if (running) {
                if (sampleInterval > 600) { // Deferred callback mode for lower CPU
                    choreographer.postFrameCallbackDelayed(frameCallback, 4L)
                } else {
                    choreographer.postFrameCallback(frameCallback)
                }
            }
        }
    }

    private fun emitMetrics() {
        val total: Int = totalFrames
        val dropped: Int = droppedFrames
        val report = FrameMetrics(
            droppedFrames = dropped,
            totalFrames = total,
            dropRate = dropped.toFloat() / total.coerceAtLeast(1)
        )
        _metrics.tryEmit(report)
        aggregateDropped += droppedFrames
        aggregateTotal += totalFrames
        synchronized(recentDropRates) {
            recentDropRates.addLast(report.dropRate)
            if (recentDropRates.size > 6) recentDropRates.removeFirst()
            _rollingDropRate.value = if (recentDropRates.isEmpty()) 0f else recentDropRates.average().toFloat()
        }
        listener?.invoke(report)
    }

    @Synchronized
    fun start(sampleInterval: Int = 120) { // 2s intervals for responsive jank detection
        if (running) return
        running = true
        this.sampleInterval = sampleInterval
        choreographer.postFrameCallback(frameCallback)
    }

    @Synchronized
    fun stop() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
    }

    @Synchronized
    fun snapshot(): FrameMetrics = FrameMetrics(
        droppedFrames = aggregateDropped,
        totalFrames = aggregateTotal.coerceAtLeast(1),
        dropRate = if (aggregateTotal > 0) aggregateDropped.toFloat() / aggregateTotal else 0f
    )

    val isRunning: Boolean get() = running
}
