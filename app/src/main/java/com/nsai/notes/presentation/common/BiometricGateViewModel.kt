package com.nsai.notes.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.data.local.security.BiometricAuthManager
import com.nsai.notes.data.local.security.BiometricResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class BiometricGateState { Locked, Verifying, Authenticated, FallbackToPin, Error }

@HiltViewModel
class BiometricGateViewModel @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(BiometricGateState.Locked)
    val state: StateFlow<BiometricGateState> = _state.asStateFlow()

    fun onBiometricResult(result: BiometricResult) {
        _state.value = when (result) {
            is BiometricResult.Success -> BiometricGateState.Authenticated
            is BiometricResult.FallbackToPin -> BiometricGateState.FallbackToPin
            is BiometricResult.Error -> BiometricGateState.Error
        }
    }

    fun reset() { _state.value = BiometricGateState.Locked }
    fun startVerifying() { _state.value = BiometricGateState.Verifying }
}
