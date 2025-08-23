package cn.edu.tju.notepad

import java.io.Serializable

data class NoteBean(
    var id: Int = 0,
    var title: String = "",
    var content: String = "",
    var time: String = "",
    var imagePaths: MutableList<String> = mutableListOf(),
    // 新增同步相关字段
    var serverId: Long = 0,  // 服务端ID
    var lastModified: Long = System.currentTimeMillis(),
    var syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    var needsUpload: Boolean = true
) : Serializable {

    constructor(title: String, content: String, time: String) : this(
        id = 0,
        title = title,
        content = content,
        time = time,
        imagePaths = mutableListOf()
    )

    fun addImagePath(imagePath: String) {
        imagePaths.add(imagePath)
        markAsModified()
    }

    fun markAsModified() {
        lastModified = System.currentTimeMillis()
        needsUpload = true
        if (syncStatus == SyncStatus.SYNCED) {
            syncStatus = SyncStatus.MODIFIED
        }
    }

    // 用于将图片路径列表转换为存储用的字符串
    fun getImagePathsAsString(): String {
        return if (imagePaths.isEmpty()) {
            ""
        } else {
            imagePaths.joinToString(";")
        }
    }

    // 转换为API模型
    fun toApiNote(): cn.edu.tju.notepad.network.ApiNote {
        return cn.edu.tju.notepad.network.ApiNote(
            id = serverId,
            title = title,
            content = content,
            time = time,
            imagePaths = getImagePathsAsString(),
            lastModified = lastModified
        )
    }

    companion object {
        // 用于从存储的字符串恢复图片路径列表
        fun parseImagePathsFromString(pathsString: String?): MutableList<String> {
            val paths = mutableListOf<String>()
            if (!pathsString.isNullOrEmpty()) {
                val pathArray = pathsString.split(";")
                for (path in pathArray) {
                    val trimmedPath = path.trim()
                    if (trimmedPath.isNotEmpty()) {
                        paths.add(trimmedPath)
                    }
                }
            }
            return paths
        }

        // 从API模型创建NoteBean
        fun fromApiNote(apiNote: cn.edu.tju.notepad.network.ApiNote): NoteBean {
            return NoteBean(
                id = 0, // 本地ID将在插入时分配
                title = apiNote.title,
                content = apiNote.content,
                time = apiNote.time,
                imagePaths = parseImagePathsFromString(apiNote.imagePaths),
                serverId = apiNote.id,
                lastModified = apiNote.lastModified,
                syncStatus = SyncStatus.SYNCED,
                needsUpload = false
            )
        }
    }
}

enum class SyncStatus {
    LOCAL_ONLY,    // 仅本地
    SYNCED,        // 已同步
    MODIFIED,      // 本地修改
    UPLOADING,     // 上传中
    CONFLICT       // 冲突
}