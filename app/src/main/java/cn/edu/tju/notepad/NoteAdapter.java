package cn.edu.tju.notepad;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.edu.jssvc.notepad.R;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteItemViewHolder> {
    private List<NoteBean> list;               // 完整的笔记列表
    private List<NoteBean> filteredList;       // 筛选后的笔记列表
    private NoteDbHelper dbHelper;
    private Context mContext;

    public NoteAdapter(List<NoteBean> list) {
        this.list = list;
        this.filteredList = new ArrayList<>(list);
    }

    // 添加带Context的构造函数
    public NoteAdapter(List<NoteBean> list, Context context) {
        this.list = list;
        this.filteredList = new ArrayList<>(list);
        this.mContext = context;
        this.dbHelper = new NoteDbHelper(context);
    }

    public class NoteItemViewHolder extends RecyclerView.ViewHolder{
        public TextView textViewTitle;
        public TextView textViewTime;
        public CardView cardView;

        public NoteItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }

    @NonNull
    @Override
    public NoteItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item,parent,false);
        return new NoteItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteItemViewHolder holder, int position) {
        NoteBean noteBean = filteredList.get(position);
        holder.textViewTitle.setText(noteBean.getTitle());
        holder.textViewTime.setText(noteBean.getTime());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(holder.itemView.getContext(), "点击"+(holder.getAdapterPosition()+1), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(holder.itemView.getContext(), NoteActivity.class);
                intent.putExtra("ComeFrom", "NoteAdapter");
                intent.putExtra("NoteBean", noteBean);
                holder.itemView.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (filteredList != null){
            return filteredList.size();
        }
        return 0;
    }

    // 添加拖拽排序方法
    public void moveItem(int fromPosition, int toPosition) {
        // 只对筛选后的列表进行操作
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(filteredList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(filteredList, i, i - 1);
            }
        }

        // 同时更新完整列表中的顺序，以保持一致性
        // 在实际应用中，可能需要更复杂的逻辑来处理筛选状态下的排序

        // 通知适配器数据变化
        notifyItemMoved(fromPosition, toPosition);
    }

    // 添加滑动删除方法
    public void removeItem(int position) {
        if (position < 0 || position >= filteredList.size()) {
            return;
        }

        NoteBean noteBean = filteredList.get(position);
        if (dbHelper == null && mContext != null) {
            dbHelper = new NoteDbHelper(mContext);
        }

        if (dbHelper != null) {
            long result = dbHelper.delete(noteBean);

            if (result > 0) {
                // 从主列表中移除
                list.remove(noteBean);
                // 从筛选列表中移除
                filteredList.remove(position);

                notifyItemRemoved(position);
                if (mContext != null) {
                    Toast.makeText(mContext, "删除成功！", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (mContext != null) {
                    Toast.makeText(mContext, "删除失败，请重试！", Toast.LENGTH_SHORT).show();
                }
                notifyItemChanged(position);
            }
        }
    }

    // 添加搜索过滤功能
    public void filter(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            // 如果搜索框为空，显示所有笔记
            filteredList.addAll(list);
        } else {
            // 否则根据标题和内容进行过滤
            String lowerCaseQuery = query.toLowerCase();

            for (NoteBean note : list) {
                if (note.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        note.getContent().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(note);
                }
            }
        }

        notifyDataSetChanged();
    }

    // 重新加载数据
    public void refreshData(List<NoteBean> newList) {
        this.list = newList;
        this.filteredList.clear();
        this.filteredList.addAll(newList);
        notifyDataSetChanged();
    }
}