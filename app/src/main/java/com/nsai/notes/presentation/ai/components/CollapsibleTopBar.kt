package com.nsai.notes.presentation.ai.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nsai.notes.domain.model.AIProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleTopBar(
    selectedProvider: AIProvider,
    onProviderSelected: (AIProvider) -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMCPSkillClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    var showModelMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    LargeTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 发光状态指示点
                Box(
                    modifier = Modifier.size(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val dotColor = if (selectedProvider.supportsVision)
                        Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    // 外发光环
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                dotColor.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                    // 中心点
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                dotColor,
                                CircleShape
                            )
                    )
                }
                Spacer(Modifier.width(8.dp))

                AssistChip(
                    onClick = { showModelMenu = true },
                    label = {
                        Text(
                            selectedProvider.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )

                // 模型选择下拉菜单
                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false }
                ) {
                    AIProvider.entries.forEach { provider ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(provider.displayName)
                                    if (provider == selectedProvider) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "✓",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showModelMenu = false
                                onProviderSelected(provider)
                            },
                            leadingIcon = {
                                val dotColor = if (provider.supportsVision)
                                    Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            dotColor,
                                            CircleShape
                                        )
                                )
                            }
                        )
                    }
                }
            }
        },
        actions = {
            // 历史按钮
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, "历史对话")
            }

            // 更多菜单
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(Icons.Default.MoreVert, "更多")
            }

            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("插件管理") },
                    onClick = {
                        showMoreMenu = false
                        onMCPSkillClick()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Extension,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("模型设置") },
                    onClick = {
                        showMoreMenu = false
                        onSettingsClick()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Settings,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    )
}