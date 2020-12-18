package com.pentagon.localhost_reborn.Fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pentagon.localhost_reborn.Adapter.ServiceAdapter;
import com.pentagon.localhost_reborn.Object.Service;
import com.pentagon.localhost_reborn.R;
import com.pentagon.localhost_reborn.Sheet.BottomSheetService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PlacesFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "PlacesFragment";
    private float mDefaultZoom = 8.0f;
    private static final int MY_LOCATION_REQUEST_CODE = 44;
    private GoogleMap mMap;
    private BottomSheetBehavior mBottomSheetBehavior;
    private ProgressBar mProgress;
    private boolean flagCamera = false;
    private boolean loadOnceFlag = true;

    private DatabaseReference mLocationDatabaseReference;
    private DatabaseReference mServiceDatabaseReference;
    private RecyclerView mRecycler;
    private ServiceAdapter mServiceAdapter;
    private List<Service> mServiceList;

    private Map<Marker, Service> markServices = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_places, null);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fp_map);
        mapFragment.getMapAsync(this);
        View bottomSheet = v.findViewById(R.id.fp_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        init(v);
        return v;
    }

    private void init(View view) {
        mProgress = view.findViewById(R.id.fp_progress);
        mLocationDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_location));
        mServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_service));
        mRecycler = view.findViewById(R.id.fp_recycler);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecycler.setHasFixedSize(true);
        mServiceList = new ArrayList<>();
        mServiceAdapter = new ServiceAdapter(getContext(), mServiceList);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: init");
        mMap = googleMap;
        mProgress.setVisibility(View.INVISIBLE);
        getCurrentLocation();
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Service service = markServices.get(marker);
                BottomSheetService bottomSheetService = new BottomSheetService(service);
                bottomSheetService.show(getChildFragmentManager(), "ServiceSheet");
                return false;
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                LatLng currentLocation;
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (!flagCamera){
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, mDefaultZoom));
                    flagCamera=!flagCamera;
                }
                if (loadOnceFlag){
                    loadData(currentLocation);
                    loadOnceFlag = !loadOnceFlag;
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

    private void loadData(LatLng currentLocation) {
        Address address = getAddress(currentLocation.latitude, currentLocation.longitude);
        if (address == null ){
            Log.d(TAG, "loadData: Address is null");
            return;
        }
        String country = address.getCountryName();
        String state = address.getAdminArea();
        String city = address.getLocality();
        if (country == null || state == null){
            Log.d(TAG, "loadData: value/s inside address is/are null ");
            return;
        }
        mLocationDatabaseReference.child(country).child(state)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot stateshot) {
                        if (stateshot.exists()){
                            mRecycler.setAdapter(mServiceAdapter);
                            for (DataSnapshot citySnap: stateshot.getChildren()){
                                for (DataSnapshot serviceSnap: citySnap.child(getString(R.string.firebase_service)).getChildren()){
                                    String serviceID = serviceSnap.getValue().toString();
                                    if (serviceID != null) {
                                        loadServices(serviceID);
                                    }
                                }

                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: loadData: " + error.toString());
                    }
                });
    }

    private void loadServices(String serviceID) {
        mServiceDatabaseReference.child(serviceID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: init");
                if (snapshot.exists()){
                    Service service = snapshot.getValue(Service.class);
                    if (service == null) { return; }
                    mServiceList.add(service);
                    mServiceAdapter.notifyItemInserted(mServiceList.size()-1);
                    if (mMap != null){
                        LatLng latLng = new LatLng(Double.parseDouble(service.getLatitude()), Double.parseDouble(service.getLongitude()));
                        Log.d(TAG, "onDataChange: latLng: " + latLng.toString());
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(service.getTitle())
                                .snippet(service.getCategory())
                        );
                        markServices.put(marker, service);
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
}
