package cn.edu.tju.notepad

import java.io.Serializable

data class NoteBean(
    var id: Int = 0,
    var title: String = "",
    var content: String = "",
    var time: String = "",
    var imagePaths: MutableList<String> = mutableListOf()
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
    }

    // 用于将图片路径列表转换为存储用的字符串
    fun getImagePathsAsString(): String {
        return if (imagePaths.isEmpty()) {
            ""
        } else {
            imagePaths.joinToString(";")
        }
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
    }
}