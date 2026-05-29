package com.nsai.notes.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val bottomNavHighlight: Int? = null // 0=notes, 1=files, 2=ai
)

private val onboardingSteps = listOf(
    OnboardingStep(
        icon = Icons.Default.AutoAwesome,
        title = "欢迎使用 NSAI笔记",
        description = "一款以本地存储为核心、融合多模型 AI 对话能力的智能笔记应用。\n\n轻量 · 安全 · 高效 · 跨平台"
    ),
    OnboardingStep(
        icon = Icons.Default.Create,
        title = "笔记管理",
        description = "底部的「笔记」标签页是您的主工作区。支持 Markdown 写作、富文本编辑、全文搜索、笔记加密和\"",
        bottomNavHighlight = 0
    ),
    OnboardingStep(
        icon = Icons.AutoMirrored.Filled.Label,
        title = "标签分类",
        description = "在笔记列表中点击标签图标，对笔记进行分类管理。创建、编辑、删除标签会实时同步到全部笔记。"
    ),
    OnboardingStep(
        icon = Icons.Default.Folder,
        title = "文件管理",
        description = "「文件」标签页浏览和管理本机文件。支持文件夹操作、隐私文件区和云存储同步。",
        bottomNavHighlight = 1
    ),
    OnboardingStep(
        icon = Icons.Default.Brush,
        title = "AI 智能助手",
        description = "切换到「AI」标签页，支持 DeepSeek (Flash/Pro 双模式)、Qwen、MiniMax、GLM、Kimi 等多模型对话，以及自定义 AI 模型。",
        bottomNavHighlight = 2
    ),
    OnboardingStep(
        icon = Icons.Default.Settings,
        title = "配置与个性化",
        description = "在设置中配置主题、字体大小、AI 模型和 API Key。支持添加最多 5 个自定义 AI 服务商。所有数据默认本地存储。"
    )
)

@Composable
fun OnboardingOverlay(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var animatedStep by remember { mutableStateOf(currentStep) }
    val step = onboardingSteps[animatedStep]
    val isLastStep = animatedStep == onboardingSteps.lastIndex

    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
            Color.Black.copy(alpha = 0.78f)
        ),
        center = Offset(Float.POSITIVE_INFINITY, 0f),
        radius = Float.POSITIVE_INFINITY
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .pointerInput(currentStep) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // Ignore, use buttons for navigation
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // No-op, handled by swipe detection below
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Skip button — top right
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("跳过", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }

        // Swipe hint area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentStep) {
                    var dragOffset = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragOffset = 0f },
                        onDragEnd = {
                            if (dragOffset < -80f && currentStep < onboardingSteps.lastIndex) {
                                currentStep++
                                animatedStep = currentStep
                            } else if (dragOffset > 80f && currentStep > 0) {
                                currentStep--
                                animatedStep = currentStep
                            }
                        },
                        onDragCancel = { },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step indicators — animated dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onboardingSteps.indices.forEach { i ->
                        val isCurrent = i == animatedStep
                        val scale by animateFloatAsState(
                            targetValue = if (isCurrent) 1.0f else 0.6f,
                            animationSpec = tween(400),
                            label = "dot_scale_$i"
                        )
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 12.dp else 8.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Animated icon
                val iconScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(500),
                    label = "icon_scale"
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(iconScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Title
                Text(
                    step.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(12.dp))

                // Description
                Text(
                    step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )

                // Bottom nav preview
                if (step.bottomNavHighlight != null) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("笔记", "文件", "AI").forEachIndexed { i, label ->
                            val isHighlighted = i == step.bottomNavHighlight
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isHighlighted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                if (isHighlighted) {
                                    Spacer(Modifier.height(3.dp))
                                    Box(
                                        Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous
                    if (animatedStep > 0) {
                        Button(
                            onClick = {
                                animatedStep--
                                currentStep = animatedStep
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("上一步")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp)) // keeps alignment
                    }

                    // Next / Complete
                    Button(
                        onClick = {
                            if (isLastStep) onComplete()
                            else {
                                animatedStep++
                                currentStep = animatedStep
                            }
                        }
                    ) {
                        Text(if (isLastStep) "开始使用" else "下一步")
                        if (!isLastStep) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
