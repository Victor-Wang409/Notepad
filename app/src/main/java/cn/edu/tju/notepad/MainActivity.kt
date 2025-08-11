package cn.edu.tju.notepad

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabAddNote: FloatingActionButton

    private lateinit var noteFragment: NoteFragment
    private lateinit var userFragment: UserFragment
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化Views
        titleTextView = findViewById(R.id.textView)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        fabAddNote = findViewById(R.id.fab_add_note)

        // 初始化Fragments
        if (savedInstanceState == null) {
            // 只在首次创建时初始化Fragment，避免重复创建
            noteFragment = NoteFragment()
            userFragment = UserFragment()

            // 初始化时添加所有Fragment并隐藏非默认的Fragment
            val fragmentManager: FragmentManager = supportFragmentManager
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()
            transaction.add(R.id.fragment_container, userFragment).hide(userFragment)
            transaction.add(R.id.fragment_container, noteFragment)
            transaction.commit()

            activeFragment = noteFragment
        }

        setupBottomNavigation()

        setupFAB()

        // 设置默认标题
        setTitle("笔记")
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.navigation_notes -> {
                    switchFragment(noteFragment)
                    setTitle("笔记")
                    fabAddNote.visibility = View.VISIBLE
                    true
                }
                R.id.navigation_user -> {
                    switchFragment(userFragment)
                    setTitle("用户")
                    fabAddNote.visibility = View.GONE
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFAB() {
        fabAddNote.setOnClickListener {
            val intent = Intent(this@MainActivity, NoteActivity::class.java)
            intent.putExtra("ComeFrom", "Add")
            startActivity(intent)
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment == targetFragment) return

        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.hide(activeFragment).show(targetFragment).commit()
        activeFragment = targetFragment

        // 如果目标是笔记Fragment，手动触发刷新
        if (targetFragment is NoteFragment) {
            targetFragment.refreshNotes()
        }
    }

    private fun setTitle(title: String) {
        titleTextView.text = title
    }

    override fun onResume() {
        super.onResume()

        // 应用恢复前台时，如果当前显示的是笔记Fragment，刷新数据
        if (activeFragment is NoteFragment) {
            (activeFragment as NoteFragment).refreshNotes()
        }
    }
}