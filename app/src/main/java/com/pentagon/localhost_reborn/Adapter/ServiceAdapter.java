package com.pentagon.localhost_reborn.Adapter;

import android.annotation.SuppressLint;
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
import com.pentagon.localhost_reborn.Object.Service;
import com.pentagon.localhost_reborn.R;
import com.pentagon.localhost_reborn.ServiceActivity;
import com.pentagon.localhost_reborn.ServiceAddActivity;

import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private Context mContext;
    private List<Service> mList;

    public ServiceAdapter(Context mContext, List<Service> mList) {
        this.mContext = mContext;
        this.mList = mList;
    }

    @NonNull
    @Override
    public ServiceAdapter.ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ServiceAdapter.ServiceViewHolder holder, int position) {
        Service service = mList.get(position);
        holder.mTitle.setText(service.getTitle());
        holder.mCategory.setText(service.getCategory());
        holder.mLocation.setText(service.getCity() + ", " + service.getState());
        double rating = Double.parseDouble(service.getRatings());
        rating = Math.round(rating * 10.0)/10.0;
        holder.mRatings.setText(String.valueOf(rating));
        Glide.with(mContext).load(service.getImgUri()).placeholder(R.drawable.img_placeholder).into(holder.mImage);
        holder.mCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContext.startActivity(new Intent(mContext, ServiceActivity.class)
                        .putExtra(mContext.getString(R.string.service_id), service.getServiceID())
                        .putExtra(mContext.getString(R.string.user_id), service.getUserID())
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class ServiceViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImage;
        private TextView mTitle, mCategory, mLocation, mRatings;
        private CardView mCard;
        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            mImage = itemView.findViewById(R.id.ls_image);
            mTitle = itemView.findViewById(R.id.ls_title);
            mCategory = itemView.findViewById(R.id.ls_category);
            mLocation = itemView.findViewById(R.id.ls_location);
            mRatings = itemView.findViewById(R.id.ls_ratings);
            mCard = itemView.findViewById(R.id.ls_card);
        }
    }
}
