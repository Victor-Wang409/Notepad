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
        private const val DATABASE_VERSION = 2
    }

    private val sqLiteDatabase: SQLiteDatabase = this.writableDatabase

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = "CREATE TABLE $TABLE_NAME(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title VARCHAR(50)," +
                "content VARCHAR(1000)," +
                "time VARCHAR(30)," +
                "image_paths TEXT)"
        db.execSQL(createTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN image_paths TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_NAME RENAME TO temp_$TABLE_NAME")
            onCreate(db)
            db.execSQL(
                "INSERT INTO $TABLE_NAME(_id, title, content, time) " +
                        "SELECT _id, title, content, time FROM temp_$TABLE_NAME"
            )
            db.execSQL("DROP TABLE temp_$TABLE_NAME")
        }
    }

    fun insert(noteBean: NoteBean): Long {
        val contentValues = ContentValues()
        contentValues.put("title", noteBean.title)
        contentValues.put("content", noteBean.content)
        contentValues.put("time", noteBean.time)
        contentValues.put("image_paths", noteBean.getImagePathsAsString())
        return sqLiteDatabase.insert(TABLE_NAME, null, contentValues)
    }

    fun query(): List<NoteBean> {
        val list = mutableListOf<NoteBean>()
        val cursor: Cursor? = sqLiteDatabase.query(
            TABLE_NAME, null, null, null, null, null, "_id desc", null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                val content = it.getString(it.getColumnIndexOrThrow("content"))
                val time = it.getString(it.getColumnIndexOrThrow("time"))
                val id = it.getInt(it.getColumnIndexOrThrow("_id"))

                val imagePathsStr = if (it.getColumnIndex("image_paths") != -1) {
                    it.getString(it.getColumnIndexOrThrow("image_paths")) ?: ""
                } else {
                    ""
                }

                val imagePaths = NoteBean.parseImagePathsFromString(imagePathsStr)
                val noteBean = NoteBean(id, title, content, time, imagePaths)
                list.add(noteBean)
            }
        }
        return list
    }

    fun update(noteBean: NoteBean): Long {
        val contentValues = ContentValues()
        contentValues.put("title", noteBean.title)
        contentValues.put("content", noteBean.content)
        contentValues.put("time", noteBean.time)
        contentValues.put("image_paths", noteBean.getImagePathsAsString())
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
}
