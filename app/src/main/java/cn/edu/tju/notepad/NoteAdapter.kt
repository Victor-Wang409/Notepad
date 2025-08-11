package cn.edu.tju.notepad

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class NoteAdapter(
    private val list: MutableList<NoteBean>,
    private val mContext: Context? = null
) : RecyclerView.Adapter<NoteAdapter.NoteItemViewHolder>() {

    private val filteredList: MutableList<NoteBean> = ArrayList(list)
    private val dbHelper: NoteDbHelper? = mContext?.let { NoteDbHelper(it) }

    inner class NoteItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteItemViewHolder, position: Int) {
        val noteBean = filteredList[position]
        holder.textViewTitle.text = noteBean.title
        holder.textViewTime.text = noteBean.time

        holder.itemView.setOnClickListener {
            Toast.makeText(
                holder.itemView.context,
                "点击${holder.adapterPosition + 1}",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(holder.itemView.context, NoteActivity::class.java).apply {
                putExtra("ComeFrom", "NoteAdapter")
                putExtra("NoteBean", noteBean)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    // 添加拖拽排序方法
    fun moveItem(fromPosition: Int, toPosition: Int) {
        // 只对筛选后的列表进行操作
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(filteredList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(filteredList, i, i - 1)
            }
        }
        // 通知适配器数据变化
        notifyItemMoved(fromPosition, toPosition)
    }

    // 添加滑动删除方法
    fun removeItem(position: Int) {
        if (position < 0 || position >= filteredList.size) {
            return
        }

        val noteBean = filteredList[position]
        val result = dbHelper?.delete(noteBean) ?: 0

        if (result > 0) {
            // 从主列表中移除
            list.remove(noteBean)
            // 从筛选列表中移除
            filteredList.removeAt(position)

            notifyItemRemoved(position)
            mContext?.let {
                Toast.makeText(it, "删除成功！", Toast.LENGTH_SHORT).show()
            }
        } else {
            mContext?.let {
                Toast.makeText(it, "删除失败，请重试！", Toast.LENGTH_SHORT).show()
            }
            notifyItemChanged(position)
        }
    }

    // 添加搜索过滤功能
    fun filter(query: String) {
        filteredList.clear()

        if (query.isEmpty()) {
            // 如果搜索框为空，显示所有笔记
            filteredList.addAll(list)
        } else {
            // 否则根据标题和内容进行过滤
            val lowerCaseQuery = query.lowercase(Locale.getDefault())

            for (note in list) {
                if (note.title.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    note.content.lowercase(Locale.getDefault()).contains(lowerCaseQuery)
                ) {
                    filteredList.add(note)
                }
            }
        }

        notifyDataSetChanged()
    }

    // 重新加载数据
    fun refreshData(newList: List<NoteBean>) {
        list.clear()
        list.addAll(newList)
        filteredList.clear()
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }
}