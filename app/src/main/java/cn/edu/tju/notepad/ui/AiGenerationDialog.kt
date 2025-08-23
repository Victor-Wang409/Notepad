package cn.edu.tju.notepad.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.edu.tju.notepad.ai.GenerationStyle
import cn.edu.tju.notepad.ai.ImproveType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGenerationDialog(
    currentTitle: String = "",
    currentContent: String = "",
    onDismiss: () -> Unit,
    onGenerate: (prompt: String, style: GenerationStyle) -> Unit,
    onImprove: (ImproveType) -> Unit,
    isGenerating: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(0) }
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(GenerationStyle.NORMAL) }
    var showStyleMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isGenerating,
            dismissOnClickOutside = !isGenerating
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI 笔记助手",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isGenerating) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab选择
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("生成新内容") },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        enabled = currentContent.isNotEmpty(),
                        text = { Text("优化现有") },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 内容区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> {
                            // 生成新内容
                            GenerateNewContent(
                                prompt = prompt,
                                onPromptChange = { prompt = it },
                                selectedStyle = selectedStyle,
                                onStyleChange = { selectedStyle = it },
                                showStyleMenu = showStyleMenu,
                                onShowStyleMenuChange = { showStyleMenu = it },
                                isGenerating = isGenerating,
                                onGenerate = {
                                    if (prompt.isNotBlank()) {
                                        onGenerate(prompt, selectedStyle)
                                    }
                                }
                            )
                        }
                        1 -> {
                            // 优化现有内容
                            ImproveExistingContent(
                                currentTitle = currentTitle,
                                currentContent = currentContent,
                                isGenerating = isGenerating,
                                onImprove = onImprove
                            )
                        }
                    }
                }

                // 加载指示器
                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AI正在生成内容，请稍候...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerateNewContent(
    prompt: String,
    onPromptChange: (String) -> Unit,
    selectedStyle: GenerationStyle,
    onStyleChange: (GenerationStyle) -> Unit,
    showStyleMenu: Boolean,
    onShowStyleMenuChange: (Boolean) -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit
) {
    Column {
        Text(
            text = "输入您的想法或关键词，AI将帮您生成笔记",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 输入框
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("请输入提示词") },
            placeholder = { Text("例如：今日工作总结、旅行计划、学习笔记等") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            enabled = !isGenerating,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 风格选择
        Text(
            text = "选择生成风格",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { onShowStyleMenuChange(true) },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Style, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(getStyleDisplayName(selectedStyle))
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = showStyleMenu,
                onDismissRequest = { onShowStyleMenuChange(false) }
            ) {
                GenerationStyle.values().forEach { style ->
                    DropdownMenuItem(
                        text = {
                            Row {
                                Icon(
                                    getStyleIcon(style),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(getStyleDisplayName(style))
                                    Text(
                                        getStyleDescription(style),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onStyleChange(style)
                            onShowStyleMenuChange(false)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 预设模板
        Text(
            text = "快速模板",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onPromptChange("今日工作总结") },
                label = { Text("工作总结") },
                enabled = !isGenerating
            )
            AssistChip(
                onClick = { onPromptChange("会议纪要") },
                label = { Text("会议纪要") },
                enabled = !isGenerating
            )
            AssistChip(
                onClick = { onPromptChange("学习笔记") },
                label = { Text("学习笔记") },
                enabled = !isGenerating
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onPromptChange("待办事项") },
                label = { Text("待办事项") },
                enabled = !isGenerating
            )
            AssistChip(
                onClick = { onPromptChange("购物清单") },
                label = { Text("购物清单") },
                enabled = !isGenerating
            )
            AssistChip(
                onClick = { onPromptChange("旅行计划") },
                label = { Text("旅行计划") },
                enabled = !isGenerating
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 生成按钮
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            enabled = prompt.isNotBlank() && !isGenerating
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("生成笔记")
        }
    }
}

@Composable
private fun ImproveExistingContent(
    currentTitle: String,
    currentContent: String,
    isGenerating: Boolean,
    onImprove: (ImproveType) -> Unit
) {
    Column {
        // 当前内容预览
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "当前笔记",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTitle.ifEmpty { "无标题" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentContent.take(100) + if (currentContent.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "选择优化方式",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 优化选项
        ImproveOption(
            icon = Icons.Default.Spellcheck,
            title = "语法修正",
            description = "修正拼写和语法错误",
            enabled = !isGenerating,
            onClick = { onImprove(ImproveType.GRAMMAR) }
        )

        ImproveOption(
            icon = Icons.Default.UnfoldMore,
            title = "扩展内容",
            description = "添加更多细节和说明",
            enabled = !isGenerating,
            onClick = { onImprove(ImproveType.EXPAND) }
        )

        ImproveOption(
            icon = Icons.Default.Compress,
            title = "简化内容",
            description = "使内容更加简洁明了",
            enabled = !isGenerating,
            onClick = { onImprove(ImproveType.SIMPLIFY) }
        )

        ImproveOption(
            icon = Icons.Default.AccountTree,
            title = "优化结构",
            description = "重新组织内容结构",
            enabled = !isGenerating,
            onClick = { onImprove(ImproveType.STRUCTURE) }
        )

        ImproveOption(
            icon = Icons.Default.School,
            title = "专业化",
            description = "使用更专业的表达",
            enabled = !isGenerating,
            onClick = { onImprove(ImproveType.PROFESSIONAL) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImproveOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getStyleDisplayName(style: GenerationStyle): String = when (style) {
    GenerationStyle.NORMAL -> "标准风格"
    GenerationStyle.FORMAL -> "正式风格"
    GenerationStyle.CASUAL -> "轻松风格"
    GenerationStyle.CREATIVE -> "创意风格"
    GenerationStyle.SUMMARY -> "总结风格"
    GenerationStyle.DETAILED -> "详细风格"
}

private fun getStyleDescription(style: GenerationStyle): String = when (style) {
    GenerationStyle.NORMAL -> "适合大多数场景"
    GenerationStyle.FORMAL -> "适合商务和学术"
    GenerationStyle.CASUAL -> "适合日常记录"
    GenerationStyle.CREATIVE -> "适合创意写作"
    GenerationStyle.SUMMARY -> "适合快速总结"
    GenerationStyle.DETAILED -> "适合深入分析"
}

private fun getStyleIcon(style: GenerationStyle): androidx.compose.ui.graphics.vector.ImageVector = when (style) {
    GenerationStyle.NORMAL -> Icons.Default.Description
    GenerationStyle.FORMAL -> Icons.Default.Business
    GenerationStyle.CASUAL -> Icons.Default.EmojiEmotions
    GenerationStyle.CREATIVE -> Icons.Default.Palette
    GenerationStyle.SUMMARY -> Icons.Default.Summarize
    GenerationStyle.DETAILED -> Icons.Default.ZoomIn
}