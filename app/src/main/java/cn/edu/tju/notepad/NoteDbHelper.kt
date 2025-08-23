package cn.edu.tju.notepad

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NoteDbHelper(context: Context?) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val TABLE_NAME = "notepad"
        private const val DATABASE_NAME = "notepadDb"
        private const val DATABASE_VERSION = 3 // 增加版本号
    }

    private val sqLiteDatabase: SQLiteDatabase = this.writableDatabase

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = "CREATE TABLE $TABLE_NAME(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title VARCHAR(50)," +
                "content VARCHAR(1000)," +
                "time VARCHAR(30)," +
                "image_paths TEXT," +
                "server_id INTEGER DEFAULT 0," +
                "last_modified INTEGER DEFAULT 0," +
                "sync_status INTEGER DEFAULT 0," +
                "needs_upload INTEGER DEFAULT 1)"
        db.execSQL(createTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // 添加image_paths字段
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN image_paths TEXT DEFAULT ''")
            } catch (e: Exception) {
                // 如果字段已存在，忽略错误
            }
        }
        if (oldVersion < 3) {
            // 添加同步相关字段
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN server_id INTEGER DEFAULT 0")
            } catch (e: Exception) {
                // 字段已存在，忽略
            }
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN last_modified INTEGER DEFAULT 0")
            } catch (e: Exception) {
                // 字段已存在，忽略
            }
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN sync_status INTEGER DEFAULT 0")
            } catch (e: Exception) {
                // 字段已存在，忽略
            }
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN needs_upload INTEGER DEFAULT 1")
            } catch (e: Exception) {
                // 字段已存在，忽略
            }
        }
    }

    fun insert(noteBean: NoteBean): Long {
        val contentValues = ContentValues().apply {
            put("title", noteBean.title)
            put("content", noteBean.content)
            put("time", noteBean.time)
            put("image_paths", noteBean.getImagePathsAsString())
            put("server_id", noteBean.serverId)
            put("last_modified", noteBean.lastModified)
            put("sync_status", noteBean.syncStatus.ordinal)
            put("needs_upload", if (noteBean.needsUpload) 1 else 0)
        }
        return sqLiteDatabase.insert(TABLE_NAME, null, contentValues)
    }

    fun query(): List<NoteBean> {
        val list = mutableListOf<NoteBean>()
        val cursor: Cursor? = sqLiteDatabase.query(
            TABLE_NAME, null, null, null, null, null, "_id desc", null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val noteBean = cursorToNoteBean(it)
                list.add(noteBean)
            }
        }
        return list
    }

    fun update(noteBean: NoteBean): Long {
        val contentValues = ContentValues().apply {
            put("title", noteBean.title)
            put("content", noteBean.content)
            put("time", noteBean.time)
            put("image_paths", noteBean.getImagePathsAsString())
            put("server_id", noteBean.serverId)
            put("last_modified", noteBean.lastModified)
            put("sync_status", noteBean.syncStatus.ordinal)
            put("needs_upload", if (noteBean.needsUpload) 1 else 0)
        }
        return sqLiteDatabase.update(
            TABLE_NAME,
            contentValues,
            "_id=?",
            arrayOf(noteBean.id.toString())
        ).toLong()
    }

    fun delete(noteBean: NoteBean): Long {
        return sqLiteDatabase.delete(
            TABLE_NAME,
            "_id=?",
            arrayOf(noteBean.id.toString())
        ).toLong()
    }

    // 查找需要上传的笔记
    fun getUnsyncedNotes(): List<NoteBean> {
        val list = mutableListOf<NoteBean>()
        val cursor = sqLiteDatabase.query(
            TABLE_NAME,
            null,
            "needs_upload = 1",
            null,
            null,
            null,
            "_id ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val noteBean = cursorToNoteBean(it)
                list.add(noteBean)
            }
        }
        return list
    }

    // 根据服务端ID查找笔记
    fun findNoteByServerId(serverId: Long): NoteBean? {
        val cursor = sqLiteDatabase.query(
            TABLE_NAME,
            null,
            "server_id = ?",
            arrayOf(serverId.toString()),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return cursorToNoteBean(it)
            }
        }
        return null
    }

    // 根据本地ID查找笔记
    fun findNoteById(id: Int): NoteBean? {
        val cursor = sqLiteDatabase.query(
            TABLE_NAME,
            null,
            "_id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return cursorToNoteBean(it)
            }
        }
        return null
    }

    private fun cursorToNoteBean(cursor: Cursor): NoteBean {
        val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
        val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
        val time = cursor.getString(cursor.getColumnIndexOrThrow("time"))
        val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))

        val imagePathsStr = getColumnValue(cursor, "image_paths", "")
        val serverId = getColumnValue(cursor, "server_id", 0L)
        val lastModified = getColumnValue(cursor, "last_modified", System.currentTimeMillis())
        val syncStatusOrdinal = getColumnValue(cursor, "sync_status", 0)
        val needsUpload = getColumnValue(cursor, "needs_upload", 1) == 1

        val imagePaths = NoteBean.parseImagePathsFromString(imagePathsStr)
        val syncStatus = try {
            SyncStatus.values()[syncStatusOrdinal]
        } catch (e: Exception) {
            SyncStatus.LOCAL_ONLY
        }

        return NoteBean(
            id = id,
            title = title,
            content = content,
            time = time,
            imagePaths = imagePaths,
            serverId = serverId,
            lastModified = lastModified,
            syncStatus = syncStatus,
            needsUpload = needsUpload
        )
    }

    // 安全获取列值的辅助方法
    private fun getColumnValue(cursor: Cursor, columnName: String, defaultValue: String): String {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) {
                cursor.getString(columnIndex) ?: defaultValue
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun getColumnValue(cursor: Cursor, columnName: String, defaultValue: Long): Long {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) {
                cursor.getLong(columnIndex)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun getColumnValue(cursor: Cursor, columnName: String, defaultValue: Int): Int {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) {
                cursor.getInt(columnIndex)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }
}