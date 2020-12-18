package com.pentagon.localhost_reborn.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.pentagon.localhost_reborn.MessageActivity;
import com.pentagon.localhost_reborn.Object.User;
import com.pentagon.localhost_reborn.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private static final String TAG = "UserAdapter";

    private final Context mContext;
    private final List<User> mList;

    public UserAdapter(Context mContext, List<User> mList) {
        this.mContext = mContext;
        this.mList = mList;
    }

    @NonNull
    @Override
    public UserAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.UserViewHolder holder, int position) {
        User user = mList.get(position);
        holder.mName.setText(user.getUserName());
        holder.mEmail.setText(user.getEmail());
        Glide.with(mContext)
                .load(user.getImgUrl())
                .placeholder(R.drawable.ic_account)
                .into(holder.mImage);
        holder.mCard.setOnClickListener(view -> {
            mContext.startActivity(new Intent(mContext, MessageActivity.class).putExtra(mContext.getString(R.string.receiver_id), user.getUserID()));
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final CardView mCard;
        private final ImageView mImage;
        private final TextView mName;
        private final TextView mEmail;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            mCard = itemView.findViewById(R.id.lu_card);
            mImage = itemView.findViewById(R.id.lu_img);
            mName = itemView.findViewById(R.id.lu_name);
            mEmail = itemView.findViewById(R.id.lu_email);
        }
    }
}
