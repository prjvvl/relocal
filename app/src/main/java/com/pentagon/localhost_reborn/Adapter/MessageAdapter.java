package com.pentagon.localhost_reborn.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.pentagon.localhost_reborn.Object.Message;
import com.pentagon.localhost_reborn.R;

import java.util.List;
import java.util.Objects;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final String TAG = "MessageAdapter";
    private final String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

    private final Context mContext;
    private final List<Message> mList;
    private static final int TYPE_SEND = 1;
    private static final int TYPE_RECEIVED = 2;

    public MessageAdapter(Context mContext, List<Message> mList) {
        this.mContext = mContext;
        this.mList = mList;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = mList.get(position);
        if (!message.getFrom().equals(uid))   return TYPE_RECEIVED;
        return TYPE_SEND;
    }


    @NonNull
    @Override
    public MessageAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_SEND){
            view = LayoutInflater.from(mContext).inflate(R.layout.layout_send, parent, false);
        }else {
            view = LayoutInflater.from(mContext).inflate(R.layout.layout_receive, parent, false);
        }
//        view = LayoutInflater.from(mContext).inflate(R.layout.layout_receive, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.MessageViewHolder holder, int position) {
        Message message = mList.get(position);
        holder.mMessage.setText(message.getMessage());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView mMessage;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            mMessage = itemView.findViewById(R.id.lm_message);
        }
    }

}
