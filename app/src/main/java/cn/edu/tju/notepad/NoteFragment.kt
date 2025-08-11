package cn.edu.tju.notepad

import android.graphics.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.withSave

class NoteFragment : Fragment() {

    private lateinit var noteDbHelper: NoteDbHelper
    private lateinit var recyclerView: RecyclerView
    private var noteAdapter: NoteAdapter? = null
    private lateinit var editTextSearch: EditText
    private lateinit var clearSearchIcon: ImageView
    private var isViewCreated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化数据库帮助类
        noteDbHelper = NoteDbHelper(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 为这个fragment填充布局
        val rootView = inflater.inflate(R.layout.fragment_note, container, false)

        recyclerView = rootView.findViewById(R.id.recyclerView)
        editTextSearch = rootView.findViewById(R.id.editTextSearch)
        clearSearchIcon = rootView.findViewById(R.id.clearSearchIcon)

        val linearLayoutManager = LinearLayoutManager(activity).apply {
            orientation = RecyclerView.VERTICAL
        }
        recyclerView.layoutManager = linearLayoutManager

        // 设置滑动删除和拖拽排序功能
        val itemTouchHelper = ItemTouchHelper(NoteItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // 设置搜索功能
        setupSearchFunctionality()

        isViewCreated = true

        // 立即加载笔记数据
        loadNotes()

        return rootView
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // 视图状态恢复后刷新数据
        if (isViewCreated) {
            loadNotes()
        }
    }

    override fun onResume() {
        super.onResume()
        // 确保Fragment恢复时刷新数据
        if (isViewCreated) {
            loadNotes()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        // 当Fragment对用户可见时刷新数据
        if (isVisibleToUser && isViewCreated) {
            loadNotes()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        // 当Fragment从隐藏状态变为可见状态时刷新数据
        if (!hidden && isViewCreated) {
            loadNotes()
        }
    }

    // 添加一个公共方法，允许外部强制刷新
    fun refreshNotes() {
        if (isViewCreated) {
            loadNotes()
        }
    }

    private fun loadNotes() {
        try {
            activity?.let { context ->
                // 确保在UI线程中执行
                context.runOnUiThread {
                    // 从数据库获取笔记列表
                    val list = noteDbHelper.query()

                    if (noteAdapter == null) {
                        noteAdapter = NoteAdapter(list.toMutableList(), activity)
                        recyclerView.adapter = noteAdapter
                    } else {
                        // 刷新数据
                        noteAdapter?.refreshData(list)
                    }

                    // 如果有搜索词，保持搜索状态
                    if (editTextSearch.text.isNotEmpty()) {
                        noteAdapter?.filter(editTextSearch.text.toString())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 显示错误消息
            activity?.let { context ->
                Toast.makeText(context, "加载笔记失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearchFunctionality() {
        // 添加文本变化监听器
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 不需要处理
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文本变化时过滤结果
                noteAdapter?.filter(s.toString())

                // 控制清除按钮的可见性
                clearSearchIcon.visibility = if (s?.isNotEmpty() == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // 不需要处理
            }
        })

        // 设置键盘搜索键监听器
        editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // 执行搜索
                noteAdapter?.filter(editTextSearch.text.toString())
                true
            } else {
                false
            }
        }

        // 设置清除按钮点击监听器
        clearSearchIcon.setOnClickListener {
            editTextSearch.setText("")
            clearSearchIcon.visibility = View.GONE
            // 清除搜索后显示所有内容
            noteAdapter?.filter("")
        }
    }

    // ItemTouchHelper.Callback实现滑动删除和拖拽排序功能
    private inner class NoteItemTouchHelperCallback : ItemTouchHelper.Callback() {

        // 用于背景和边框绘制的画笔对象
        private val backgroundPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true  // 添加抗锯齿效果使边缘更平滑
        }

        private val borderPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true  // 添加抗锯齿效果使边缘更平滑
        }

        // 圆角半径
        private val cornerRadius = 20f

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            // 定义拖拽方向为上下，滑动方向为左右
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
            return makeMovementFlags(dragFlags, swipeFlags)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            source: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            // 处理拖拽排序
            val fromPosition = source.adapterPosition
            val toPosition = target.adapterPosition
            noteAdapter?.moveItem(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // 处理滑动删除
            val position = viewHolder.adapterPosition

            // 显示确认对话框，询问用户是否确定删除
            activity?.let { context ->
                AlertDialog.Builder(context).apply {
                    setTitle("是否要删除该信息？")
                    setPositiveButton("确定") { _, _ ->
                        // 用户确认删除
                        noteAdapter?.removeItem(position)
                    }
                    setNegativeButton("取消") { _, _ ->
                        // 用户取消删除，恢复显示
                        noteAdapter?.notifyItemChanged(position)
                    }
                    create().show()
                }
            }
        }

        override fun isLongPressDragEnabled(): Boolean {
            // 允许长按拖拽
            return true
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            // 允许滑动删除
            return true
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView

            // 保存当前画布状态
            c.withSave {
                when (actionState) {
                    ItemTouchHelper.ACTION_STATE_SWIPE -> {
                        // 滑动删除状态 - 显示红色背景
                        // 计算透明度 - 滑动距离越大，透明度越低
                        val alpha = 1.0f - kotlin.math.abs(dX) / itemView.width.toFloat()

                        // 设置透明度
                        itemView.alpha = alpha

                        // 创建圆角矩形区域
                        val rectF = if (dX > 0) {
                            // 向右滑动
                            RectF(
                                itemView.left.toFloat(),
                                itemView.top.toFloat(),
                                itemView.left + dX,
                                itemView.bottom.toFloat()
                            )
                        } else {
                            // 向左滑动
                            RectF(
                                itemView.right + dX,
                                itemView.top.toFloat(),
                                itemView.right.toFloat(),
                                itemView.bottom.toFloat()
                            )
                        }

                        // 绘制圆角红色背景
                        c.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)
                    }

                    ItemTouchHelper.ACTION_STATE_DRAG -> {
                        if (isCurrentlyActive) {
                            // 拖拽状态 - 显示蓝色边框
                            // 创建圆角矩形区域
                            val rectF = RectF(
                                itemView.left.toFloat(),
                                itemView.top.toFloat(),
                                itemView.right.toFloat(),
                                itemView.bottom.toFloat()
                            )

                            // 绘制蓝色圆角边框
                            borderPaint.color = Color.BLUE
                            c.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)
                        }
                    }
                }

                // 恢复画布状态
            }

            // 调用父类方法以保持默认行为
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            // 交互结束时恢复默认外观
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.alpha = 1.0f
        }
    }
}