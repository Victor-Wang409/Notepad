package cn.edu.jssvc.notepad;

import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter {
    public List<NoteBean> list;
    private NoteDbHelper dbHelper;

    public NoteAdapter(List<NoteBean> list) {
        this.list = list;
    }

    public class NoteItemViewHolder extends RecyclerView.ViewHolder{
        public TextView textViewTitle;
        public TextView textViewTime;
        public NoteItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item,parent,false);

        return new NoteItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NoteBean noteBean = list.get(position);
        NoteItemViewHolder noteItemViewHolder = (NoteItemViewHolder) holder;
        noteItemViewHolder.textViewTitle.setText(noteBean.getTitle());
        noteItemViewHolder.textViewTime.setText(noteBean.getTime());
        
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(holder.itemView.getContext(),NoteActivity.class);
                intent.putExtra("ComeFrom","NoteAdapter");
                intent.putExtra("NoteBean",noteBean);
                holder.itemView.getContext().startActivity(intent);
//                Toast.makeText(holder.itemView.getContext(), "点击"+holder.getAdapterPosition(), Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                builder.setTitle("是否要删除该信息？");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dbHelper = new NoteDbHelper(holder.itemView.getContext());
                        long lows = dbHelper.delete(noteBean);
                        if (lows>0){
                            Toast.makeText(holder.itemView.getContext(), "删除成功！", Toast.LENGTH_SHORT).show();
                            list.remove(holder.getAdapterPosition());
                            NoteAdapter.this.notifyItemRemoved(holder.getAdapterPosition());
                        }
                        else {
                            Toast.makeText(holder.itemView.getContext(), "删除失败，请重试！", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("取消",null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
//                Toast.makeText(holder.itemView.getContext(), "长按"+holder.getAdapterPosition(), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        if (list!=null){
            return list.size();
        }
        return 0;
    }
}
