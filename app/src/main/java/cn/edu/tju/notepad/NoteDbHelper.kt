package cn.edu.tju.notepad;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NoteDbHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "notepad";
    private static final String DATABASE_NAME = "notepadDb";
    private static final int DATABASE_VERSION = 2; // 数据库版本升级
    private SQLiteDatabase sqLiteDatabase;

    public NoteDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        sqLiteDatabase = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title VARCHAR(50)," +
                "content VARCHAR(1000)," +
                "time VARCHAR(30)," +
                "image_paths TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 添加新的列：图片路径
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN image_paths TEXT DEFAULT ''");

            // 更新内容列的长度限制
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO temp_" + TABLE_NAME);
            onCreate(sqLiteDatabase);
            sqLiteDatabase.execSQL("INSERT INTO " + TABLE_NAME + "(_id, title, content, time) " +
                    "SELECT _id, title, content, time FROM temp_" + TABLE_NAME);
            sqLiteDatabase.execSQL("DROP TABLE temp_" + TABLE_NAME);
        }
    }

    public long insert(NoteBean noteBean) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", noteBean.getTitle());
        contentValues.put("content", noteBean.getContent());
        contentValues.put("time", noteBean.getTime());
        contentValues.put("image_paths", noteBean.getImagePathsAsString());
        return sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
    }

    public List<NoteBean> query() {
        List<NoteBean> list = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(TABLE_NAME, null, null, null, null, null, "_id desc", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));

                // 获取图片路径
                String imagePathsStr = "";

                // 检查列是否存在（适应数据库版本迁移）
                if (cursor.getColumnIndex("image_paths") != -1) {
                    imagePathsStr = cursor.getString(cursor.getColumnIndexOrThrow("image_paths"));
                }

                List<String> imagePaths = NoteBean.parseImagePathsFromString(imagePathsStr);

                NoteBean noteBean = new NoteBean(id, title, content, time, imagePaths);
                list.add(noteBean);
            }
            cursor.close();
        }
        return list;
    }

    public long update(NoteBean noteBean) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", noteBean.getTitle());
        contentValues.put("content", noteBean.getContent());
        contentValues.put("time", noteBean.getTime());
        contentValues.put("image_paths", noteBean.getImagePathsAsString());
        return sqLiteDatabase.update(TABLE_NAME, contentValues, "_id=?", new String[]{String.valueOf(noteBean.getId())});
    }

    public long delete(NoteBean noteBean) {
        return sqLiteDatabase.delete(TABLE_NAME, "_id=?", new String[]{String.valueOf(noteBean.getId())});
    }
}