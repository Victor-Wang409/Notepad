package cn.edu.tju.notepad;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteFragment extends Fragment {

    private NoteDbHelper noteDbHelper;
    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private EditText editTextSearch;
    private ImageView clearSearchIcon;
    private boolean isViewCreated = false;

    public NoteFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化数据库帮助类
        noteDbHelper = new NoteDbHelper(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_note, container, false);

        // Initialize views
        recyclerView = rootView.findViewById(R.id.recyclerView);
        editTextSearch = rootView.findViewById(R.id.editTextSearch);
        clearSearchIcon = rootView.findViewById(R.id.clearSearchIcon);

        // Set up RecyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        // Set up swipe-to-delete and drag-and-drop functionality
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new NoteItemTouchHelperCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Set up search functionality
        setupSearchFunctionality();

        isViewCreated = true;

        // 立即加载笔记数据
        loadNotes();

        return rootView;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // 视图状态恢复后刷新数据
        if (isViewCreated) {
            loadNotes();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 确保Fragment恢复时刷新数据
        if (isViewCreated) {
            loadNotes();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // 当Fragment对用户可见时刷新数据
        if (isVisibleToUser && isViewCreated) {
            loadNotes();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // 当Fragment从隐藏状态变为可见状态时刷新数据
        if (!hidden && isViewCreated) {
            loadNotes();
        }
    }

    // 添加一个公共方法，允许外部强制刷新
    public void refreshNotes() {
        if (isViewCreated) {
            loadNotes();
        }
    }

    private void loadNotes() {
        try {
            if (getActivity() == null) return;

            // 确保在UI线程执行
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 检查数据库帮助类是否初始化
                    if (noteDbHelper == null) {
                        noteDbHelper = new NoteDbHelper(getActivity());
                    }

                    // 从数据库获取笔记列表
                    List<NoteBean> list = noteDbHelper.query();

                    if (noteAdapter == null) {
                        noteAdapter = new NoteAdapter(list, getActivity());
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
            });
        } catch (Exception e) {
            e.printStackTrace();
            // 可以添加Toast显示错误消息
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "加载笔记失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupSearchFunctionality() {
        // Add text change listener
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter results when text changes
                if (noteAdapter != null) {
                    noteAdapter.filter(s.toString());
                }

                // Control clear button visibility
                if (s.length() > 0) {
                    clearSearchIcon.setVisibility(View.VISIBLE);
                } else {
                    clearSearchIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Set keyboard search key listener
        editTextSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Execute search
                    if (noteAdapter != null) {
                        noteAdapter.filter(editTextSearch.getText().toString());
                    }
                    return true;
                }
                return false;
            }
        });

        // Set clear button click listener
        clearSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextSearch.setText("");
                clearSearchIcon.setVisibility(View.GONE);
                // Show all content after clearing search
                if (noteAdapter != null) {
                    noteAdapter.filter("");
                }
            }
        });
    }

    // ItemTouchHelper.Callback implementation for swipe-to-delete and drag-and-drop
    private class NoteItemTouchHelperCallback extends ItemTouchHelper.Callback {

        // Paint objects for background and border drawing
        private Paint backgroundPaint;
        private Paint borderPaint;
        // Corner radius
        private float cornerRadius = 20f;

        // Constructor initializes Paint objects
        public NoteItemTouchHelperCallback() {
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.RED);
            backgroundPaint.setStyle(Paint.Style.FILL);
            backgroundPaint.setAntiAlias(true);  // Add anti-aliasing effect for smoother edges

            borderPaint = new Paint();
            borderPaint.setColor(Color.RED);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(5);
            borderPaint.setAntiAlias(true);  // Add anti-aliasing effect for smoother edges
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            // Define drag direction as up and down, swipe direction as left and right
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source,
                              @NonNull RecyclerView.ViewHolder target) {
            // Handle drag-and-drop sorting
            int fromPosition = source.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            noteAdapter.moveItem(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // Handle swipe-to-delete
            final int position = viewHolder.getAdapterPosition();

            // Show confirmation dialog asking the user if they're sure about deleting
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("是否要删除该信息？");
            builder.setPositiveButton("确定", (dialogInterface, i) -> {
                // User confirms deletion
                noteAdapter.removeItem(position);
            });
            builder.setNegativeButton("取消", (dialogInterface, i) -> {
                // User cancels deletion, restore display
                noteAdapter.notifyItemChanged(position);
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

        @Override
        public boolean isLongPressDragEnabled() {
            // Allow long press drag
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            // Allow swipe-to-delete
            return true;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {

            View itemView = viewHolder.itemView;

            // Save current canvas state
            int saveCount = c.save();

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                // Swipe-to-delete state - show red background
                // Calculate transparency - the greater the swipe distance, the lower the transparency
                float alpha = 1.0f - Math.abs(dX) / (float) itemView.getWidth();

                // Set transparency
                itemView.setAlpha(alpha);

                // Create rounded rectangle area
                RectF rectF;
                if (dX > 0) {
                    // Right swipe
                    rectF = new RectF(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + dX, itemView.getBottom());
                } else {
                    // Left swipe
                    rectF = new RectF(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                }

                // Draw rounded red background
                c.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint);

            } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                // Drag state - show blue border
                // Create rounded rectangle area
                RectF rectF = new RectF(itemView.getLeft(), itemView.getTop(),
                        itemView.getRight(), itemView.getBottom());

                // Draw blue rounded corners
                borderPaint.setColor(Color.BLUE);
                c.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint);
            }

            // Restore canvas state
            c.restoreToCount(saveCount);

            // Call parent method to maintain default behavior
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            // Restore default appearance when interaction ends
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setAlpha(1.0f);
        }
    }
}