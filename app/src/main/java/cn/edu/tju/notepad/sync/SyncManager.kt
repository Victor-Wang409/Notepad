package cn.edu.tju.notepad.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import cn.edu.tju.notepad.NoteBean
import cn.edu.tju.notepad.NoteDbHelper
import cn.edu.tju.notepad.SyncStatus
import cn.edu.tju.notepad.network.NetworkClient
import cn.edu.tju.notepad.network.CreateNoteResponse
import cn.edu.tju.notepad.network.UpdateNoteResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SyncManager(
    private val context: Context,
    private val noteDbHelper: NoteDbHelper
) {
    private val networkClient = NetworkClient(context)
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SyncManager"
        private const val LAST_SYNC_TIME_KEY = "last_sync_time"
    }

    suspend fun syncNotes(): SyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始同步笔记")

            // 1. 检查网络连接
            val healthResponse = networkClient.apiService.healthCheck()
            if (!healthResponse.isSuccessful) {
                return@withContext SyncResult.Error("服务器连接失败")
            }

            // 2. 获取服务端数据
            val lastSync = getLastSyncTime()
            val serverResponse = networkClient.apiService.getNotes(lastSync)

            if (!serverResponse.isSuccessful) {
                return@withContext SyncResult.Error("获取服务端数据失败: ${serverResponse.code()}")
            }

            val noteResponse = serverResponse.body()
                ?: return@withContext SyncResult.Error("服务端响应为空")

            // 3. 处理服务端数据
            handleServerNotes(noteResponse.notes)

            // 4. 上传本地修改
            uploadLocalChanges()

            // 5. 更新同步时间
            saveLastSyncTime(noteResponse.timestamp)

            Log.d(TAG, "同步完成")
            SyncResult.Success

        } catch (e: Exception) {
            Log.e(TAG, "同步失败", e)
            SyncResult.Error("同步失败: ${e.message}")
        }
    }

    private suspend fun handleServerNotes(serverNotes: List<cn.edu.tju.notepad.network.ApiNote>) {
        serverNotes.forEach { apiNote ->
            val localNote = noteDbHelper.findNoteByServerId(apiNote.id)

            when {
                localNote == null -> {
                    // 新笔记，直接插入
                    val newNote = NoteBean.fromApiNote(apiNote)
                    noteDbHelper.insert(newNote)
                    Log.d(TAG, "插入新笔记: ${newNote.title}")
                }

                localNote.lastModified < apiNote.lastModified -> {
                    // 服务端更新，覆盖本地
                    localNote.title = apiNote.title
                    localNote.content = apiNote.content
                    localNote.time = apiNote.time
                    localNote.imagePaths = NoteBean.parseImagePathsFromString(apiNote.imagePaths)
                    localNote.lastModified = apiNote.lastModified
                    localNote.syncStatus = SyncStatus.SYNCED
                    localNote.needsUpload = false
                    noteDbHelper.update(localNote)
                    Log.d(TAG, "更新笔记: ${localNote.title}")
                }

                localNote.lastModified > apiNote.lastModified -> {
                    // 本地更新，标记需要上传
                    localNote.needsUpload = true
                    noteDbHelper.update(localNote)
                    Log.d(TAG, "本地笔记较新，标记上传: ${localNote.title}")
                }

                else -> {
                    // 时间相同，标记为已同步
                    localNote.syncStatus = SyncStatus.SYNCED
                    localNote.needsUpload = false
                    noteDbHelper.update(localNote)
                }
            }
        }
    }

    private suspend fun uploadLocalChanges() {
        val unsyncedNotes = noteDbHelper.getUnsyncedNotes()
        Log.d(TAG, "需要上传的笔记数量: ${unsyncedNotes.size}")

        unsyncedNotes.forEach { note ->
            try {
                note.syncStatus = SyncStatus.UPLOADING
                noteDbHelper.update(note)

                val response = if (note.serverId == 0L) {
                    // 创建新笔记
                    networkClient.apiService.createNote(note.toApiNote())
                } else {
                    // 更新现有笔记
                    networkClient.apiService.updateNote(note.serverId, note.toApiNote())
                }

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    when (responseBody) {
                        is CreateNoteResponse -> {
                            note.serverId = responseBody.id
                            note.lastModified = responseBody.timestamp
                        }
                        is UpdateNoteResponse -> {
                            note.lastModified = responseBody.timestamp
                        }
                        // 处理Map类型的响应（用于兼容性）
                        is Map<*, *> -> {
                            (responseBody["id"] as? Number)?.let {
                                note.serverId = it.toLong()
                            }
                            (responseBody["timestamp"] as? Number)?.let {
                                note.lastModified = it.toLong()
                            }
                        }
                    }
                    note.syncStatus = SyncStatus.SYNCED
                    note.needsUpload = false
                    noteDbHelper.update(note)
                    Log.d(TAG, "上传成功: ${note.title}")
                } else {
                    note.syncStatus = SyncStatus.LOCAL_ONLY
                    noteDbHelper.update(note)
                    Log.e(TAG, "上传失败: ${note.title}, 错误码: ${response.code()}")
                }

            } catch (e: Exception) {
                note.syncStatus = SyncStatus.LOCAL_ONLY
                noteDbHelper.update(note)
                Log.e(TAG, "上传异常: ${note.title}", e)
            }
        }
    }

    // 上传图片到服务器
    suspend fun uploadImage(imagePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (!file.exists()) return@withContext null

            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

            val response = networkClient.apiService.uploadImage(body)
            if (response.isSuccessful) {
                response.body()?.url
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "图片上传失败", e)
            null
        }
    }

    private fun getLastSyncTime(): Long {
        return sharedPrefs.getLong(LAST_SYNC_TIME_KEY, 0)
    }

    private fun saveLastSyncTime(timestamp: Long) {
        sharedPrefs.edit().putLong(LAST_SYNC_TIME_KEY, timestamp).apply()
    }

    fun setServerUrl(url: String) {
        networkClient.setServerUrl(url)
    }
}

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
}