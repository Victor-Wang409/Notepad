package cn.edu.tju.notepad

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
        items(
            items = filteredNotes,
            key = { it.id }
        ) { note ->
            SwipeToDeleteNoteItem(
                note = note,
                onNoteClick = { onNoteClick(note) },
                onDeleteNote = { onDeleteNote(note) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteNoteItem(
    note: NoteBean,
    onNoteClick: () -> Unit,
    onDeleteNote: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope() // 添加协程作用域

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // 从右向左滑动删除
                    showDeleteDialog = true
                    false // 先不执行删除，等待用户确认
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // 从左向右滑动 - 可以添加其他功能，比如标记为重要等
                    false // 暂时不执行任何操作
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.25f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false, // 禁用从左向右的滑动
        enableDismissFromEndToStart = true,   // 启用从右向左的滑动删除
        backgroundContent = {
            SwipeBackground(dismissState = dismissState)
        }
    ) {
        NoteItemCard(
            note = note,
            onClick = onNoteClick
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                // 使用协程重置滑动状态
                scope.launch {
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除笔记 \"${note.title.ifEmpty { "无标题" }}\" 吗？此操作无法撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteNote()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        // 使用协程重置滑动状态
                        scope.launch {
                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                        }
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection ?: return

//    val color by animateColorAsState(
//        targetValue = when (dismissState.targetValue) {
//            SwipeToDismissBoxValue.Settled -> Color.Transparent
//            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
//            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
//        },
//        label = "background_color"
//    )

    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.EndToStart -> {
            androidx.compose.ui.graphics.lerp(
                start = Color.Transparent,
                stop = MaterialTheme.colorScheme.error,
                fraction = dismissState.progress * 3
            )
        }
        SwipeToDismissBoxValue.StartToEnd -> {
            androidx.compose.ui.graphics.lerp(
                start = Color.Transparent,
                stop = MaterialTheme.colorScheme.primary,
                fraction = dismissState.progress
            )
        }
        else -> Color.Transparent
    }

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> Alignment.Center
    }

    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete // 可以改成其他图标
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        SwipeToDismissBoxValue.Settled -> Icons.Default.Delete
    }

    val scale by animateFloatAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.25f else 1f,
        label = "icon_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "删除",
            modifier = Modifier.scale(scale),
            tint = Color.White
        )
    }
}

@Composable
fun NoteItemCard(
    note: NoteBean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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

                // 同步状态指示器（如果需要的话）
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