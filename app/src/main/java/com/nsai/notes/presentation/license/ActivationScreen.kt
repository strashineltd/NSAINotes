package com.nsai.notes.presentation.license

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.data.local.license.LicenseManager
import com.nsai.notes.data.remote.license.ProductService
import com.nsai.notes.data.remote.license.ProductInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val licenseManager: LicenseManager,
    private val productService: ProductService
) : ViewModel() {

    data class UiState(
        val activationCode: String = "",
        val message: String? = null,
        val isError: Boolean = false,
        val isActive: Boolean = false,
        val expireDays: Int = 0,
        val deviceCode: String = "",
        val products: List<ProductInfo> = emptyList(),
        val isLoadingProducts: Boolean = false,
        val isPolling: Boolean = false,
        val pendingOrderId: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        licenseManager.checkLicense()
        _uiState.value = _uiState.value.copy(deviceCode = licenseManager.getDeviceCode())
        viewModelScope.launch { refreshState() }
        loadProducts()
        startAutoRefresh()
    }

    private fun refreshState() {
        _uiState.value = _uiState.value.copy(
            isActive = licenseManager.isActive.value,
            expireDays = licenseManager.getExpireDays()
        )
    }

    fun refreshProducts() { loadProducts() }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProducts = true)
            val products = withContext(Dispatchers.IO) { productService.fetchProducts() }
            _uiState.value = _uiState.value.copy(products = products, isLoadingProducts = false)
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(15000) // Refresh every 15 seconds
                if (!_uiState.value.isActive) {
                    val products = withContext(Dispatchers.IO) { productService.fetchProducts() }
                    if (products.isNotEmpty() && products != _uiState.value.products) {
                        _uiState.value = _uiState.value.copy(products = products)
                    }
                }
            }
        }
    }

    fun purchase(product: ProductInfo) {
        viewModelScope.launch {
            try {
                val deviceId = licenseManager.getDeviceIdForActivation()
                val result = withContext(Dispatchers.IO) { productService.createOrder(product.id, deviceId) }
                if (result.error != null) {
                    _uiState.value = _uiState.value.copy(message = result.error, isError = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        pendingOrderId = result.orderId, isPolling = true,
                        message = "订单已创建，等待管理员确认付款...", isError = false
                    )
                    startPollingForActivation(result.orderId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "网络错误: ${e.message}", isError = true)
            }
        }
    }

    private fun startPollingForActivation(orderId: String) {
        viewModelScope.launch {
            for (i in 1..30) { // poll for ~2.5 minutes
                delay(5000) // every 5 seconds
                try {
                    val resp = withContext(Dispatchers.IO) {
                        productService.checkOrderStatus(orderId)
                    }
                    if (resp != null) {
                        // Try to activate with the returned code
                        val result = licenseManager.activate(resp)
                        when (result) {
                            is com.nsai.notes.data.local.license.ActivateResult.Success -> {
                                val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                _uiState.value = _uiState.value.copy(
                                    isActive = true, isPolling = false,
                                    expireDays = licenseManager.getExpireDays(),
                                    message = "🎉 激活成功！AI功能已解锁",
                                    isError = false
                                )
                                return@launch
                            }
                            is com.nsai.notes.data.local.license.ActivateResult.Error -> {
                                // Keep polling - maybe the code isn't ready yet
                            }
                        }
                    }
                } catch (_: Exception) { /* retry */ }
            }
            _uiState.value = _uiState.value.copy(
                isPolling = false,
                message = "轮询超时，请手动输入激活码或联系管理员",
                isError = true
            )
        }
    }

    fun onCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(activationCode = code, message = null, isError = false)
    }

    fun activate() {
        val code = _uiState.value.activationCode.trim()
        if (code.isBlank()) return

        val result = licenseManager.activate(code)
        when (result) {
            is com.nsai.notes.data.local.license.ActivateResult.Success -> {
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                _uiState.value = _uiState.value.copy(
                    isActive = true,
                    expireDays = licenseManager.getExpireDays(),
                    message = "激活成功！到期时间: ${df.format(Date(result.expireTimestamp))}",
                    isError = false
                )
            }
            is com.nsai.notes.data.local.license.ActivateResult.Error -> {
                _uiState.value = _uiState.value.copy(message = result.message, isError = true)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationScreen(
    onNavigateBack: () -> Unit,
    viewModel: ActivationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("套餐购买") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            if (uiState.isActive) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("AI功能已激活", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("剩余 ${uiState.expireDays} 天", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Text("NSAI笔记 AI功能", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("¥10/年起 · 平台同步套餐", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(24.dp))

                // Device code
                Card(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("设备码", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.deviceCode, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Product cards from platform
                if (uiState.isLoadingProducts) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text("加载套餐...", style = MaterialTheme.typography.bodySmall)
                } else if (uiState.products.isNotEmpty()) {
                    uiState.products.forEach { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("¥${product.priceYuan}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("/ ${product.durationLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(product.name, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.purchase(product) },
                                    enabled = !uiState.isPolling,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("立即购买") }
                            }
                        }
                    }
                } else {
                    Text("暂无可用套餐", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Text("或输入已有激活码", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))

                OutlinedTextField(
                    value = uiState.activationCode,
                    onValueChange = { viewModel.onCodeChange(it) },
                    placeholder = { Text("NSAI-XXXX-XXXX-XXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = uiState.isError
                )

                uiState.message?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = if (uiState.isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.activate() },
                    enabled = uiState.activationCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("激活") }

                Spacer(Modifier.height(24.dp))
                Text("获取激活码请联系开发者\n提供上方设备码即可生成专属激活码",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center)
            }

        }
    }
}
