package com.pentagon.localhost_reborn;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearSmoothScroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pentagon.localhost_reborn.Object.Service;
import com.pentagon.localhost_reborn.Object.User;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ServiceAddActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ServiceAddActivity";
    private int PICK_IMAGE_REQUEST = 11;

    private float mDefaultZoom = 12.0f;
    private static final int MY_LOCATION_REQUEST_CODE = 44;
    private LatLng mCurrentLocation;
    private GoogleMap mMap;
    private Address mServiceAddress;
    private boolean cameraFlag = false;

    private ImageView mImage, mImageUpload;
    private EditText mTitle, mDetails;
    private Spinner mSpinner;
    private TextView mAddress;
    private Button mSubmit;


    private ProgressDialog mProgressDialog;
    private Uri imgUri;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_add);
        init();
    }

    private void init() {
        mAuth = FirebaseAuth.getInstance();
        mImage = findViewById(R.id.asa_image);
        mImageUpload = findViewById(R.id.asa_image_upload);
        mTitle = findViewById(R.id.asa_title);
        mDetails = findViewById(R.id.asa_details);
        mSpinner = findViewById(R.id.asa_spinner);
        mAddress = findViewById(R.id.asa_address);
        mSubmit = findViewById(R.id.asa_submit);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.asa_map_fragment);
        mapFragment.getMapAsync(this);
        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Submit();
            }
        });
        mImageUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });
    }

    private void Submit() {
        String title = mTitle.getText().toString().trim();
        String category = mSpinner.getSelectedItem().toString();
        String details = mDetails.getText().toString().trim();
        if (title.isEmpty() || category.isEmpty() || details.isEmpty() || imgUri.toString().isEmpty() ||  mServiceAddress == null) {
            Toast.makeText(this, "Fill all details!", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference mServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_service));
        String serviceID = mServiceDatabaseReference.push().getKey();
        String userID = mAuth.getCurrentUser().getUid();
        Service service = new Service(serviceID, userID, title, category, details, mServiceAddress.getAddressLine(0), mServiceAddress.getLocality(), mServiceAddress.getAdminArea(), mServiceAddress.getCountryName(), String.valueOf(mServiceAddress.getLatitude()), String.valueOf(mServiceAddress.getLongitude()), "imgUrl", "0.0", "0");
        try {
            uploadImage(service);
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Submit: Exception: " + e.getMessage());
        }
    }

    private void uploadImage(Service service) {
        mProgressDialog = ProgressDialog.show(ServiceAddActivity.this, "", "Uploading....", true);
        final StorageReference imgStorageReference = FirebaseStorage.getInstance().getReference(getString(R.string.firebase_users)).child(service.getUserID()).child(getString(R.string.firebase_service)).child(service.getServiceID()).child(System.currentTimeMillis()+"."+getFileExtension(imgUri));
        imgStorageReference.putFile(imgUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imgStorageReference.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        service.setImgUri(uri.toString());
                                        addService(service);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        mProgressDialog.dismiss();
                                        Toast.makeText(ServiceAddActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "onFailure: uploadImage: getDownloadUrl: Exception: " + e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressDialog.dismiss();
                        Toast.makeText(ServiceAddActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onFailure: uploadImage: putFile: Exception: " + e.getMessage());
                    }
                });
    }

    private void addService(Service service) {
        mProgressDialog.setMessage("Uploading... (1/3)");
        DatabaseReference mServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_service));
        mServiceDatabaseReference.child(service.getServiceID()).setValue(service)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateUserService(service);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mProgressDialog.dismiss();
                Toast.makeText(ServiceAddActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onFailure: addService: Exception: " + e.getMessage());
            }
        });
    }

    private void updateUserService(Service service) {
        mProgressDialog.setMessage("Uploading... (2/3)");
        DatabaseReference mUserServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_users)).child(service.getUserID()).child(getString(R.string.firebase_service));
        String id = mUserServiceDatabaseReference.push().getKey();
        mUserServiceDatabaseReference.child(id).setValue(service.getServiceID())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateLocationService(service);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressDialog.dismiss();
                        Toast.makeText(ServiceAddActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onFailure: updateUserService: Exception: " + e.getMessage());
                    }
                });
    }

    private void updateLocationService(Service service) {
        mProgressDialog.setMessage("Uploading... (3/3)");
        DatabaseReference mLocationServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_location)).child(service.getCountry()).child(service.getState()).child(service.getCity()).child(getString(R.string.firebase_service));
        String id = mLocationServiceDatabaseReference.push().getKey();
        mLocationServiceDatabaseReference.child(id).setValue(service.getServiceID())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mProgressDialog.dismiss();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressDialog.dismiss();
                        Toast.makeText(ServiceAddActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onFailure: updateLocationService: Exception: " + e.getMessage());
                    }
                });
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null ){
            imgUri = data.getData();
            mImage.setImageURI(imgUri);
        }
    }

    private Address getAddress(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            return addresses.get(0);
        } catch (IOException e) {
            Log.d(TAG, "onCreate: IOException: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocation();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
                mServiceAddress = getAddress(latLng.latitude, latLng.longitude);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAddress.setText(mServiceAddress.getAddressLine(0));
                    }
                });
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(ServiceAddActivity.this);
            alertDialog.setTitle("Location required!")
                    .setMessage("To access this app properly, you need to give us permission to access your location")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(ServiceAddActivity.this, new String[]{
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
}



//        Address address = getAddress(17.701592, 73.994722);

//    String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
//    String city = addresses.get(0).getLocality();
//    String state = addresses.get(0).getAdminArea();
//    String country = addresses.get(0).getCountryName();
//    String postalCode = addresses.get(0).getPostalCode();
//    String knownName = addresses.get(0).getFeatureName();