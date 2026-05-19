package com.nsai.notes.data.local.license

import android.util.Base64
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.remote.license.LicenseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseManager @Inject constructor(
    private val deviceKeyGenerator: DeviceKeyGenerator,
    private val licenseVerifier: LicenseVerifier,
    private val licenseService: LicenseService,
    private val settingsDataStore: SettingsDataStore
) {
    private val _isActive = MutableStateFlow(true) // Temporarily disabled license check
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _expireTime = MutableStateFlow(0L)
    val expireTime: StateFlow<Long> = _expireTime.asStateFlow()

    private val _features = MutableStateFlow<List<String>>(emptyList())
    val features: StateFlow<List<String>> = _features.asStateFlow()

    private val _productName = MutableStateFlow<String?>(null)
    val productName: StateFlow<String?> = _productName.asStateFlow()

    private val deviceId by lazy { deviceKeyGenerator.generateDeviceId() }

    fun getDeviceCode(): String = deviceKeyGenerator.getShortDeviceCode()

    /** Check if a specific paid feature is unlocked */
    fun hasFeature(feature: String): Boolean = _features.value.contains(feature)

    suspend fun activate(activationCode: String): ActivateResult = withContext(Dispatchers.IO) {
        val clean = activationCode.replace("-", "").replace(" ", "")
        if (clean.length < 10) return@withContext ActivateResult.Error("激活码格式无效")

        // Try server validation first
        val result = licenseService.validate(activationCode, deviceId)
        if (result.valid && result.expireTimestamp != null) {
            val expireTs = result.expireTimestamp
            settingsDataStore.setLicenseData(clean, expireTs)
            settingsDataStore.setLicenseFeatures(result.features ?: emptyList())
            settingsDataStore.setLicenseProductName(result.productName ?: "")
            _isActive.value = true
            _expireTime.value = expireTs
            _features.value = result.features ?: emptyList()
            _productName.value = result.productName
            return@withContext ActivateResult.Success(expireTs)
        }

        if (!result.valid && result.message.isNotBlank()) {
            return@withContext ActivateResult.Error(result.message)
        }

        return@withContext ActivateResult.Error("激活失败")
    }

    suspend fun checkLicense() = withContext(Dispatchers.IO) {
        val saved = settingsDataStore.getLicenseExpireTime()
        if (saved > 0L && System.currentTimeMillis() < saved) {
            _isActive.value = true
            _expireTime.value = saved
            _features.value = settingsDataStore.getLicenseFeatures()
            _productName.value = settingsDataStore.getLicenseProductName()
        } else {
            _isActive.value = false
            _expireTime.value = 0L
            _features.value = emptyList()
            _productName.value = null
        }
    }

    fun getExpireDays(): Int {
        val exp = _expireTime.value
        if (exp <= 0L) return 0
        val remaining = exp - System.currentTimeMillis()
        return (remaining / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    }

    fun getDeviceIdForActivation(): String = deviceId.take(12)
}

sealed class ActivateResult {
    data class Success(val expireTimestamp: Long) : ActivateResult()
    data class Error(val message: String) : ActivateResult()
}
