package com.nsai.notes.presentation.tags

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManageScreen(
    onNavigateBack: () -> Unit,
    viewModel: TagManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("标签管理", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.newTagName,
                    onValueChange = { viewModel.onEvent(TagManageEvent.UpdateNewTagName(it)) },
                    placeholder = { Text("新标签名称") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.onEvent(TagManageEvent.CreateTag) },
                    enabled = uiState.newTagName.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加标签",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.tags, key = { _, tag -> tag.id }) { index, tag ->
                    val delay = (index * 50).coerceAtMost(300)
                    AnimatedTagItem(delay = delay) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(
                                onClick = { viewModel.onEvent(TagManageEvent.DeleteTag(tag.id)) }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除标签",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedTagItem(
    delay: Int,
    content: @Composable () -> Unit
) {
    val tokens = LocalAnimationConfig.current
    val effectiveDelay = tokens.staggeredDelay.takeIf { delay > 0 }?.let { it * (delay / 50).coerceAtMost(6) } ?: 0
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(delayMillis = effectiveDelay)) +
                slideInVertically(animationSpec = tween(delayMillis = effectiveDelay)) { it / 4 }
    ) {
        content()
    }
}
