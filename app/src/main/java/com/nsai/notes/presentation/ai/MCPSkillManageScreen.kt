package com.nsai.notes.presentation.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.domain.model.MCPServer
import com.nsai.notes.domain.model.SkillPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCPSkillManageScreen(
    onNavigateBack: () -> Unit,
    viewModel: MCPSkillViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCP & Skill 管理", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = uiState.activeTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = uiState.activeTab == 0, onClick = { viewModel.onEvent(MCPSkillEvent.SwitchTab(0)) },
                    text = { Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cable, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("MCP服务器")
                    } })
                Tab(selected = uiState.activeTab == 1, onClick = { viewModel.onEvent(MCPSkillEvent.SwitchTab(1)) },
                    text = { Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Extension, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Skill插件")
                    } })
            }

            when (uiState.activeTab) {
                0 -> MCPServerList(
                    servers = uiState.mcpServers,
                    testResults = uiState.testResults,
                    onAdd = { viewModel.onEvent(MCPSkillEvent.StartEditServer(null)) },
                    onEdit = { viewModel.onEvent(MCPSkillEvent.StartEditServer(it)) },
                    onDelete = { viewModel.onEvent(MCPSkillEvent.DeleteServer(it)) },
                    onTest = { viewModel.onEvent(MCPSkillEvent.TestServer(it)) }
                )
                1 -> SkillPluginList(
                    skills = uiState.skillPlugins,
                    onAdd = { viewModel.onEvent(MCPSkillEvent.StartEditSkill(null)) },
                    onEdit = { viewModel.onEvent(MCPSkillEvent.StartEditSkill(it)) },
                    onDelete = { viewModel.onEvent(MCPSkillEvent.DeleteSkill(it)) }
                )
            }
        }
    }

    uiState.editingServer?.let { server ->
        MCPServerEditDialog(
            server = server,
            onSave = { viewModel.onEvent(MCPSkillEvent.SaveServer(it)) },
            onDismiss = { viewModel.onEvent(MCPSkillEvent.CancelEdit) }
        )
    }

    uiState.editingSkill?.let { skill ->
        SkillEditDialog(
            skill = skill,
            onSave = { viewModel.onEvent(MCPSkillEvent.SaveSkill(it)) },
            onDismiss = { viewModel.onEvent(MCPSkillEvent.CancelEdit) }
        )
    }
}

@Composable
private fun MCPServerList(
    servers: List<MCPServer>,
    testResults: Map<String, String>,
    onAdd: () -> Unit,
    onEdit: (MCPServer) -> Unit,
    onDelete: (String) -> Unit,
    onTest: (String) -> Unit
) {
    if (servers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Cable, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))
                Text("暂无MCP服务器", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Spacer(Modifier.height(4.dp))
                Text("MCP服务器可扩展AI能力，连接外部数据源和工具", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAdd) { Text("添加MCP服务器") }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${servers.size} 个服务器", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    TextButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("添加")
                    }
                }
            }
            items(servers, key = { it.id }) { server ->
                MCPServerCard(
                    server = server,
                    testResult = testResults[server.id],
                    onEdit = { onEdit(server) },
                    onDelete = { onDelete(server.id) },
                    onTest = { onTest(server.id) }
                )
            }
        }
    }
}

@Composable
private fun MCPServerCard(
    server: MCPServer,
    testResult: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(server.name, style = MaterialTheme.typography.titleMedium)
                    if (server.description.isNotBlank()) {
                        Text(server.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Switch(checked = server.isEnabled, onCheckedChange = { onEdit() })
            }
            Spacer(Modifier.height(8.dp))
            Text(server.url, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(server.transport.label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Spacer(Modifier.weight(1f))
                if (testResult != null) {
                    Text(testResult, style = MaterialTheme.typography.labelSmall,
                        color = if (testResult.contains("成功") || testResult.contains("✓"))
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onTest, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.NetworkCheck, "测试", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "编辑", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MCPServerEditDialog(
    server: MCPServer,
    onSave: (MCPServer) -> Unit,
    onDismiss: () -> Unit
) {
    val isNew = server.name.isEmpty()
    var name by remember { mutableStateOf(server.name) }
    var url by remember { mutableStateOf(server.url) }
    var apiKey by remember { mutableStateOf(server.apiKey) }
    var description by remember { mutableStateOf(server.description) }
    var transport by remember { mutableStateOf(server.transport) }
    var enabled by remember { mutableStateOf(server.isEnabled) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "添加MCP服务器" else "编辑MCP服务器") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("服务器URL") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("https://mcp.example.com/sse") })
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key（可选）") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = transport.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("传输协议") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MCPServer.Transport.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { transport = t; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(server.copy(name = name, url = url, apiKey = apiKey, description = description, transport = transport, isEnabled = enabled)) },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SkillPluginList(
    skills: List<SkillPlugin>,
    onAdd: () -> Unit,
    onEdit: (SkillPlugin) -> Unit,
    onDelete: (String) -> Unit
) {
    if (skills.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Extension, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))
                Text("暂无Skill插件", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Spacer(Modifier.height(4.dp))
                Text("Skill插件可自定义AI行为和提示词模板", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAdd) { Text("添加Skill插件") }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${skills.size} 个插件", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    TextButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("添加")
                    }
                }
            }
            items(skills, key = { it.id }) { skill ->
                SkillCard(
                    skill = skill,
                    onEdit = { onEdit(skill) },
                    onDelete = { onDelete(skill.id) }
                )
            }
        }
    }
}

@Composable
private fun SkillCard(
    skill: SkillPlugin,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(skill.name, style = MaterialTheme.typography.titleMedium)
                    if (skill.description.isNotBlank()) {
                        Text(skill.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Switch(checked = skill.isEnabled, onCheckedChange = { onEdit() })
            }
            Spacer(Modifier.height(4.dp))
            Text(skill.category.label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            if (skill.prompt.length > 80) {
                Spacer(Modifier.height(4.dp))
                Text(skill.prompt.take(80) + "...", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "编辑", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillEditDialog(
    skill: SkillPlugin,
    onSave: (SkillPlugin) -> Unit,
    onDismiss: () -> Unit
) {
    val isNew = skill.name.isEmpty()
    var name by remember { mutableStateOf(skill.name) }
    var description by remember { mutableStateOf(skill.description) }
    var prompt by remember { mutableStateOf(skill.prompt) }
    var category by remember { mutableStateOf(skill.category) }
    var enabled by remember { mutableStateOf(skill.isEnabled) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "添加Skill插件" else "编辑Skill插件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2)
                OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("System Prompt") },
                    modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 8)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = category.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        SkillPlugin.Category.entries.forEach { c ->
                            DropdownMenuItem(text = { Text(c.label) }, onClick = { category = c; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(skill.copy(name = name, description = description, prompt = prompt, category = category, isEnabled = enabled)) },
                enabled = name.isNotBlank() && prompt.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
