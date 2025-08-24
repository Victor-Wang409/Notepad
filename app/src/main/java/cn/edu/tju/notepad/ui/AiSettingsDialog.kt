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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.edu.tju.notepad.ai.AiConfigManager
import cn.edu.tju.notepad.ai.ModelType
import cn.edu.tju.notepad.ai.WenxinApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configManager = remember { AiConfigManager(context) }
    val scope = rememberCoroutineScope()

    var appId by remember { mutableStateOf(configManager.getAppId()) }
    var bearerToken by remember { mutableStateOf(configManager.getBearerToken()) }
    var showBearerToken by remember { mutableStateOf(false) }
    var aiEnabled by remember { mutableStateOf(configManager.isAiEnabled()) }
    var selectedModel by remember { mutableStateOf(configManager.getModelType()) }
    var showModelMenu by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // 标题栏
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
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
                                        text = "如何获取认证信息？",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "1. 访问百度千帆大模型平台\n" +
                                                "2. 创建应用并获取访问凭证\n" +
                                                "3. Bearer Token格式: bce-v3/ALTAK-xxx/xxx\n" +
                                                "4. App ID为可选项，某些场景下需要",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // App ID输入（可选）
                        OutlinedTextField(
                            value = appId,
                            onValueChange = { appId = it },
                            label = { Text("App ID (可选)") },
                            placeholder = { Text("留空或输入App ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            supportingText = {
                                Text("App ID是可选的，某些API调用可能需要")
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bearer Token输入
                        OutlinedTextField(
                            value = bearerToken,
                            onValueChange = { bearerToken = it },
                            label = { Text("Bearer Token *") },
                            placeholder = { Text("bce-v3/ALTAK-xxx/xxx") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showBearerToken)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showBearerToken = !showBearerToken }) {
                                    Icon(
                                        if (showBearerToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showBearerToken) "隐藏" else "显示"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            isError = bearerToken.isNotEmpty() && !configManager.validateBearerToken(bearerToken),
                            supportingText = {
                                if (bearerToken.isNotEmpty() && !configManager.validateBearerToken(bearerToken)) {
                                    Text("Token格式不正确，应为: bce-v3/ALTAK-xxx/xxx",
                                        color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("必填项，用于API认证")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 模型选择
                        Text(
                            text = "选择AI模型",
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

                        // 高级设置
                        Text(
                            text = "高级设置",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "保存生成历史",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = configManager.isSaveHistoryEnabled(),
                                        onCheckedChange = { configManager.setSaveHistoryEnabled(it) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "自动建议（实验性）",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = configManager.isAutoSuggestEnabled(),
                                        onCheckedChange = { configManager.setAutoSuggestEnabled(it) }
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
                                    configManager.setAppId(appId)
                                    configManager.setBearerToken(bearerToken)

                                    // 测试连接
                                    val client = WenxinApiClient(context)
                                    val success = client.testConnection()

                                    testResult = if (success) {
                                        configManager.saveTestResult(true, "连接成功")
                                        "✅ 连接成功！AI功能已就绪"
                                    } else {
                                        configManager.saveTestResult(false, "连接失败")
                                        "❌ 连接失败，请检查Bearer Token是否正确"
                                    }
                                    isTesting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTesting &&
                                    bearerToken.isNotEmpty() &&
                                    configManager.validateBearerToken(bearerToken)
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 显示当前状态
                    if (configManager.isConfigured()) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "已配置",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // 保存配置
                            configManager.setAppId(appId)
                            configManager.setBearerToken(bearerToken)
                            configManager.setAiEnabled(aiEnabled)
                            configManager.setModelType(selectedModel)
                            onDismiss()
                        },
                        enabled = bearerToken.isEmpty() || configManager.validateBearerToken(bearerToken)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

private fun getModelDescription(model: ModelType): String = when (model) {
    ModelType.ERNIE_4_0_8K -> "最新最强，支持8K上下文"
    ModelType.ERNIE_4_0 -> "强大的4.0版本，适合复杂任务"
    ModelType.ERNIE_3_5_8K -> "3.5版本8K上下文，平衡之选"
    ModelType.ERNIE_3_5 -> "稳定可靠，推荐日常使用"
    ModelType.ERNIE_TURBO -> "快速响应，适合简单任务"
    ModelType.ERNIE_SPEED -> "极速版本，适合实时交互"
}