package com.nsai.notes.data.local.security

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

sealed class BiometricResult {
    data object Success : BiometricResult()
    data class Error(val message: String) : BiometricResult()
    data object FallbackToPin : BiometricResult()
}

enum class BiometricError(val message: String) {
    NOT_AVAILABLE("设备不支持生物识别"),
    NOT_ENROLLED("未录入生物信息"),
    USER_CANCELED("用户取消验证"),
    FAILED("验证失败，请重试")
}

@Singleton
class BiometricAuthManager @Inject constructor() {
    
    fun canAuthenticate(activity: Activity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == 
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "身份验证",
        subtitle: String = "请验证您的身份以继续",
        negativeButtonText: String = "使用密码",
        onResult: (BiometricResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onResult(BiometricResult.FallbackToPin)
                    BiometricPrompt.ERROR_USER_CANCELED -> onResult(BiometricResult.Error(BiometricError.USER_CANCELED.message))
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> onResult(BiometricResult.Error(BiometricError.NOT_ENROLLED.message))
                    else -> onResult(BiometricResult.Error(BiometricError.FAILED.message))
                }
            }

            override fun onAuthenticationFailed() {
                onResult(BiometricResult.Error(BiometricError.FAILED.message))
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        prompt.authenticate(promptInfo)
    }
}
