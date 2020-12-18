package com.pentagon.localhost_reborn.Fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pentagon.localhost_reborn.Adapter.ProductAdapter;
import com.pentagon.localhost_reborn.Object.Product;
import com.pentagon.localhost_reborn.Object.Service;
import com.pentagon.localhost_reborn.R;

import java.io.IOException;
import java.security.AlgorithmParameterGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "HomeFragment";
    private RecyclerView mRecycler;
    private ProgressBar mProgress;
    private List<Service> mServiceList;
    private List<Product> mProductList;
    private ProductAdapter mProductAdapter;

    private GoogleMap mMap;
    private static final int MY_LOCATION_REQUEST_CODE = 44;
    private boolean loadOnceFlag = true;

    private DatabaseReference mLocationDatabaseReference;
    private DatabaseReference mServiceDatabaseReference;
    private DatabaseReference mProductDatabaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         View view = inflater.inflate(R.layout.fragment_home, null);
         init(view);
         return view;
    }

    private void init(View view) {
        Log.d(TAG, "init: ");
        mLocationDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_location));
        mServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_service));
        mProductDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_product));
        mServiceList = new ArrayList<>();
        mProductList = new ArrayList<>();
        mProductAdapter = new ProductAdapter(getContext(), mProductList);
        mProgress = view.findViewById(R.id.fh_progress);
        mRecycler = view.findViewById(R.id.fh_recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecycler.setAdapter(mProductAdapter);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fh_map_fragment);
        mapFragment.getMapAsync(this);
    }

    private void loadData(String country, String state, String city) {
        Log.d(TAG, "loadData: init: country: " + country + " state: " + state + " city: " + city);
        if (country == null || state == null){
            Log.d(TAG, "loadServiceData: unable to load data: input is null");
            return;
        }
        mLocationDatabaseReference.child(country).child(state)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot stateSnap) {
                        mProgress.setVisibility(View.INVISIBLE);
                        if (stateSnap.exists()){
                            for (DataSnapshot citySnap : stateSnap.getChildren()){
                                try {
                                    for (DataSnapshot serviceSnap: citySnap.child(getContext().getString(R.string.firebase_service)).getChildren()){
                                        if (serviceSnap.exists()){
                                            loadServiceData(serviceSnap.getValue().toString());
                                        }
                                    }
                                }catch (Exception e){
                                    Log.d(TAG, "onDataChange: loadData: " + e.getMessage());
                                }
                            }
                        }else {
                            Log.d(TAG, "onDataChange: loadData: stateSnap dose not exits");
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle(country + "/" + state  + "/" + city)
                                    .setMessage("Currently no services are available in your area!")
                                    .setPositiveButton("OK", (dialogInterface, i) -> {});
                            builder.show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: Error: " + error.getMessage());
                    }
                });
    }

    private void loadServiceData(String serviceID) {
        Log.d(TAG, "loadServiceData: init");
        mServiceDatabaseReference.child(serviceID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            Service service = snapshot.getValue(Service.class);
                            if (service != null){
                                mServiceList.add(service);
                            }
                            try {
                                for (DataSnapshot productSnap: snapshot.child(getActivity().getString(R.string.firebase_product)).getChildren()){
                                    if (productSnap.exists()){
                                        loadProductData(productSnap.getValue().toString());
                                    }
                                }
                            }catch (Exception e){
                                Log.d(TAG, "onDataChange: loadServiceActivity: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: loadServiceData: " + error.getMessage());
                    }
                });
    }

    private void loadProductData(String productID) {
        mProductDatabaseReference.child(productID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            Product product = snapshot.getValue(Product.class);
                            if (product!=null){
                                mProductList.add(product);
                                mProductAdapter.notifyItemInserted(mProductList.size()-1);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private Address getAddress(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            return addresses.get(0);
        } catch (IOException e) {
            Log.d(TAG, "onCreate: IOException: " + e.getMessage());
        }
        return null;
    }

    private void getCurrentLocation() {
        Log.d(TAG, "getCurrentLocation: init");
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getCurrentLocation: asking for location");
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setTitle("Location required!")
                    .setMessage("To access this app properly, you need to give us permission to access your location")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{
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
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (loadOnceFlag){
                    Address address = getAddress(currentLocation.latitude, currentLocation.longitude);
                    Log.d(TAG, "onMyLocationChange: Address: " + address);
                    try {
                        loadData(address.getCountryName(), address.getAdminArea(), address.getLocality());
                        loadOnceFlag = !loadOnceFlag;
                    }catch (Exception e){
                        Log.d(TAG, "onMyLocationChange: Exception: " + e.getMessage());
                    }
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "onMapReady: init");
        try {
            getCurrentLocation();
        }catch (Exception e){
            Log.d(TAG, "onMapReady: Exception: " + e.getMessage());
        }
    }

}
