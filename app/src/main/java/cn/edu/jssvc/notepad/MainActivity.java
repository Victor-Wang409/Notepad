package cn.edu.jssvc.notepad;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    NoteDbHelper noteDbHelper;
    RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        noteDbHelper = new NoteDbHelper(MainActivity.this);

        recyclerView = findViewById(R.id.recycleView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        ImageView imageViewAdd = findViewById(R.id.imageViewAdd);

        imageViewAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,NoteActivity.class);
                intent.putExtra("ComeFrom","Add");
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        List<NoteBean> list = noteDbHelper.query();
        NoteAdapter noteAdapter = new NoteAdapter(list);
        recyclerView.setAdapter(noteAdapter);
    }
}