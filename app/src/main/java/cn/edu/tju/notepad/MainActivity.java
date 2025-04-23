package cn.edu.tju.notepad;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import cn.edu.tju.notepad.R;

public class MainActivity extends AppCompatActivity {
    NoteDbHelper noteDbHelper;
    RecyclerView recyclerView;
    NoteAdapter noteAdapter;
    EditText editTextSearch;
    ImageView clearSearchIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        noteDbHelper = new NoteDbHelper(MainActivity.this);

        recyclerView = findViewById(R.id.recycleView);
        editTextSearch = findViewById(R.id.editTextSearch);
        clearSearchIcon = findViewById(R.id.clearSearchIcon);
        ImageView imageViewAdd = findViewById(R.id.imageViewAdd);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        // 设置滑动删除和拖拽排序功能
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new NoteItemTouchHelperCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);


        imageViewAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                intent.putExtra("ComeFrom", "Add");
                startActivity(intent);
            }
        });

        // 初始化搜索功能
        setupSearchFunctionality();
    }

    private void setupSearchFunctionality() {
        // 添加文本变化监听器
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不需要实现
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 当文本改变时过滤结果
                if (noteAdapter != null) {
                    noteAdapter.filter(s.toString());
                }

                // 控制清除按钮的显示
                if (s.length() > 0) {
                    clearSearchIcon.setVisibility(View.VISIBLE);
                } else {
                    clearSearchIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 不需要实现
            }
        });

        // 设置键盘搜索键监听器
        editTextSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // 执行搜索
                    if (noteAdapter != null) {
                        noteAdapter.filter(editTextSearch.getText().toString());
                    }
                    return true;
                }
                return false;
            }
        });

        // 设置清除按钮点击事件
        clearSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextSearch.setText("");
                clearSearchIcon.setVisibility(View.GONE);
                // 清空搜索后显示所有内容
                if (noteAdapter != null) {
                    noteAdapter.filter("");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        List<NoteBean> list = noteDbHelper.query();

        if (noteAdapter == null) {
            noteAdapter = new NoteAdapter(list, MainActivity.this);
            recyclerView.setAdapter(noteAdapter);
        } else {
            // 刷新数据
            noteAdapter.refreshData(list);
        }

        // 如果有搜索词，保持搜索状态
        if (editTextSearch != null && editTextSearch.getText().length() > 0) {
            noteAdapter.filter(editTextSearch.getText().toString());
        }
    }

    // 实现ItemTouchHelper.Callback来处理滑动删除和拖拽排序
    private class NoteItemTouchHelperCallback extends ItemTouchHelper.Callback {

        // 用于背景和边框绘制的Paint对象
        private Paint backgroundPaint;
        private Paint borderPaint;
        // 圆角半径
        private float cornerRadius = 20f;

        // 构造函数初始化Paint对象
        public NoteItemTouchHelperCallback() {
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.RED);
            backgroundPaint.setStyle(Paint.Style.FILL);
            backgroundPaint.setAntiAlias(true);  // 添加抗锯齿效果，使边缘更平滑

            borderPaint = new Paint();
            borderPaint.setColor(Color.RED);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(5);
            borderPaint.setAntiAlias(true);  // 添加抗锯齿效果，使边缘更平滑
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            // 定义拖拽方向为上下，滑动方向为左右
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source,
                              @NonNull RecyclerView.ViewHolder target) {
            // 处理拖拽排序
            int fromPosition = source.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            noteAdapter.moveItem(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // 处理滑动删除
            final int position = viewHolder.getAdapterPosition();

            // 显示确认对话框，询问用户是否确定要删除
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("是否要删除该信息？");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // 用户确认删除
                    noteAdapter.removeItem(position);
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // 用户取消删除，恢复显示
                    noteAdapter.notifyItemChanged(position);
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

        @Override
        public boolean isLongPressDragEnabled() {
            // 允许长按拖拽
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            // 允许滑动删除
            return true;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {

            View itemView = viewHolder.itemView;

            // 保存画布当前状态
            int saveCount = c.save();

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                // 滑动删除状态 - 显示红色背景
                // 计算透明度 - 滑动距离越大，透明度越低
                float alpha = 1.0f - Math.abs(dX) / (float) itemView.getWidth();
//                float alpha = 1.0f;
//
                // 设置透明度
                itemView.setAlpha(alpha);

                // 创建带圆角的矩形区域
                RectF rectF;
                if (dX > 0) {
                    // 右滑
                    rectF = new RectF(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + dX, itemView.getBottom());
                } else {
                    // 左滑
                    rectF = new RectF(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                }

                // 绘制圆角红色背景
                c.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint);

            } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                // 拖拽状态 - 显示蓝色边框
                // 创建带圆角的矩形区域
                RectF rectF = new RectF(itemView.getLeft(), itemView.getTop(),
                        itemView.getRight(), itemView.getBottom());

                // 绘制蓝色圆角
                borderPaint.setColor(Color.BLUE);
                c.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint);
            }

            // 恢复画布状态
            c.restoreToCount(saveCount);

            // 调用父类方法保持默认行为
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            // 当交互结束时恢复默认外观
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setAlpha(1.0f);
        }
    }
}