package cn.edu.tju.notepad

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import cn.edu.tju.notepad.sync.SyncManager
import cn.edu.tju.notepad.sync.SyncResult
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var notes by remember { mutableStateOf<List<NoteBean>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var showSyncSettings by remember { mutableStateOf(false) }

    val noteDbHelper = remember { NoteDbHelper(context) }
    val syncManager = remember { SyncManager(context, noteDbHelper) }

    // 创建刷新笔记列表的函数
    val refreshNotes = remember {
        {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    isLoading = true
                    val notesList = noteDbHelper.query()
                    notes = notesList
                    isLoading = false
                } catch (e: Exception) {
                    e.printStackTrace()
                    isLoading = false
                }
            }
        }
    }

    // 执行同步
    val performSync = remember {
        {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    isSyncing = true
                    val result = syncManager.syncNotes()
                    when (result) {
                        is SyncResult.Success -> {
                            Toast.makeText(context, "同步成功", Toast.LENGTH_SHORT).show()
                            refreshNotes()
                        }
                        is SyncResult.Error -> {
                            Toast.makeText(context, "同步失败: ${result.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "同步异常: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSyncing = false
                }
            }
        }
    }

    // 加载笔记数据
    LaunchedEffect(Unit) {
        refreshNotes()
    }

    // 监听生命周期事件，当从其他Activity返回时自动刷新
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshNotes()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 顶部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 同步按钮
            IconButton(
                onClick = { performSync() },
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = "同步",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 设置按钮
            IconButton(
                onClick = { showSyncSettings = true }
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "同步设置",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 搜索框
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // 内容区域
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (notes.isEmpty()) {
            EmptyNotesView()
        } else {
            NoteListScreen(
                notes = notes,
                onNoteClick = { note ->
                    val intent = Intent(context, NoteActivity::class.java).apply {
                        putExtra("ComeFrom", "NoteAdapter")
                        putExtra("NoteBean", note)
                    }
                    context.startActivity(intent)
                },
                onDeleteNote = { note ->
                    lifecycleOwner.lifecycleScope.launch {
                        try {
                            val result = noteDbHelper.delete(note)
                            if (result > 0) {
                                // 重新加载笔记列表
                                notes = noteDbHelper.query()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                searchQuery = searchQuery
            )
        }
    }

    // 同步设置对话框
    if (showSyncSettings) {
        SyncSettingsDialog(
            syncManager = syncManager,
            onDismiss = { showSyncSettings = false }
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("搜索笔记...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "搜索"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") }
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "清除"
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
            }
        ),
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}

@Composable
fun EmptyNotesView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📝",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有笔记",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角的 + 按钮添加第一条笔记",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SyncSettingsDialog(
    syncManager: SyncManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var serverUrl by remember {
        mutableStateOf(
            context.getSharedPreferences("sync_settings", android.content.Context.MODE_PRIVATE)
                .getString("server_url", "http://192.168.1.100:3000/") ?: ""
        )
    }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("同步设置") },
        text = {
            Column {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("http://192.168.1.100:3000/") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isTestingConnection = true
                        connectionStatus = ""

                        // 测试连接
                        kotlinx.coroutines.GlobalScope.launch {
                            try {
                                syncManager.setServerUrl(serverUrl)
                                val result = syncManager.syncNotes()
                                connectionStatus = when (result) {
                                    is SyncResult.Success -> "连接成功！"
                                    is SyncResult.Error -> "连接失败: ${result.message}"
                                }
                            } catch (e: Exception) {
                                connectionStatus = "连接异常: ${e.message}"
                            } finally {
                                isTestingConnection = false
                            }
                        }
                    },
                    enabled = !isTestingConnection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("测试连接")
                }

                if (connectionStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = connectionStatus,
                        color = if (connectionStatus.contains("成功"))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 保存设置
                    syncManager.setServerUrl(serverUrl)
                    onDismiss()
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