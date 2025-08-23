package cn.edu.tju.notepad

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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

    // 添加同步管理器
    val noteDbHelper = remember { NoteDbHelper(context) }
    val syncManager = remember { SyncManager(context, noteDbHelper) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 现有的用户信息部分保持不变...

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

        Spacer(modifier = Modifier.weight(1f))

        // 应用版本信息
        Text(
            text = "记事本应用 v1.1.0 (支持云同步)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // 所有对话框...
    if (showSyncSettings) {
        SyncSettingsDialog(
            syncManager = syncManager,
            onDismiss = { showSyncSettings = false }
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
        title = {
            Text("关于应用")
        },
        text = {
            Column {
                Text("记事本应用")
                Spacer(modifier = Modifier.height(8.dp))
                Text("版本: 1.0.0")
                Spacer(modifier = Modifier.height(8.dp))
                Text("开发者: TJU")
                Spacer(modifier = Modifier.height(8.dp))
                Text("一个简洁易用的记事本应用，支持文字记录和图片添加。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}
