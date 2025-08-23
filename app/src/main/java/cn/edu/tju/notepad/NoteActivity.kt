package cn.edu.tju.notepad

import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import cn.edu.tju.notepad.sync.SyncManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NoteActivity : ComponentActivity() {

    private lateinit var noteDbHelper: NoteDbHelper
    private lateinit var syncManager: SyncManager
    private var noteBean: NoteBean? = null
    private var comeFrom: String = ""

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleSelectedImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        noteDbHelper = NoteDbHelper(this)
        syncManager = SyncManager(this, noteDbHelper)
        comeFrom = intent.getStringExtra("ComeFrom") ?: ""
        noteBean = intent.getSerializableExtra("NoteBean", NoteBean::class.java)

        setContent {
            NotepadTheme {
                NoteEditScreen(
                    noteBean = noteBean,
                    comeFrom = comeFrom,
                    onBackPressed = { finish() },
                    onSaveNote = { title, content, imagePaths ->
                        saveNote(title, content, imagePaths)
                    },
                    onAddImage = { addImage() },
                    onDeleteImage = { imagePath ->
                        // 处理图片删除
                        deleteImageFile(imagePath)
                    }
                )
            }
        }
    }

    private fun addImage() {
        if (ImageUtils.checkStoragePermission(this)) {
            imagePickerLauncher.launch("image/*")
        } else {
            ImageUtils.requestStoragePermission(this)
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val imagePath = ImageUtils.copyUriToPrivateStorage(this, uri)
            if (imagePath.isNotEmpty()) {
                // 更新当前笔记的图片路径列表
                if (noteBean == null) {
                    noteBean = NoteBean(
                        id = 0,
                        title = "",
                        content = "",
                        time = "",
                        imagePaths = mutableListOf(imagePath),
                        serverId = 0,
                        lastModified = System.currentTimeMillis(),
                        syncStatus = SyncStatus.LOCAL_ONLY,
                        needsUpload = true
                    )
                } else {
                    noteBean?.imagePaths?.add(imagePath)
                    noteBean?.markAsModified()
                }
                Toast.makeText(this, "图片添加成功", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "图片添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveNote(title: String, content: String, imagePaths: List<String>) {
        if (title.trim().isEmpty() && content.trim().isEmpty()) {
            Toast.makeText(this, "标题和内容不能都为空", Toast.LENGTH_SHORT).show()
            return
        }

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        try {
            when (comeFrom) {
                "Add" -> {
                    val newNote = NoteBean(
                        id = 0,
                        title = title,
                        content = content,
                        time = currentTime,
                        imagePaths = imagePaths.toMutableList(),
                        serverId = 0,
                        lastModified = System.currentTimeMillis(),
                        syncStatus = SyncStatus.LOCAL_ONLY,
                        needsUpload = true
                    )
                    val result = noteDbHelper.insert(newNote)
                    if (result > 0) {
                        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()

                        // 后台同步（可选）
                        lifecycleScope.launch {
                            try {
                                syncManager.syncNotes()
                            } catch (e: Exception) {
                                // 静默处理同步错误，不影响用户体验
                            }
                        }

                        finish()
                    } else {
                        Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
                    }
                }
                "NoteAdapter" -> {
                    noteBean?.let { note ->
                        val hasChanges = note.title != title ||
                                note.content != content ||
                                note.imagePaths != imagePaths

                        if (hasChanges) {
                            note.title = title
                            note.content = content
                            note.time = currentTime
                            note.imagePaths = imagePaths.toMutableList()
                            // 标记为已修改，需要同步
                            note.markAsModified()

                            val result = noteDbHelper.update(note)
                            if (result > 0) {
                                Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show()

                                // 后台同步（可选）
                                lifecycleScope.launch {
                                    try {
                                        syncManager.syncNotes()
                                    } catch (e: Exception) {
                                        // 静默处理同步错误，不影响用户体验
                                    }
                                }

                                finish()
                            } else {
                                Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            finish()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteImageFile(imagePath: String) {
        try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ImageUtils.REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    Toast.makeText(this, "需要存储权限才能添加图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteBean: NoteBean?,
    comeFrom: String,
    onBackPressed: () -> Unit,
    onSaveNote: (String, String, List<String>) -> Unit,
    onAddImage: () -> Unit,
    onDeleteImage: (String) -> Unit
) {
    var title by remember { mutableStateOf(noteBean?.title ?: "") }
    var content by remember { mutableStateOf(noteBean?.content ?: "") }
    var imagePaths by remember { mutableStateOf(noteBean?.imagePaths?.toMutableList() ?: mutableListOf<String>()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 监听noteBean的变化，更新imagePaths
    LaunchedEffect(noteBean?.imagePaths) {
        noteBean?.imagePaths?.let { paths ->
            imagePaths = paths.toMutableList()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (comeFrom == "Add") "新建笔记" else "编辑笔记",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (comeFrom == "NoteAdapter" && noteBean != null) {
                                val hasChanges = noteBean.title != title ||
                                        noteBean.content != content ||
                                        noteBean.imagePaths != imagePaths
                                if (hasChanges) {
                                    showConfirmDialog = true
                                } else {
                                    onBackPressed()
                                }
                            } else {
                                onSaveNote(title, content, imagePaths.toList())
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddImage,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加图片")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题输入框
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            // 内容输入框
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(bottom = 16.dp),
                maxLines = Int.MAX_VALUE
            )

            // 图片列表
            if (imagePaths.isNotEmpty()) {
                Text(
                    text = "图片 (${imagePaths.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(imagePaths.size) { index ->
                        val imagePath = imagePaths[index]
                        ImageItem(
                            imagePath = imagePath,
                            onDeleteImage = { pathToDelete ->
                                imagePaths.remove(pathToDelete)
                                onDeleteImage(pathToDelete)
                            }
                        )
                    }
                }
            }
        }
    }

    // 确认对话框
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("保存修改") },
            text = { Text("是否需要保存修改？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onSaveNote(title, content, imagePaths.toList())
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onBackPressed()
                    }
                ) {
                    Text("不保存")
                }
            }
        )
    }
}

@Composable
fun ImageItem(
    imagePath: String,
    onDeleteImage: (String) -> Unit
) {
    Box(
        modifier = Modifier.size(120.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            val bitmap = remember(imagePath) {
                try {
                    val file = File(imagePath)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(imagePath)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "图片加载失败",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // 删除按钮
        IconButton(
            onClick = { onDeleteImage(imagePath) },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除图片",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}