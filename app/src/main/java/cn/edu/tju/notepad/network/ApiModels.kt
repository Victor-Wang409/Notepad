package cn.edu.tju.notepad.network

import com.google.gson.annotations.SerializedName

data class ApiNote(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val time: String = "",
    @SerializedName("image_paths")
    val imagePaths: String = "",
    @SerializedName("last_modified")
    val lastModified: Long = System.currentTimeMillis()
)

data class NoteResponse(
    val notes: List<ApiNote>,
    val timestamp: Long
)

data class CreateNoteResponse(
    val id: Long,
    val timestamp: Long
)

data class UpdateNoteResponse(
    val changes: Int,
    val timestamp: Long
)

data class UploadResponse(
    val filename: String,
    val url: String
)