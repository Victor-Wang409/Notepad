package cn.edu.tju.notepad.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cn.edu.tju.notepad.ai.AiConfigManager
import cn.edu.tju.notepad.ai.ModelType
import cn.edu.tju.notepad.ai.WenxinApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configManager = remember { AiConfigManager(context) }
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(configManager.getApiKey()) }
    var secretKey by remember { mutableStateOf(configManager.getSecretKey()) }
    var showApiKey by remember { mutableStateOf(false) }
    var showSecretKey by remember { mutableStateOf(false) }
    var aiEnabled by remember { mutableStateOf(configManager.isAiEnabled()) }
    var selectedModel by remember { mutableStateOf(configManager.getModelType()) }
    var showModelMenu by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 设置",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI功能开关
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (aiEnabled)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "启用 AI 功能",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "开启后可使用AI生成和优化笔记",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = aiEnabled,
                            onCheckedChange = {
                                aiEnabled = it
                                configManager.setAiEnabled(it)
                            }
                        )
                    }
                }

                if (aiEnabled) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // API配置说明
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "如何获取API密钥？",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. 访问百度智能云控制台\n" +
                                            "2. 创建文心一言应用\n" +
                                            "3. 获取API Key和Secret Key",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // API Key输入
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("请输入API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showApiKey)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) "隐藏" else "显示"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        isError = apiKey.isNotEmpty() && !configManager.validateApiKey(apiKey),
                        supportingText = {
                            if (apiKey.isNotEmpty() && !configManager.validateApiKey(apiKey)) {
                                Text("API Key格式不正确")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secret Key输入
                    OutlinedTextField(
                        value = secretKey,
                        onValueChange = { secretKey = it },
                        label = { Text("Secret Key") },
                        placeholder = { Text("请输入Secret Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showSecretKey)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showSecretKey = !showSecretKey }) {
                                Icon(
                                    if (showSecretKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showSecretKey) "隐藏" else "显示"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        isError = secretKey.isNotEmpty() && !configManager.validateSecretKey(secretKey),
                        supportingText = {
                            if (secretKey.isNotEmpty() && !configManager.validateSecretKey(secretKey)) {
                                Text("Secret Key格式不正确")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 模型选择
                    Text(
                        text = "选择模型",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box {
                        OutlinedButton(
                            onClick = { showModelMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedModel.modelName)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            ModelType.values().forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.modelName)
                                            Text(
                                                text = getModelDescription(model),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedModel = model
                                        configManager.setModelType(model)
                                        showModelMenu = false
                                    },
                                    leadingIcon = {
                                        if (model == selectedModel) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 测试连接按钮
                    Button(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                testResult = null

                                // 保存配置
                                configManager.setApiKey(apiKey)
                                configManager.setSecretKey(secretKey)

                                // 测试连接
                                val client = WenxinApiClient(context)
                                val result = client.generateNoteContent(
                                    "测试连接",
                                    cn.edu.tju.notepad.ai.GenerationStyle.NORMAL
                                )

                                testResult = if (result != null) {
                                    "✅ 连接成功！AI功能已就绪"
                                } else {
                                    "❌ 连接失败，请检查API密钥"
                                }
                                isTesting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isTesting &&
                                configManager.validateApiKey(apiKey) &&
                                configManager.validateSecretKey(secretKey)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("测试中...")
                        } else {
                            Icon(Icons.Default.CloudQueue, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("测试连接")
                        }
                    }

                    // 测试结果
                    testResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.startsWith("✅"))
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = result,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // 保存配置
                            configManager.setApiKey(apiKey)
                            configManager.setSecretKey(secretKey)
                            configManager.setAiEnabled(aiEnabled)
                            configManager.setModelType(selectedModel)
                            onDismiss()
                        }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

private fun getModelDescription(model: ModelType): String = when (model) {
    ModelType.ERNIE_4_0 -> "最新最强大的模型，适合复杂任务"
}