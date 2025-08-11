package cn.edu.tju.notepad

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {

    // 请求码常量
    const val REQUEST_IMAGE_PICK = 1002
    const val REQUEST_STORAGE_PERMISSION = 1004

    /**
     * 检查并请求存储权限
     */
    fun checkStoragePermission(activity: Activity): Boolean {
        return when {
            true -> { // Android 13及以上
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        REQUEST_STORAGE_PERMISSION
                    )
                    false
                } else {
                    true
                }
            }
            else -> { // Android 9及以下
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ),
                        REQUEST_STORAGE_PERMISSION
                    )
                    false
                } else {
                    true
                }
            }
        }
    }

    /**
     * 从图库选择图片
     */
    fun pickImageFromGallery(activity: Activity) {
        if (checkStoragePermission(activity)) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activity.startActivityForResult(intent, REQUEST_IMAGE_PICK)
        } else {
            Toast.makeText(activity, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 从URI复制文件到应用私有存储
     */
    fun copyUriToPrivateStorage(context: Context, uri: Uri): String {
        val contentResolver: ContentResolver = context.contentResolver
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: return ""

        val file = File(directory, fileName)

        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return ""
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024) // 4k缓冲区
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }

            outputStream.flush()
            inputStream.close()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "保存图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            ""
        }
    }

    /**
     * 请求存储权限（用于兼容性，实际权限检查在checkStoragePermission中完成）
     */
    fun requestStoragePermission(context: Context) {
        // 这个方法主要用于兼容性，实际的权限请求在checkStoragePermission中处理
        Toast.makeText(context, "请允许存储权限以添加图片", Toast.LENGTH_SHORT).show()
    }
}