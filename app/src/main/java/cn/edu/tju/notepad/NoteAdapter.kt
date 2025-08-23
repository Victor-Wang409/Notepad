package cn.edu.tju.notepad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun NoteListScreen(
    notes: List<NoteBean>,
    onNoteClick: (NoteBean) -> Unit,
    onDeleteNote: (NoteBean) -> Unit,
    searchQuery: String = ""
) {
    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isEmpty()) {
            notes
        } else {
            val query = searchQuery.lowercase(Locale.getDefault())
            notes.filter { note ->
                note.title.lowercase(Locale.getDefault()).contains(query) ||
                note.content.lowercase(Locale.getDefault()).contains(query)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredNotes, key = { it.id }) { note ->
            NoteItemCard(
                note = note,
                onClick = { onNoteClick(note) },
                onDelete = { onDeleteNote(note) }
            )
        }
    }
}

@Composable
fun NoteItemCard(
    note: NoteBean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 标题和同步状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title.ifEmpty { "无标题" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // 同步状态指示器
                    SyncStatusIndicator(syncStatus = note.syncStatus)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 内容预览
                if (note.content.isNotEmpty()) {
                    Text(
                        text = note.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 图片数量提示
                if (note.imagePaths.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📷 ${note.imagePaths.size} 张图片",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 时间
                Text(
                    text = note.time,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 删除按钮
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条笔记吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SyncStatusIndicator(syncStatus: SyncStatus) {
    val (icon, color, description) = when (syncStatus) {
        SyncStatus.LOCAL_ONLY -> Triple("📱", MaterialTheme.colorScheme.outline, "仅本地")
        SyncStatus.SYNCED -> Triple("✅", MaterialTheme.colorScheme.primary, "已同步")
        SyncStatus.MODIFIED -> Triple("📝", MaterialTheme.colorScheme.tertiary, "待同步")
        SyncStatus.UPLOADING -> Triple("⏳", MaterialTheme.colorScheme.secondary, "同步中")
        SyncStatus.CONFLICT -> Triple("⚠️", MaterialTheme.colorScheme.error, "冲突")
    }

    Text(
        text = icon,
        fontSize = 16.sp,
        color = color
    )
}
