package cn.edu.tju.notepad;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Date;

import cn.edu.jssvc.notepad.R;

public class NoteActivity extends AppCompatActivity {
    private EditText editTextTitle, editTextContent;
    private LinearLayout imageContainer;
    private Button btnAddImage;

    private NoteDbHelper noteDbHelper;
    private NoteBean noteBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        // 初始化视图
        initViews();

        // 设置数据
        setupData();

        // 设置监听器
        setupListeners();
    }

    private void initViews() {
        ImageView imageViewBack = findViewById(R.id.imageViewBack);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContent = findViewById(R.id.editTextContent);
        imageContainer = findViewById(R.id.imageContainer);
        btnAddImage = findViewById(R.id.btnAddImage);
    }

    private void setupData() {
        // 获取传递的数据
        Intent intent = getIntent();
        String comeFrom = intent.getStringExtra("ComeFrom");
        noteDbHelper = new NoteDbHelper(NoteActivity.this);

        // 如果是编辑现有笔记
        if (comeFrom != null && comeFrom.equals("NoteAdapter")) {
            noteBean = (NoteBean) intent.getSerializableExtra("NoteBean");
            if (noteBean != null) {
                editTextTitle.setText(noteBean.getTitle());
                editTextContent.setText(noteBean.getContent());

                // 加载图片
                loadImages();
            }
        }
    }

    private void setupListeners() {
        // 返回按钮
        findViewById(R.id.imageViewBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // 提交按钮
        Button buttonCommit = findViewById(R.id.buttonCommit);
        buttonCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNote();
            }
        });

        // 添加图片按钮
        btnAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageUtils.pickImageFromGallery(NoteActivity.this);
            }
        });
    }

    private void saveNote() {
        // 获取输入内容
        String title = editTextTitle.getText().toString().trim();
        String content = editTextContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(NoteActivity.this, "标题或内容为空，请补充后再发布", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = getIntent();
        String comeFrom = intent.getStringExtra("ComeFrom");

        if (comeFrom != null && comeFrom.equals("Add")) {
            // 新建笔记
            String time = String.valueOf(new Date());
            if (noteBean == null) {
                noteBean = new NoteBean(title, content, time);
            } else {
                noteBean.setTitle(title);
                noteBean.setContent(content);
                noteBean.setTime(time);
            }

            // 插入数据库
            long result = noteDbHelper.insert(noteBean);
            if (result > 0) {
                Toast.makeText(NoteActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(NoteActivity.this, "发布失败，请重新发布", Toast.LENGTH_SHORT).show();
            }
        } else if (comeFrom != null && comeFrom.equals("NoteAdapter")) {
            // 编辑现有笔记
            boolean noChanges = title.equals(noteBean.getTitle()) && content.equals(noteBean.getContent());

            if (noChanges) {
                Toast.makeText(NoteActivity.this, "没有修改", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 显示确认对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
            builder.setTitle("是否需要修改？");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    noteBean.setTitle(title);
                    noteBean.setContent(content);
                    noteBean.setTime(String.valueOf(new Date()));

                    // 更新数据库
                    long result = noteDbHelper.update(noteBean);
                    if (result > 0) {
                        Toast.makeText(NoteActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(NoteActivity.this, "修改失败，请重试！", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("取消", null);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    private void loadImages() {
        if (noteBean != null && noteBean.getImagePaths() != null) {
            for (String imagePath : noteBean.getImagePaths()) {
                if (!imagePath.isEmpty()) {
                    addImageToContainer(imagePath);
                }
            }
        }
    }

    private void addImageToContainer(String imagePath) {
        try {
            // 创建图片视图
            View imageView = LayoutInflater.from(this).inflate(R.layout.image_item, imageContainer, false);
            ImageView img = imageView.findViewById(R.id.imageViewItem);
            ImageView btnDelete = imageView.findViewById(R.id.btnDeleteImage);

            // 设置图片
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                img.setImageBitmap(bitmap);

                // 设置删除按钮操作
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 从容器中移除视图
                        imageContainer.removeView(imageView);

                        // 从笔记中移除图片路径
                        if (noteBean != null && noteBean.getImagePaths() != null) {
                            noteBean.getImagePaths().remove(imagePath);
                        }
                    }
                });

                // 将视图添加到容器
                imageContainer.addView(imageView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ImageUtils.REQUEST_IMAGE_PICK && data != null) {
                // 图库选择结果
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    String imagePath = ImageUtils.copyUriToPrivateStorage(this, selectedImage);

                    if (!imagePath.isEmpty()) {
                        // 添加图片到界面
                        addImageToContainer(imagePath);

                        // 添加图片路径到笔记
                        if (noteBean == null) {
                            noteBean = new NoteBean("", "", "");
                        }
                        noteBean.addImagePath(imagePath);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ImageUtils.REQUEST_STORAGE_PERMISSION) {
            // 检查是否所有请求的权限都被授予了
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // 所有权限都获取成功，可以选择图片
                ImageUtils.pickImageFromGallery(this);
            } else {
                // 用户拒绝了一些权限
                Toast.makeText(this, "需要存储权限才能上传图片", Toast.LENGTH_LONG).show();
            }
        }
    }

//    // 添加权限说明方法
//    private void showPermissionExplanation() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("需要存储权限");
//        builder.setMessage("应用需要存储权限才能选择和保存图片。请在设置中启用这些权限。");
//        builder.setPositiveButton("确定", (dialog, which) -> {
//            // 再次请求权限
//            ImageUtils.checkStoragePermission(NoteActivity.this);
//        });
//        builder.setNegativeButton("取消", null);
//        builder.show();
//    }
}

