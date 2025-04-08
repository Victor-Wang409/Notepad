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

import java.util.Collections;
import java.util.List;

import cn.edu.jssvc.notepad.R;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteItemViewHolder> {
    public List<NoteBean> list;
    private NoteDbHelper dbHelper;

    public NoteAdapter(List<NoteBean> list) {
        this.list = list;
    }

    // 添加带Context的构造函数
    public NoteAdapter(List<NoteBean> list, Context context) {
        this.list = list;
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
        NoteBean noteBean = list.get(position);
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

//        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
//                builder.setTitle("是否要删除该信息？");
//                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        removeItem(holder.getAdapterPosition());
//                    }
//                });
//                builder.setNegativeButton("取消", null);
//                AlertDialog alertDialog = builder.create();
//                alertDialog.show();
//                return true;
//            }
//        });
    }

    @Override
    public int getItemCount() {
        if (list != null){
            return list.size();
        }
        return 0;
    }

    // 添加拖拽排序方法
    public void moveItem(int fromPosition, int toPosition) {
        // 交换数据
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(list, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(list, i, i - 1);
            }
        }
        // 通知适配器数据变化
        notifyItemMoved(fromPosition, toPosition);
    }

    // 添加滑动删除方法
    // 缓存上下文用于Toast显示
    private Context mContext;

    // 添加滑动删除方法
    public void removeItem(int position) {
        if (position < 0 || position >= list.size()) {
            return;
        }

        NoteBean noteBean = list.get(position);
        if (dbHelper == null && mContext != null) {
            dbHelper = new NoteDbHelper(mContext);
        }

        if (dbHelper != null) {
            long result = dbHelper.delete(noteBean);

            if (result > 0) {
                list.remove(position);
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
}