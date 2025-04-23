package cn.edu.tju.notepad;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private TextView titleTextView;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddNote;

    private NoteFragment noteFragment;
    private UserFragment userFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        titleTextView = findViewById(R.id.textView);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAddNote = findViewById(R.id.fab_add_note);

        // Initialize fragments
        if (savedInstanceState == null) {
            // 只在首次创建时初始化Fragment，避免重复创建
            noteFragment = new NoteFragment();
            userFragment = new UserFragment();

            // 初始化时添加所有Fragment并隐藏非默认的Fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.fragment_container, userFragment).hide(userFragment);
            transaction.add(R.id.fragment_container, noteFragment);
            transaction.commit();

            activeFragment = noteFragment;
        }

        // Set up bottom navigation
        setupBottomNavigation();

        // Set up FAB
        setupFAB();

        // 设置默认标题
        setTitle("笔记");
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.navigation_notes) {
                    switchFragment(noteFragment);
                    setTitle("笔记");
                    fabAddNote.setVisibility(View.VISIBLE);
                    return true;
                } else if (id == R.id.navigation_user) {
                    switchFragment(userFragment);
                    setTitle("用户");
                    fabAddNote.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        });
    }

    private void setupFAB() {
        fabAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                intent.putExtra("ComeFrom", "Add");
                startActivity(intent);
            }
        });
    }

    private void switchFragment(Fragment targetFragment) {
        if (activeFragment == targetFragment) return;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(activeFragment).show(targetFragment).commit();
        activeFragment = targetFragment;

        // 如果目标是笔记Fragment，手动触发刷新
        if (targetFragment instanceof NoteFragment) {
            ((NoteFragment) targetFragment).refreshNotes();
        }
    }

    private void setTitle(String title) {
        titleTextView.setText(title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 应用恢复前台时，如果当前显示的是笔记Fragment，刷新数据
        if (activeFragment instanceof NoteFragment) {
            ((NoteFragment) activeFragment).refreshNotes();
        }
    }
}