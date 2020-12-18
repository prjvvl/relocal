package com.pentagon.localhost_reborn.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pentagon.localhost_reborn.Object.Product;
import com.pentagon.localhost_reborn.Object.Service;
import com.pentagon.localhost_reborn.R;
import com.pentagon.localhost_reborn.ServiceActivity;

import java.util.List;
import java.util.Objects;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private static final String TAG = "ProductAdapter";
    private FirebaseUser currentUser;

    private Context mContext;
    private List<Product> mList;

    public ProductAdapter(Context mContext, List<Product> mList) {
        this.mContext = mContext;
        this.mList = mList;
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public ProductAdapter.ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductAdapter.ProductViewHolder holder, int position) {
        Product product = mList.get(position);
        final Service[] service = new Service[1];
        int likes = Integer.parseInt(product.getLikes());
        final int[] likeCount = {likes};
        final boolean[] hasLiked = {false};
        holder.mProductName.setText(product.getTitle());
        holder.mProductDetails.setText(product.getDescription());
        Glide.with(mContext)
                .load(product.getImgUri())
                .placeholder(R.drawable.img_placeholder)
                .into(holder.mProductImage);
        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference(mContext.getString(R.string.firebase_product)).child(product.getProductID());
        mDatabaseReference.child(mContext.getString(R.string.firebase_liked_by)).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && currentUser != null){
                    for (DataSnapshot userSnap : snapshot.getChildren()){
                        if (Objects.equals(userSnap.getKey(), currentUser.getUid())){
                            if (Objects.equals(userSnap.getValue(), "1")){
                                holder.mLike.setImageResource(R.drawable.ic_fav);
                                hasLiked[0] = true;
                            }
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: error: " + error.getMessage());
            }
        });
        holder.mLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentUser!=null){
                    if (hasLiked[0]){
                        holder.mLike.setImageResource(R.drawable.ic_favorite);
                        holder.mLikeCount.setText(String.valueOf(--likeCount[0]));
                        mDatabaseReference.child(mContext.getString(R.string.firebase_liked_by)).child(currentUser.getUid()).setValue("0")
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: like: " + e.getMessage());
                                    }
                                });
                        mDatabaseReference.child("likes").setValue(String.valueOf(likeCount[0]))
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: " + e.getMessage());
                                    }
                                });
                        hasLiked[0] = false;
                    }else {
                        holder.mLike.setImageResource(R.drawable.ic_fav);
                        holder.mLikeCount.setText(String.valueOf(++likeCount[0]));
                        mDatabaseReference.child(mContext.getString(R.string.firebase_liked_by)).child(currentUser.getUid()).setValue("1")
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: like: " + e.getMessage());
                                    }
                                });
                        mDatabaseReference.child("likes").setValue(String.valueOf(likeCount[0]))
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: " + e.getMessage());
                                    }
                                });
                    }
                }else {
                    Toast.makeText(mContext, "Sign in first!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        holder.mLikeCount.setText(product.getLikes());
        DatabaseReference mServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(mContext.getString(R.string.firebase_service));
        mServiceDatabaseReference.child(product.getServiceID())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            try {
                                service[0] = snapshot.getValue(Service.class);
                                if (service[0] !=null){
                                    holder.mServiceName.setText(service[0].getTitle());
                                    holder.mOwnerName.setText(service[0].getCategory());
                                }
                            }catch (Exception e){
                                Log.d(TAG, "onDataChange: Excpe: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: errror: " + error.getMessage());
                    }
                });
        holder.mCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (service[0]!=null){
                    mContext.startActivity(new Intent(mContext, ServiceActivity.class)
                            .putExtra(mContext.getString(R.string.service_id), service[0].getServiceID())
                            .putExtra(mContext.getString(R.string.user_id), service[0].getUserID())
                    );
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        private TextView mProductName, mProductDetails, mLikeCount, mServiceName, mOwnerName;
        private ImageView mServiceImage, mProductImage, mLike;
        private CardView mCard;
        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            mProductName = itemView.findViewById(R.id.lp_product_name);
            mProductDetails = itemView.findViewById(R.id.lp_product_details);
            mServiceImage = itemView.findViewById(R.id.lp_service_img);
            mProductImage = itemView.findViewById(R.id.lp_product_img);
            mServiceName = itemView.findViewById(R.id.lp_service_name);
            mOwnerName = itemView.findViewById(R.id.lp_owner);
            mLike = itemView.findViewById(R.id.lp_like);
            mLikeCount = itemView.findViewById(R.id.lp_like_count);
            mCard = itemView.findViewById(R.id.lp_card);
        }
    }
}
