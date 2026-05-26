package com.nsai.notes.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.nsai.notes.data.local.security.BiometricAuthManager

@Composable
fun BiometricGateScreen(
    biometricAuthManager: BiometricAuthManager,
    viewModel: BiometricGateViewModel,
    onAuthenticated: () -> Unit,
    onFallbackToPin: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            BiometricGateState.Verifying -> {
                activity?.let {
                    biometricAuthManager.authenticate(
                        activity = it,
                        title = "验证身份",
                        subtitle = "使用生物识别解锁应用",
                        onResult = { result -> viewModel.onBiometricResult(result) }
                    )
                }
            }
            BiometricGateState.Authenticated -> onAuthenticated()
            BiometricGateState.FallbackToPin -> onFallbackToPin()
            else -> {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("NSAI Notes", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            when (state) {
                BiometricGateState.Locked -> {
                    Button(onClick = { viewModel.startVerifying() }) {
                        Text("验证身份继续")
                    }
                }
                BiometricGateState.Verifying -> CircularProgressIndicator()
                BiometricGateState.Error -> {
                    Text("验证失败，请重试", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.startVerifying() }) { Text("重试") }
                    TextButton(onClick = onFallbackToPin) { Text("使用密码") }
                }
                else -> {}
            }
        }
    }
}
