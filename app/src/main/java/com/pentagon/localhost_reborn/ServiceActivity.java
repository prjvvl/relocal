package com.pentagon.localhost_reborn;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pentagon.localhost_reborn.Adapter.ProductAdapter;
import com.pentagon.localhost_reborn.Object.Product;
import com.pentagon.localhost_reborn.Object.Service;
import com.pentagon.localhost_reborn.Object.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServiceActivity extends AppCompatActivity implements OnMapReadyCallback {

    private BottomSheetBehavior mBottomSheetBehavior;
    private static final String TAG = "ServiceActivity";


    private float mDefaultZoom = 12.0f;
    private static final int MY_LOCATION_REQUEST_CODE = 44;
    private LatLng mCurrentLocation;
    private GoogleMap mMap;
    private Address mServiceAddress;
    private boolean cameraFlag = false;

    private TextView mTitle, mRatings, mSPname, mSPemail, mCategory, mLocation, mDetails, mAddress, mShowMore;
    private ImageView mServiceImage, mSPimage, mEmpty;
    private Button mAddProduct;
    private LinearLayout mProductLayout;
    private RecyclerView mRecycler;
    private CardView serviceProvideCard;
    private String serviceID, serviceProvideID;

    private ImageView star_1, star_2, star_3, star_4, star_5;

    private boolean hasRated = false;
    private int ratingsGiveByUser = 0;

    private FirebaseUser currentUser;
    private Service mService;
    private List<String> mProductIDList;
    private List<Product> mProductList;
    private ProductAdapter mProductAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        View bottomSheet = findViewById(R.id.as_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.as_map_fragment);
        mapFragment.getMapAsync(this);
        serviceID = getIntent().getStringExtra(getString(R.string.service_id));
        serviceProvideID = getIntent().getStringExtra(getString(R.string.user_id));
        init();
        loadService();
    }

    private void init() {
        mTitle = findViewById(R.id.as_title);
        mRatings = findViewById(R.id.as_ratings);
        mSPname = findViewById(R.id.as_service_provider_name);
        mSPemail = findViewById(R.id.as_service_provider_email);
        mCategory = findViewById(R.id.as_category);
        mLocation = findViewById(R.id.as_location);
        mDetails = findViewById(R.id.as_details);
        mAddress = findViewById(R.id.as_address);
        mShowMore = findViewById(R.id.as_show_more);
        mServiceImage = findViewById(R.id.as_service_img);
        mSPimage = findViewById(R.id.as_service_provider_img);
        mAddProduct = findViewById(R.id.as_btn_add_product);
        mProductLayout = findViewById(R.id.as_product_layout);
        star_1 = findViewById(R.id.as_star_1);
        star_2 = findViewById(R.id.as_star_2);
        star_3 = findViewById(R.id.as_star_3);
        star_4 = findViewById(R.id.as_star_4);
        star_5 = findViewById(R.id.as_star_5);
        mEmpty = findViewById(R.id.as_empty);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        mRecycler = findViewById(R.id.as_recycler);
        serviceProvideCard = findViewById(R.id.as_service_provider_card);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mProductIDList = new ArrayList<>();
        mProductList = new ArrayList<>();
        mShowMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProductLayout.setVisibility(View.VISIBLE);
                mShowMore.setVisibility(View.GONE);
                try {
                    loadProduct();
                }catch (Exception e){
                    Log.d(TAG, "onClick: loadProduct: Exception: " + e.getMessage());
                }
            }
        });
        if (currentUser!=null){
            if (serviceProvideID.equals(currentUser.getUid())){
                mAddProduct.setVisibility(View.VISIBLE);
            }
        }
        mAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ServiceActivity.this, ProductAddActivity.class)
                        .putExtra(getString(R.string.service_id), serviceID)
                        .putExtra(getString(R.string.user_id), serviceProvideID)
                );
            }
        });
        serviceProvideCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (currentUser!=null){
//                    startActivity(new Intent(ServiceActivity.this, MessageActivity.class).putExtra(getString(R.string.receiver_id), mService.getUserID()));
//                }else {
//                    Toast.makeText(ServiceActivity.this, "Sign in first!", Toast.LENGTH_SHORT).show();
//                }
                startActivity(new Intent(ServiceActivity.this, ProfileActivity.class).putExtra(getString(R.string.user_id), serviceProvideID));
            }
        });
        star_1.setOnClickListener(view -> { ratings(1); });
        star_2.setOnClickListener(view -> { ratings(2); });
        star_3.setOnClickListener(view -> { ratings(3); });
        star_4.setOnClickListener(view -> { ratings(4); });
        star_5.setOnClickListener(view -> { ratings(5); });
    }

    private void loadProduct() {
        if (mProductIDList.size() < 1) {
            mRecycler.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
            return;
        }
        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_product));
        mProductList.clear();
        mProductAdapter = new ProductAdapter(ServiceActivity.this, mProductList);
        mRecycler.setAdapter(mProductAdapter);
        for (int i=0; i<mProductIDList.size(); i++) {
            String id = mProductIDList.get(i);
            mDatabaseReference.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()){
                        try {
                            Product product = snapshot.getValue(Product.class);
                            mProductList.add(product);
                            mProductAdapter.notifyItemInserted(mProductList.size() - 1);
                        }catch (Exception e){
                            Log.d(TAG, "onDataChange: Exception: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ServiceActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onCancelled: loadProduct:" + error.getMessage());
                }
            });
        }
    }

    private void loadService() {
        if (serviceID.isEmpty() || serviceProvideID.isEmpty()) { return; }
        DatabaseReference mServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_service));
        mServiceDatabaseReference.child(serviceID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            hasRated(snapshot.child(getString(R.string.firebase_rater)));
                            mService = snapshot.getValue(Service.class);
                            if (mService == null) { return; }
                            mTitle.setText(mService.getTitle());
                            double rating = Double.parseDouble(mService.getRatings());
                            rating = Math.round(rating * 10.0)/10.0;
                            mRatings.setText(String.valueOf(rating));
                            mCategory.setText(mService.getCategory());
                            mLocation.setText(mService.getCity() + ", " + mService.getState());
                            mDetails.setText(mService.getDetails());
                            mAddress.setText(mService.getAddress());
                            Glide.with(ServiceActivity.this)
                                    .load(mService.getImgUri())
                                    .placeholder(R.drawable.img_placeholder)
                                    .into(mServiceImage);
                            try {
                                DataSnapshot productSnaps = snapshot.child(getString(R.string.firebase_product));
                                mProductIDList.clear();
                                for (DataSnapshot productSnap : productSnaps.getChildren()){
                                    mProductIDList.add(productSnap.getValue().toString());
                                }
                            }catch (Exception e){
                                Toast.makeText(ServiceActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "onDataChange: loadService: loading product list: " + e.getMessage());
                            }
                            try {
                                LatLng latLng = new LatLng(Double.valueOf(mService.getLatitude()), Double.valueOf(mService.getLongitude()));
                                mMap.addMarker(new MarkerOptions().position(latLng));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mDefaultZoom));
                            }catch (Exception e){
                                Log.d(TAG, "onDataChange: Exception: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: loadService: Service: " + error.getMessage());
                    }
                });
        DatabaseReference mUserDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_users));
        mUserDatabaseReference.child(serviceProvideID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            User user = snapshot.getValue(User.class);
                            if (user == null) { return; }
                            mSPname.setText(user.getUserName());
                            mSPemail.setText(user.getEmail());
                            Glide.with(ServiceActivity.this)
                                    .load(user.getImgUrl())
                                    .placeholder(R.drawable.ic_account)
                                    .into(mSPimage);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: loadService: User: " + error.getMessage());
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: init");
        mMap = googleMap;
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(ServiceActivity.this);
            alertDialog.setTitle("Location required!")
                    .setMessage("To access this app properly, you need to give us permission to access your location")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(ServiceActivity.this, new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            }, MY_LOCATION_REQUEST_CODE);
                        }
                    });
            alertDialog.show();
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                mCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (!cameraFlag){
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, mDefaultZoom));
                    cameraFlag = !cameraFlag;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_LOCATION_REQUEST_CODE:
                if (grantResults.length > 0){
                    boolean allPermissionsGranted = true;
                    for (int i=0; i<grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            allPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission not granted: " + i);
                            break;
                        }
                    }
                    if (allPermissionsGranted){
                        try {
                            getCurrentLocation();
                        }catch (Exception e){
                            Log.d(TAG, "onRequestPermissionsResult: Exception: " + e.getMessage());
                        }
                    }
                }
        }
    }

    public void hasRated(DataSnapshot raterShot){
        if (!raterShot.exists() || currentUser == null)    return;
        String currentUserID = currentUser.getUid();
        for (DataSnapshot rater : raterShot.getChildren()){
            Log.d(TAG, "hasRated: rater: " + rater.getKey() + " ratings: " + rater.getValue());
            if (Objects.equals(rater.getKey(), currentUserID)){
                ratingsGiveByUser = Integer.parseInt(Objects.requireNonNull(rater.getValue()).toString());
                hasRated = true;
                rate(ratingsGiveByUser);
                return;
            }
        }
    }

    private void rateZero(){
        star_1.setImageResource(R.drawable.ic_star_0);
        star_2.setImageResource(R.drawable.ic_star_0);
        star_3.setImageResource(R.drawable.ic_star_0);
        star_4.setImageResource(R.drawable.ic_star_0);
        star_5.setImageResource(R.drawable.ic_star_0);
    }

    private void rate(int rating){
        rateZero();
        if (rating >= 1)    star_1.setImageResource(R.drawable.ic_baseline_star_24);
        if (rating >= 2)    star_2.setImageResource(R.drawable.ic_baseline_star_24);
        if (rating >= 3)    star_3.setImageResource(R.drawable.ic_baseline_star_24);
        if (rating >= 4)    star_4.setImageResource(R.drawable.ic_baseline_star_24);
        if (rating == 5)    star_5.setImageResource(R.drawable.ic_baseline_star_24);
    }

    public void ratings(int rating){
        if (currentUser == null){
            Toast.makeText(this, "Sign in first!", Toast.LENGTH_SHORT).show();
            return;
        }
        rate(rating);
        Service service = mService;
        int count = Integer.parseInt(service.getCount());
        double ratings = Double.parseDouble(service.getRatings());

        double newRatings;
        int newCount;
        if (hasRated){
            if (rating == ratingsGiveByUser)    return;
            newRatings = ((ratings * count) + rating - ratingsGiveByUser)/count;
            newCount = count;
        }else {
            newRatings = ((ratings * count) + rating)/(count+1);
            newCount = count+1;
        }
        service.setCount(String.valueOf(newCount));
        service.setRatings(String.valueOf(newRatings));

        DatabaseReference mServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_service));
        mServiceDatabaseReference.child(service.getServiceID()).child("count").setValue(service.getCount())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: count: " + e.getMessage());
                    }
                });
        mServiceDatabaseReference.child(service.getServiceID()).child("ratings").setValue(service.getRatings())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: ratings: " + e.getMessage());
                    }
                });
        String currentUserID = currentUser.getUid();
        mServiceDatabaseReference.child(service.getServiceID()).child(getString(R.string.firebase_rater)).child(currentUserID).setValue(rating)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: raterID: " + e.getMessage());
                    }
                });

    }

}