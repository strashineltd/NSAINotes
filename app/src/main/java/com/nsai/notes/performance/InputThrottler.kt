package com.nsai.notes.performance

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputThrottler @Inject constructor() {

    private val navigationThrottleMap = ConcurrentHashMap<String, Long>()
    private val clickThrottleMap = ConcurrentHashMap<String, Long>()

    private val navThrottleMs = 300L
    private val clickThrottleMs = 200L
    private val fastClickThrottleMs = 80L

    fun shouldAllowNavigation(route: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val last = navigationThrottleMap[route] ?: 0L
        if (now - last < navThrottleMs) return false
        navigationThrottleMap[route] = now
        return true
    }

    fun shouldAllowClick(actionKey: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val last = clickThrottleMap[actionKey] ?: 0L
        if (now - last < clickThrottleMs) return false
        clickThrottleMap[actionKey] = now
        return true
    }

    fun shouldAllowFastClick(actionKey: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val last = clickThrottleMap[actionKey] ?: 0L
        if (now - last < fastClickThrottleMs) return false
        clickThrottleMap[actionKey] = now
        return true
    }

    fun reset() {
        navigationThrottleMap.clear()
        clickThrottleMap.clear()
    }
}
