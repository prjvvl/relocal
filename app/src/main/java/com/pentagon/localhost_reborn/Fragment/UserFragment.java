package com.pentagon.localhost_reborn.Fragment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.pentagon.localhost_reborn.Adapter.ServiceAdapter;
import com.pentagon.localhost_reborn.MainActivity;
import com.pentagon.localhost_reborn.MessageActivity;
import com.pentagon.localhost_reborn.Object.Service;
import com.pentagon.localhost_reborn.Object.User;
import com.pentagon.localhost_reborn.R;
import com.pentagon.localhost_reborn.ServiceAddActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

public class UserFragment extends Fragment {
    private static final String TAG = "UserFragment";

    private static final int REQUEST_SIGN_IN_CODE = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserDBR;
    private DatabaseReference mServiceDBR;
    private List<Service> mServiceList;
    private ServiceAdapter mServiceAdapter;
    private FirebaseUser currentUser;

    private ProgressDialog mProgressDialog;
    private String userToMessageID;
    private ImageView mProfile, mEmpty;
    private TextView mUsername, mEmail, mPhone, mEdit;
    private Button mAdd, mMessage;
    private ProgressBar mProgress;
    private RecyclerView mRecycler;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: init");
        View v = inflater.inflate(R.layout.fragment_user, null);
        init(v);
        return v;
    }

    private void init(View view) {
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(Objects.requireNonNull(getContext()), gso);
        mServiceList = new ArrayList<>();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserDBR = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_users));
        mServiceDBR = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_service));
        mProfile = view.findViewById(R.id.fu_profile);
        mEmpty = view.findViewById(R.id.fu_img_empty);
        mEmail = view.findViewById(R.id.fu_email);
        mUsername = view.findViewById(R.id.fu_username);
        mPhone = view.findViewById(R.id.fu_phone);
        mEdit = view.findViewById(R.id.fu_edit);
        mAdd = view.findViewById(R.id.fu_btn_add);
        mMessage = view.findViewById(R.id.fu_message);
        mProgress = view.findViewById(R.id.fu_progress);
        mRecycler = view.findViewById(R.id.fu_recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (signIn()){
                    startActivity(new Intent(getActivity(), ServiceAddActivity.class));
                }
            }
        });
        mEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (signIn()){
                    signOut();
                }
            }
        });
        mMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userToMessageID!=null){
                    startActivity(new Intent(getActivity(), MessageActivity.class).putExtra(getString(R.string.receiver_id), userToMessageID));
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        update();
    }

    private void update() {
        String uid = getActivity().getIntent().getStringExtra(getString(R.string.user_id));
        if (uid != null){
            if (currentUser != null){
                if (currentUser.getUid().equals(uid)){
                    if (signIn()){
                        mEmpty.setImageDrawable(getContext().getDrawable(R.drawable.empty));
                        mUsername.setText(mAuth.getCurrentUser().getDisplayName());
                        mEmail.setText(mAuth.getCurrentUser().getEmail());
                        Glide.with(getContext()).load(mAuth.getCurrentUser().getPhotoUrl()).placeholder(R.drawable.ic_account).into(mProfile);
                        getData(mAuth.getCurrentUser().getUid());
                    }else {
                        mEmpty.setImageDrawable(getContext().getDrawable(R.drawable.login));
                    }
                }else {
                    mAdd.setVisibility(View.INVISIBLE);
                    mEdit.setVisibility(View.INVISIBLE);
                    mMessage.setVisibility(View.VISIBLE);
                    userToMessageID = uid;
                    mEmpty.setImageDrawable(getContext().getDrawable(R.drawable.empty));
                    DatabaseReference mUserDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_users));
                    mUserDatabaseReference.child(uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()) return;
                                    User user = snapshot.getValue(User.class);
                                    if (user!=null){
                                        try {
                                            mUsername.setText(user.getUserName());
                                            mEmail.setText(user.getEmail());
                                            Glide.with(getContext())
                                                    .load(user.getImgUrl())
                                                    .placeholder(R.drawable.ic_account)
                                                    .into(mProfile);
                                        }catch (Exception e){
                                            Log.d(TAG, "onDataChange: E" + e.getMessage());
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.d(TAG, "onCancelled: " + error.getMessage());
                                }
                            });
                    getData(uid);

                }
            }
        }else {
            if (signIn()){
                mEmpty.setImageDrawable(getContext().getDrawable(R.drawable.empty));
                mUsername.setText(mAuth.getCurrentUser().getDisplayName());
                mEmail.setText(mAuth.getCurrentUser().getEmail());
                Glide.with(getContext()).load(mAuth.getCurrentUser().getPhotoUrl()).placeholder(R.drawable.ic_account).into(mProfile);
                getData(mAuth.getCurrentUser().getUid());
            }else {
                mEmpty.setImageDrawable(getContext().getDrawable(R.drawable.login));
            }
        }
    }

    private void getData(String userId) {
        mProgress.setVisibility(View.VISIBLE);
        mEmpty.setVisibility(View.INVISIBLE);
        mServiceList.clear();
        DatabaseReference mDatabaseReference = mUserDBR.child(userId).child(getString(R.string.firebase_service));
        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() < 1) { mEmpty.setVisibility(View.VISIBLE); }
                mServiceAdapter = new ServiceAdapter(getContext(), mServiceList);
                mRecycler.setAdapter(mServiceAdapter);
                mProgress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: getUserData: " + error.getMessage());
                mProgress.setVisibility(View.INVISIBLE);
            }
        });

        mDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildAdded: snap: " + snapshot.getValue());
                getServiceData(snapshot.getValue().toString());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: getUserData: childEvent: " + error.getMessage());

            }
        });

    }

    private void getServiceData(String serviceID) {
        Log.d(TAG, "getServiceData: init: " + serviceID);
        DatabaseReference mDatabaseReference = mServiceDBR.child(serviceID);
        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Service service = snapshot.getValue(Service.class);
                Log.d(TAG, "onDataChange: Service: " + service.getTitle());
                mServiceList.add(service);
                mServiceAdapter.notifyItemInserted(mServiceList.size()-1);
//                mServiceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: getServiceData: " + error.getMessage());
            }
        });
    }

    //    SignIn -------------------------
    private boolean signIn() {
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "signIn: Signing in");
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("User need to sign in first in order to access these features!")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mProgressDialog = ProgressDialog.show(getContext(), "", "Please wait..", true);
                            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                            startActivityForResult(signInIntent, REQUEST_SIGN_IN_CODE);
                        }
                    }).show();
            return false;
        }
        return true;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGN_IN_CODE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account);
            } catch (ApiException e) {
                mProgressDialog.dismiss();
                Log.d(TAG, "onActivityResult: ApiException: " + e.getMessage());
                Toast.makeText(getContext(), "ApiException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(Objects.requireNonNull(getActivity()), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            addUser(account);
                        }else {
                            Toast.makeText(getContext(), "Failed! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "onComplete: Failed: " + task.getException());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addUser(GoogleSignInAccount account) {
        if (mAuth.getCurrentUser() == null) { return; }
        FirebaseUser fireUser = mAuth.getCurrentUser();
        User user = new User(fireUser.getDisplayName(), fireUser.getEmail(), fireUser.getUid(), "none", account.getPhotoUrl().toString());
        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_users));
        mDatabaseReference.child(fireUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    mDatabaseReference.child(fireUser.getUid()).setValue(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    update();
                                    mProgressDialog.dismiss();
                                    Toast.makeText(getContext(), "Success", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    mProgressDialog.dismiss();
                                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "onFailure: addUser: " + e.getMessage());
                                }
                            });
                }else {
                    update();
                    mProgressDialog.dismiss();
                    Toast.makeText(getContext(), "Success", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mProgressDialog.dismiss();
                Toast.makeText(getContext(), "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onFailure: checkUser: " + error.getMessage());
            }
        });

    }

    // ------------------------- SignIn

    private void signOut(){
        Log.d(TAG, "signIn: Signing out");
        try {
            mAuth.signOut();
            mGoogleSignInClient.signOut();
            Toast.makeText(getActivity(), "Sign out", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), MainActivity.class).putExtra(getString(R.string.show_fragment), getString(R.string.home_fragment)));
        }catch (Exception e){
            Log.d(TAG, "signOut: Exception: " + e.getMessage());
        }
    }

}
