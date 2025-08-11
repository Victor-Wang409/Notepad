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

        // Initialize views
        initViews();

        // Set up data
        setupData();

        // Set up listeners
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
        // Get passed data
        Intent intent = getIntent();
        String comeFrom = intent.getStringExtra("ComeFrom");
        noteDbHelper = new NoteDbHelper(NoteActivity.this);

        // If editing an existing note
        if (comeFrom != null && comeFrom.equals("NoteAdapter")) {
            noteBean = (NoteBean) intent.getSerializableExtra("NoteBean");
            if (noteBean != null) {
                editTextTitle.setText(noteBean.getTitle());
                editTextContent.setText(noteBean.getContent());

                // Load images
                loadImages();
            }
        }
    }

    private void setupListeners() {
        // Back button
        findViewById(R.id.imageViewBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Submit button
        Button buttonCommit = findViewById(R.id.buttonCommit);
        buttonCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNote();
            }
        });

        // Add image button
        btnAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageUtils.pickImageFromGallery(NoteActivity.this);
            }
        });
    }

    private void saveNote() {
        // Get input content
        String title = editTextTitle.getText().toString().trim();
        String content = editTextContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(NoteActivity.this, "标题或内容为空，请补充后再发布", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = getIntent();
        String comeFrom = intent.getStringExtra("ComeFrom");

        if (comeFrom != null && comeFrom.equals("Add")) {
            // Create new note
            String time = String.valueOf(new Date());
            if (noteBean == null) {
                noteBean = new NoteBean(title, content, time);
            } else {
                noteBean.setTitle(title);
                noteBean.setContent(content);
                noteBean.setTime(time);
            }

            // Insert into database
            long result = noteDbHelper.insert(noteBean);
            if (result > 0) {
                Toast.makeText(NoteActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(NoteActivity.this, "发布失败，请重新发布", Toast.LENGTH_SHORT).show();
            }
        } else if (comeFrom != null && comeFrom.equals("NoteAdapter")) {
            // Edit existing note
            boolean noChanges = title.equals(noteBean.getTitle()) && content.equals(noteBean.getContent());

            if (noChanges) {
                Toast.makeText(NoteActivity.this, "没有修改", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Show confirmation dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
            builder.setTitle("是否需要修改？");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    noteBean.setTitle(title);
                    noteBean.setContent(content);
                    noteBean.setTime(String.valueOf(new Date()));

                    // Update database
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
            // Create image view
            View imageView = LayoutInflater.from(this).inflate(R.layout.image_item, imageContainer, false);
            ImageView img = imageView.findViewById(R.id.imageViewItem);
            ImageView btnDelete = imageView.findViewById(R.id.btnDeleteImage);

            // Set image
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                img.setImageBitmap(bitmap);

                // Set delete button action
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Remove view from container
                        imageContainer.removeView(imageView);

                        // Remove image path from note
                        if (noteBean != null && noteBean.getImagePaths() != null) {
                            noteBean.getImagePaths().remove(imagePath);
                        }
                    }
                });

                // Add view to container
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
                // Gallery selection result
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    String imagePath = ImageUtils.copyUriToPrivateStorage(this, selectedImage);

                    if (!imagePath.isEmpty()) {
                        // Add image to UI
                        addImageToContainer(imagePath);

                        // Add image path to note
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
            // Check if all requested permissions were granted
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // All permissions granted, can pick image
                ImageUtils.pickImageFromGallery(this);
            } else {
                // User denied some permissions
                Toast.makeText(this, "需要存储权限才能上传图片", Toast.LENGTH_LONG).show();
            }
        }
    }
}