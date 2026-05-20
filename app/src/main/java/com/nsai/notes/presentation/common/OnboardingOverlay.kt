package com.nsai.notes.presentation.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
        description = "一款以本地存储为核心、集成多模型 AI 对话能力的智能笔记应用。"
    ),
    OnboardingStep(
        icon = Icons.Default.Create,
        title = "📝 笔记管理",
        description = "使用底部的「笔记」标签页创建和管理笔记。支持 Markdown 写作、标签分类、全文搜索、笔记加密。",
        bottomNavHighlight = 0
    ),
    OnboardingStep(
        icon = Icons.Default.Folder,
        title = "📁 文件管理",
        description = "在「文件」标签页中浏览本机文件、管理文件夹、创建隐私文件区。",
        bottomNavHighlight = 1
    ),
    OnboardingStep(
        icon = Icons.Default.Brush,
        title = "🤖 AI 智能助手",
        description = "切换到「AI」标签页，选择多模型对话。支持快速/思考模式、图片生成、Agent 工具和知识库检索。",
        bottomNavHighlight = 2
    ),
    OnboardingStep(
        icon = Icons.Default.Settings,
        title = "⚙️ 配置与设置",
        description = "在设置中配置主题、字体大小、API Key 和各 AI 服务商的模型。所有数据默认存储在本地。"
    )
)

@Composable
fun OnboardingOverlay(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val step = onboardingSteps[currentStep]
    val isLastStep = currentStep == onboardingSteps.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onboardingSteps.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == currentStep) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == currentStep) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Title
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                // Description
                Text(
                    step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                // Bottom nav preview
                if (step.bottomNavHighlight != null) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("笔记", "文件", "AI").forEachIndexed { i, label ->
                            val isHighlighted = i == step.bottomNavHighlight
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isHighlighted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                if (isHighlighted) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text("跳过")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentStep > 0) {
                            IconButton(onClick = { currentStep-- }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一步")
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            if (isLastStep) onComplete()
                            else currentStep++
                        }) {
                            Text(if (isLastStep) "开始使用" else "下一步")
                        }
                    }
                }
            }
        }
    }
}
