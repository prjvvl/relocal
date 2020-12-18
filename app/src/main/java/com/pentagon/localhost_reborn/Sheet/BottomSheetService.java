package com.pentagon.localhost_reborn.Sheet;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.pentagon.localhost_reborn.Object.Service;
import com.pentagon.localhost_reborn.R;
import com.pentagon.localhost_reborn.ServiceActivity;

public class BottomSheetService extends BottomSheetDialogFragment {
    private static final String TAG = "BottomSheetService";
    private Service service;

    public BottomSheetService(Service service) {
        this.service = service;
    }

    private CardView mCard;
    private ImageView mImage;
    private TextView mTitle, mCategory, mLocation, mRatings;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_service, container, false);
        mCard = view.findViewById(R.id.bss_card);
        mImage = view.findViewById(R.id.bss_image);
        mTitle = view.findViewById(R.id.bss_title);
        mCategory = view.findViewById(R.id.bss_category);
        mLocation = view.findViewById(R.id.bss_location);
        mRatings = view.findViewById(R.id.bss_ratings);
        mTitle.setText(service.getTitle());
        mCategory.setText(service.getCategory());
        mLocation.setText(service.getCity()+", "+service.getState());
        mRatings.setText(service.getRatings());
        Glide.with(getContext())
                .load(service.getImgUri())
                .placeholder(R.drawable.img_placeholder)
                .into(mImage);
        mCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().startActivity(new Intent(getContext(), ServiceActivity.class)
                        .putExtra(getContext().getString(R.string.service_id), service.getServiceID())
                        .putExtra(getContext().getString(R.string.user_id), service.getUserID())
                );
            }
        });
        return view;
    }
}
