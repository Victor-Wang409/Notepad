package cn.edu.tju.notepad

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.tju.notepad.sync.SyncManager
import cn.edu.tju.notepad.ai.AiConfigManager
import cn.edu.tju.notepad.ui.AiSettingsDialog

@Composable
fun UserScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    var username by remember {
        mutableStateOf(sharedPreferences.getString("username", "未设置用户名") ?: "未设置用户名")
    }
    var email by remember {
        mutableStateOf(sharedPreferences.getString("email", "未设置邮箱") ?: "未设置邮箱")
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSyncSettings by remember { mutableStateOf(false) }
    var showAiSettings by remember { mutableStateOf(false) }

    // 添加管理器
    val noteDbHelper = remember { NoteDbHelper(context) }
    val syncManager = remember { SyncManager(context, noteDbHelper) }
    val aiConfigManager = remember { AiConfigManager(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户头像
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "用户头像",
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 用户信息
        Text(
            text = username,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = email,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 用户信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                UserInfoItem(
                    label = "用户名",
                    value = username,
                    icon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                UserInfoItem(
                    label = "邮箱",
                    value = email,
                    icon = Icons.Default.Email
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 编辑信息按钮
        Button(
            onClick = { showEditDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("编辑个人信息")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI设置按钮
        OutlinedButton(
            onClick = { showAiSettings = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (aiConfigManager.isAiEnabled())
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (aiConfigManager.isAiEnabled())
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("AI智能助手")
                if (aiConfigManager.isAiEnabled()) {
                    Text(
                        "已启用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 同步设置按钮
        OutlinedButton(
            onClick = { showSyncSettings = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.CloudSync,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("云同步设置")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 关于按钮
        OutlinedButton(
            onClick = { showAboutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("关于应用")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 应用版本信息
        Text(
            text = "智能记事本 v2.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "支持AI生成 · 云同步 · 图片附件",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // 编辑用户信息对话框
    if (showEditDialog) {
        EditUserInfoDialog(
            currentUsername = username,
            currentEmail = email,
            onDismiss = { showEditDialog = false },
            onSave = { newUsername, newEmail ->
                username = newUsername
                email = newEmail
                sharedPreferences.edit()
                    .putString("username", newUsername)
                    .putString("email", newEmail)
                    .apply()
                showEditDialog = false
            }
        )
    }

    // AI设置对话框
    if (showAiSettings) {
        AiSettingsDialog(
            onDismiss = { showAiSettings = false }
        )
    }

    // 同步设置对话框
    if (showSyncSettings) {
        SyncSettingsDialog(
            syncManager = syncManager,
            onDismiss = { showSyncSettings = false }
        )
    }

    // 关于对话框
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
fun UserInfoItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EditUserInfoDialog(
    currentUsername: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var username by remember { mutableStateOf(TextFieldValue(currentUsername)) }
    var email by remember { mutableStateOf(TextFieldValue(currentEmail)) }
    var usernameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("编辑个人信息")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = it.text.isBlank()
                    },
                    label = { Text("用户名") },
                    isError = usernameError,
                    supportingText = {
                        if (usernameError) {
                            Text("用户名不能为空")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (username.text.isNotBlank()) {
                        onSave(username.text, email.text)
                    } else {
                        usernameError = true
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("智能记事本")
        },
        text = {
            Column {
                Text(
                    text = "版本: 2.0.0",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("开发者: TJU Team")
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "功能特性：",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("• AI智能生成与优化笔记")
                Text("• 支持百度文心一言大模型")
                Text("• 云端同步，多设备访问")
                Text("• 图片附件支持")
                Text("• Markdown格式支持")

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "一个智能、高效、安全的笔记应用，让记录变得更简单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}