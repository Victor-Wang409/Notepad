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
    private SQLiteDatabase sqLiteDatabase;

    public NoteDbHelper(@Nullable Context context) {
        super(context, "notepadDb", null, 1);
        sqLiteDatabase = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table "+TABLE_NAME+"(_id integer primary key autoincrement,title varchar(20),content varchar(500),time varchar(30))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public long insert(NoteBean noteBean){
        ContentValues contentValues = new ContentValues();
        contentValues.put("title",noteBean.getTitle());
        contentValues.put("content",noteBean.getContent());
        contentValues.put("time",noteBean.getTime());
        return sqLiteDatabase.insert(TABLE_NAME,null,contentValues);
    }

    public List<NoteBean> query(){
        List<NoteBean> list = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(TABLE_NAME,null,null,null,null,null,"_id desc",null);
        if (cursor!=null){
            while (cursor.moveToNext()){
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                NoteBean noteBean = new NoteBean(id,title,content,time);
                list.add(noteBean);
            }
            cursor.close();
        }
        return list;
    }

    public long update(NoteBean noteBean){
        ContentValues contentValues = new ContentValues();
        contentValues.put("title",noteBean.getTitle());
        contentValues.put("content",noteBean.getContent());
        contentValues.put("time",noteBean.getTime());
        return sqLiteDatabase.update(TABLE_NAME,contentValues,"_id=?",new String[]{String.valueOf(noteBean.getId())});
    }

    public long delete(NoteBean noteBean){
        return sqLiteDatabase.delete(TABLE_NAME,"_id=?",new String[]{String.valueOf(noteBean.getId())});
    }
}
