package com.pentagon.localhost_reborn;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pentagon.localhost_reborn.Object.Product;
import com.pentagon.localhost_reborn.Object.User;

public class ProductAddActivity extends AppCompatActivity {

    private static final String TAG = "ProductAddActivity";
    private int PICK_IMAGE_REQUEST = 11;

    private ImageView mImage, mImageUpload;
    private TextView mTitle, mDetails;
    private Button mSubmit;
    private String serviceID, serviceProvideID;
    private ProgressDialog mProgressDialog;
    private Uri imgUri;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_add);
        serviceID = getIntent().getStringExtra(getString(R.string.service_id));
        serviceProvideID = getIntent().getStringExtra(getString(R.string.user_id));
        init();
    }

    private void init() {
        mImage = findViewById(R.id.apa_image);
        mImageUpload = findViewById(R.id.apa_image_upload);
        mTitle = findViewById(R.id.apa_title);
        mDetails = findViewById(R.id.apa_details);
        mSubmit = findViewById(R.id.apa_submit);
        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });
        mImageUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });
    }



    private void submit() {
        String title = mTitle.getText().toString().trim();
        String details = mDetails.getText().toString().trim();
        if (title.isEmpty() || details.isEmpty() || imgUri.toString().isEmpty()) {
            Toast.makeText(this, "Fill all details!", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_product));
        String productID = mDatabaseReference.push().getKey();
        Product product = new Product(productID, serviceProvideID, serviceID, title, details, "imgUri", "0");
        try {
            upload(product);
        }catch (Exception e){
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "submit: Exception: " + e.getMessage());
        }
    }

    private void upload(Product product) {
        mProgressDialog = ProgressDialog.show(ProductAddActivity.this, "", "Uploading....", true);
        final StorageReference imgStorageReference = FirebaseStorage.getInstance().getReference(getString(R.string.firebase_users)).child(product.getUserID()).child(getString(R.string.firebase_service)).child(product.getServiceID()).child(getString(R.string.firebase_product)).child(product.getProductID()).child(System.currentTimeMillis()+"."+getFileExtension(imgUri));
        imgStorageReference.putFile(imgUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imgStorageReference.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        product.setImgUri(uri.toString());
                                        addProduct(product);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        mProgressDialog.dismiss();
                                        Toast.makeText(ProductAddActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "onFailure: upload: getDownloadUrl: Exception: " + e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressDialog.dismiss();
                        Toast.makeText(ProductAddActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onFailure: upload: uploadImage: Exception: " + e.getMessage());
                    }
                });
    }

    private void addProduct(Product product) {
        mProgressDialog.setMessage("Uploading... (1/2)");
        DatabaseReference mProductDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_product));
        mProductDatabaseReference.child(product.getProductID()).setValue(product)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateServiceProduct(product);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressDialog.dismiss();
                        Toast.makeText(ProductAddActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onFailure: addProduct: Exception: " + e.getMessage());
                    }
                });
    }

    private void updateServiceProduct(Product product) {
        mProgressDialog.setMessage("Uploading... (2/2)");
        DatabaseReference mServiceDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_service)).child(product.getServiceID()).child(getString(R.string.firebase_product));
        String id = mServiceDatabaseReference.push().getKey();
        mServiceDatabaseReference.child(id).setValue(product.getProductID())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mProgressDialog.dismiss();
                        Toast.makeText(ProductAddActivity.this, "Success", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ProductAddActivity.this, MainActivity.class));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressDialog.dismiss();
                        Toast.makeText(ProductAddActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onFailure: updateServiceProduct: " + e.getMessage());
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

}