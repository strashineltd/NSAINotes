package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIProviderConfig
import com.nsai.notes.domain.model.SearchEngine

private val providerColors = mapOf(
    AIProvider.DEEPSEEK to Color(0xFF4D6BFE),
    AIProvider.KIMI to Color(0xFF6B4CFF),
    AIProvider.GLM to Color(0xFF10A37F),
    AIProvider.MINIMAX to Color(0xFFE91E63),
    AIProvider.QWEN to Color(0xFF0070F3),
    AIProvider.MIMO to Color(0xFFFF6900)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    selectedProvider: AIProvider,
    onProviderChange: (AIProvider) -> Unit,
    providerConfigs: List<AIProviderConfig>,
    onUpdateApiKey: (AIProvider, String) -> Unit,
    onUpdateBaseUrl: (AIProvider, String) -> Unit,
    onTestConnection: (AIProvider) -> Unit,
    testResults: Map<AIProvider, String>,
    searchEngine: String,
    onSearchEngineChange: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("设置", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))

            Text("模型", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            AIProvider.entries.forEach { provider ->
                val config = providerConfigs.find { it.provider == provider }
                val isSelected = provider == selectedProvider
                val color = providerColors[provider] ?: MaterialTheme.colorScheme.primary
                val testResult = testResults[provider]

                ProviderCard(
                    provider = provider,
                    config = config,
                    isSelected = isSelected,
                    color = color,
                    testResult = testResult,
                    onSelect = { onProviderChange(provider) },
                    onUpdateApiKey = { key -> onUpdateApiKey(provider, key) },
                    onUpdateBaseUrl = { url -> onUpdateBaseUrl(provider, url) },
                    onTestConnection = { onTestConnection(provider) }
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text("搜索引擎", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SearchEngineDropdown(
                selected = searchEngine,
                onSelect = onSearchEngineChange
            )
            Spacer(Modifier.height(20.dp))

            Text("对话历史", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClearHistory) {
                Text("清除所有对话", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProviderCard(
    provider: AIProvider,
    config: AIProviderConfig?,
    isSelected: Boolean,
    color: Color,
    testResult: String?,
    onSelect: () -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onUpdateBaseUrl: (String) -> Unit,
    onTestConnection: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                else Color.Transparent
            )
            .clickable {
                onSelect()
                expanded = !expanded
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = provider.displayName.first().toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (config?.isEnabled == true) {
                    Text(
                        text = "已启用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                OutlinedTextField(
                    value = config?.apiKey ?: "",
                    onValueChange = onUpdateApiKey,
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = config?.baseUrl ?: provider.defaultBaseUrl,
                    onValueChange = onUpdateBaseUrl,
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTestConnection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("测试连接", style = MaterialTheme.typography.labelSmall)
                    }
                    if (testResult != null) {
                        val isSuccess = testResult.contains("成功", ignoreCase = true) ||
                                testResult.contains("success", ignoreCase = true)
                        Icon(
                            if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isSuccess) Color(0xFF34C759) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = testResult,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSuccess) Color(0xFF34C759) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEngineDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val engines = SearchEngine.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { expanded = true }) {
            Text(selected, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.KeyboardArrowDown,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            engines.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(engine.name) },
                    onClick = {
                        onSelect(engine.name)
                        expanded = false
                    }
                )
            }
        }
    }
}
