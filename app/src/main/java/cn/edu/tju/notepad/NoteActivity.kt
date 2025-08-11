package cn.edu.tju.notepad

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.*

class NoteActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextContent: EditText
    private lateinit var imageContainer: LinearLayout
    private lateinit var btnAddImage: Button

    private lateinit var noteDbHelper: NoteDbHelper
    private var noteBean: NoteBean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        // 初始化视图
        initViews()

        // 设置数据
        setupData()

        // 设置监听器
        setupListeners()
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.imageViewBack)
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        imageContainer = findViewById(R.id.imageContainer)
        btnAddImage = findViewById(R.id.btnAddImage)
    }

    private fun setupData() {
        // 获取传递的数据
        val intent = intent
        val comeFrom = intent.getStringExtra("ComeFrom")
        noteDbHelper = NoteDbHelper(this@NoteActivity)

        // 如果编辑现有笔记
        if (comeFrom == "NoteAdapter") {
            noteBean = intent.getSerializableExtra("NoteBean", NoteBean::class.java)
            noteBean?.let { note ->
                editTextTitle.setText(note.title)
                editTextContent.setText(note.content)

                // 加载图片
                loadImages()
            }
        }
    }

    private fun setupListeners() {
        // 返回按钮
        findViewById<ImageView>(R.id.imageViewBack).setOnClickListener {
            finish()
        }

        // 提交按钮
        val buttonCommit = findViewById<Button>(R.id.buttonCommit)
        buttonCommit.setOnClickListener {
            saveNote()
        }

        // 添加图片按钮
        btnAddImage.setOnClickListener {
            ImageUtils.pickImageFromGallery(this@NoteActivity)
        }
    }

    private fun saveNote() {
        // 获取输入内容
        val title = editTextTitle.text.toString().trim()
        val content = editTextContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this@NoteActivity, "标题或内容为空，请补充后再发布", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = intent
        val comeFrom = intent.getStringExtra("ComeFrom")

        when (comeFrom) {
            "Add" -> {
                // 创建新笔记
                val time = Date().toString()
                if (noteBean == null) {
                    noteBean = NoteBean(title, content, time)
                } else {
                    noteBean?.apply {
                        this.title = title
                        this.content = content
                        this.time = time
                    }
                }

                // 插入到数据库
                val result = noteDbHelper.insert(noteBean!!)
                if (result > 0) {
                    Toast.makeText(this@NoteActivity, "发布成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NoteActivity, "发布失败，请重新发布", Toast.LENGTH_SHORT).show()
                }
            }
            "NoteAdapter" -> {
                // 编辑现有笔记
                val currentNote = noteBean!!
                val noChanges = title == currentNote.title && content == currentNote.content

                if (noChanges) {
                    Toast.makeText(this@NoteActivity, "没有修改", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                // 显示确认对话框
                AlertDialog.Builder(this@NoteActivity).apply {
                    setTitle("是否需要修改？")
                    setPositiveButton("确定") { _, _ ->
                        currentNote.apply {
                            this.title = title
                            this.content = content
                            this.time = Date().toString()
                        }

                        // 更新数据库
                        val result = noteDbHelper.update(currentNote)
                        if (result > 0) {
                            Toast.makeText(this@NoteActivity, "修改成功", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@NoteActivity, "修改失败，请重试！", Toast.LENGTH_SHORT).show()
                        }
                    }
                    setNegativeButton("取消", null)
                    create().show()
                }
            }
        }
    }

    private fun loadImages() {
        noteBean?.imagePaths?.forEach { imagePath ->
            if (imagePath.isNotEmpty()) {
                addImageToContainer(imagePath)
            }
        }
    }

    private fun addImageToContainer(imagePath: String) {
        try {
            // 创建图片视图
            val imageView = LayoutInflater.from(this).inflate(R.layout.image_item, imageContainer, false)
            val img = imageView.findViewById<ImageView>(R.id.imageViewItem)
            val btnDelete = imageView.findViewById<ImageView>(R.id.btnDeleteImage)

            // 设置图片
            val imgFile = File(imagePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                img.setImageBitmap(bitmap)

                // 设置删除按钮动作
                btnDelete.setOnClickListener {
                    // 从容器中移除视图
                    imageContainer.removeView(imageView)

                    // 从笔记中移除图片路径
                    noteBean?.imagePaths?.remove(imagePath)
                }

                // 添加视图到容器
                imageContainer.addView(imageView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == ImageUtils.REQUEST_IMAGE_PICK && data != null) {
                // 图库选择结果
                val selectedImage: Uri? = data.data
                selectedImage?.let { uri ->
                    val imagePath = ImageUtils.copyUriToPrivateStorage(this, uri)

                    if (imagePath.isNotEmpty()) {
                        // 添加图片到UI
                        addImageToContainer(imagePath)

                        // 添加图片路径到笔记
                        if (noteBean == null) {
                            noteBean = NoteBean("", "", "")
                        }
                        noteBean?.addImagePath(imagePath)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == ImageUtils.REQUEST_STORAGE_PERMISSION) {
            // 检查是否所有请求的权限都被授予
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                // 所有权限都被授予，可以选择图片
                ImageUtils.pickImageFromGallery(this)
            } else {
                // 用户拒绝了某些权限
                Toast.makeText(this, "需要存储权限才能上传图片", Toast.LENGTH_LONG).show()
            }
        }
    }
}