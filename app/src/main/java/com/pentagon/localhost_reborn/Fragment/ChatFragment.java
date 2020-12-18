package com.pentagon.localhost_reborn.Fragment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.pentagon.localhost_reborn.Adapter.UserAdapter;
import com.pentagon.localhost_reborn.Object.User;
import com.pentagon.localhost_reborn.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private static final int REQUEST_SIGN_IN_CODE = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private ProgressDialog mProgressDialog;

    private List<User> mList;
    private DatabaseReference mUserDatabaseReference;
    private UserAdapter mUserAdapter;
    private RecyclerView mRecycler;
    private FloatingActionButton mAdd;
    private TextView mEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_chat, null);
        init(view);
        return view;
    }


    public void init(View view){
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(Objects.requireNonNull(getContext()), gso);
        mRecycler = view.findViewById(R.id.fc_recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mList = new ArrayList<>();
        mUserAdapter = new UserAdapter(getActivity(), mList);
        mRecycler.getRecycledViewPool().clear();
        mEmpty = view.findViewById(R.id.fc_empty);
        mRecycler.setAdapter(mUserAdapter);
        mUserDatabaseReference = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(getContext()).getString(R.string.firebase_users));
        mAdd = view.findViewById(R.id.fc_add);
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (signIn()){
                    Toast.makeText(getContext(), "working", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (signIn())   loadData();
    }

    private void loadData() {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference mDatabaseReference = mUserDatabaseReference.child(uid);
        mDatabaseReference.child(Objects.requireNonNull(getContext()).getString(R.string.firebase_chat))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        mList.clear();
                        for (DataSnapshot snapID : snapshot.getChildren()){
                            String userID = snapID.getKey();
                            loadUser(userID);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: loadData" + error.toString());
                    }
                });
    }

    private void loadUser(String userID) {
        mUserDatabaseReference.child(userID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        User user = snapshot.getValue(User.class);
                        if (user == null)   return;
                        mList.add(user);
                        mRecycler.getRecycledViewPool().clear();
                        mUserAdapter.notifyDataSetChanged();
                        if (mEmpty.getVisibility() == View.VISIBLE){
                            mEmpty.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: loadUser: " + error.getMessage());
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
    //  ---------------------------------------
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
                                    loadData();
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
                    loadData();
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

}
