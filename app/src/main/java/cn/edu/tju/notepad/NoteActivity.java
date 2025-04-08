package cn.edu.tju.notepad;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;

import cn.edu.jssvc.notepad.R;

public class NoteActivity extends AppCompatActivity {
    private EditText editTextTitle,editTextContent;
    NoteDbHelper noteDbHelper;  // 数据库
    private NoteBean noteBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        ImageView imageViewBack = findViewById(R.id.imageViewBack);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContent = findViewById(R.id.editTextContent);

        Intent intent = getIntent();
        String comeFrom = intent.getStringExtra("ComeFrom");
        assert comeFrom != null;
        if (comeFrom.equals("NoteAdapter")){
            noteBean = (NoteBean) intent.getSerializableExtra("NoteBean");
            assert noteBean != null;
            editTextTitle.setText(noteBean.getTitle());
            editTextContent.setText(noteBean.getContent());
        }


        noteDbHelper =  new NoteDbHelper(NoteActivity.this);

        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button buttonCommit = findViewById(R.id.buttonCommit);

        buttonCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //拿到用户的输入
                String title = editTextTitle.getText().toString().trim();
                String content = editTextContent.getText().toString().trim();

                if (title.isEmpty() || content.isEmpty()){
                    Toast.makeText(NoteActivity.this, "标题或内容为空，请补充后再发布", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (comeFrom.equals("Add")){
                        //插入数据库
                        String time = String.valueOf(new Date());

                        NoteBean noteBean = new NoteBean(title,content,time);
                        long ll = noteDbHelper.insert(noteBean);
                        if (ll>0){
                            Toast.makeText(NoteActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else {
                            Toast.makeText(NoteActivity.this, "发布失败，请重新发布", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else if (comeFrom.equals("NoteAdapter")){
                        if (title.equals(noteBean.getTitle()) && content.equals(noteBean.getContent())){
                            Toast.makeText(NoteActivity.this, "没有修改", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
                            builder.setTitle("是否需要修改？");
                            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    noteBean.setTitle(title);
                                    noteBean.setContent(content);
                                    noteBean.setTime(String.valueOf(new Date()));
                                    long lows = noteDbHelper.update(noteBean);
                                    if (lows>0){
                                        Toast.makeText(NoteActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                    else {
                                        Toast.makeText(NoteActivity.this, "修改失败，请重试！", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });
                            builder.setNegativeButton("取消",null);
                            AlertDialog alertDialog = builder.create();
                            alertDialog.show();
                        }
                    }

                }
            }
        });
    }
}