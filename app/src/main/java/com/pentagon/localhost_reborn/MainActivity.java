package com.pentagon.localhost_reborn;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pentagon.localhost_reborn.Fragment.ChatFragment;
import com.pentagon.localhost_reborn.Fragment.HomeFragment;
import com.pentagon.localhost_reborn.Fragment.PlacesFragment;
import com.pentagon.localhost_reborn.Fragment.UserFragment;
import com.pentagon.localhost_reborn.Object.Service;
import com.pentagon.localhost_reborn.Object.User;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_SIGN_IN_CODE = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        String fragment = getIntent().getStringExtra(getString(R.string.show_fragment));
        if (fragment != null){
            if (fragment.equals(getString(R.string.user_fragment))) loadFragment(new UserFragment());
            else if (fragment.equals(getString(R.string.chat_fragment))) loadFragment(new ChatFragment());
            else if (fragment.equals(getString(R.string.places_fragment))) loadFragment(new PlacesFragment());
            else loadFragment(new HomeFragment());
        }else loadFragment(new HomeFragment());
        BottomNavigationView navView = findViewById(R.id.am_nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null){
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.am_frame_layout, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    loadFragment(new HomeFragment());
                    return true;
                case R.id.navigation_places:
                    loadFragment(new PlacesFragment());
                    return true;
                case R.id.navigation_chat:
//                    signOut();
                    loadFragment(new ChatFragment());
                    return true;
                case R.id.navigation_user:
//                    signIn();
                    loadFragment(new UserFragment());
                    return true;
            }
            return false;
        }
    };

//    SignIn -------------------------
    private void signIn() {
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "signIn: Signing in");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("User need to sign in first in order to access these features!")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mProgressDialog = ProgressDialog.show(MainActivity.this, "", "Please wait....", true);
                            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                            startActivityForResult(signInIntent, REQUEST_SIGN_IN_CODE);
                        }
                    }).show();
            return;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGN_IN_CODE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onActivityResult: ApiException: " + e.getMessage());
                mProgressDialog.dismiss();
            }
        }
    }

    private void firebaseAuth(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            addUser(account);
                        }else {
                            Toast.makeText(MainActivity.this, "Failed! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            mProgressDialog.dismiss();
                            Log.d(TAG, "onComplete: Failed: " + task.getException());
                        }
                    }
                });
    }

    private void addUser(GoogleSignInAccount account) {
        if (mAuth.getCurrentUser() == null) { return; }
        FirebaseUser fireUser = mAuth.getCurrentUser();
        User user = new User(fireUser.getDisplayName(), fireUser.getEmail(), fireUser.getUid(), "none", account.getPhotoUrl().toString());
        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference("User");
        mDatabaseReference.child(fireUser.getUid()).setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mProgressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressDialog.dismiss();
                        Log.d(TAG, "onFailure: addUser: " + e.getMessage());
                    }
                });
    }

// ------------------------- SignIn

    private void signOut(){
        Log.d(TAG, "signIn: Signing out");
        try {
            mAuth.signOut();
            mGoogleSignInClient.signOut();
            Toast.makeText(this, "Sign out", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Log.d(TAG, "signOut: Exception: " + e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Exit localhost?")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
//
                    }
                });
        builder.show();
    }
}

