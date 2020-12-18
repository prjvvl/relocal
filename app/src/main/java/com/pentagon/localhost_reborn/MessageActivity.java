package com.pentagon.localhost_reborn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pentagon.localhost_reborn.Adapter.MessageAdapter;
import com.pentagon.localhost_reborn.Object.Message;
import com.pentagon.localhost_reborn.Object.User;

import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MessageActivity extends AppCompatActivity {

    private static final String TAG = "MessageActivity";
    private ImageView mImage;
    private TextView mName, mEmail, mSend;
    private RecyclerView mRecycler;
    private EditText mEdit;
    private User mReceiver, mSender;
    private DatabaseReference mUserDatabaseReference;
    private DatabaseReference mMessageDatabaseReference;
    private List<Message> mList;
    private MessageAdapter mMessageAdapter;
    private String receiver;
    private CardView mCard;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        init();
        loadReceiver();
    }

    public void init(){
        receiver = getIntent().getStringExtra(getString(R.string.receiver_id));
        mImage = findViewById(R.id.amsg_image);
        mName = findViewById(R.id.amsg_name);
        mEmail = findViewById(R.id.amsg_email);
        mRecycler = findViewById(R.id.amsg_recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
        mRecycler.getRecycledViewPool().clear();
        mList = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(MessageActivity.this, mList);
        mRecycler.setAdapter(mMessageAdapter);
        mEdit = findViewById(R.id.amsg_edit);
        mSend = findViewById(R.id.amsg_send);
        mSend.setOnClickListener(view -> {  sendMessage();  });
        mUserDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_users));
        mMessageDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_message));
        mCard = findViewById(R.id.amsg_card);
        mCard.setOnClickListener(view -> {
            startActivity(new Intent(MessageActivity.this, ProfileActivity.class).putExtra(getString(R.string.user_id), mReceiver.getUserID()));
        });
    }

    public void loadReceiver(){
        Log.d(TAG, "loadReceiver: init");
        mUserDatabaseReference.child(receiver)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            mReceiver = snapshot.getValue(User.class);
                            if (mReceiver == null)  return;
                            mEmail.setText(mReceiver.getEmail());
                            mName.setText(mReceiver.getUserName());
                            Glide.with(MessageActivity.this)
                                    .load(mReceiver.getImgUrl())
                                    .placeholder(R.drawable.ic_account)
                                    .into(mImage);
                            loadSender();
                        }catch (Exception e){
                            Log.d(TAG, "onDataChange: Exception: " + e.getMessage());
                            Toast.makeText(MessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: Exception: " + error.getMessage());
                    }
                });
    }

    public void loadSender(){
        Log.d(TAG, "loadSender: init");
        String id = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        mUserDatabaseReference.child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            mSender = snapshot.getValue(User.class);
                            loadMessageIDs();
                        }catch (Exception e){
                            Log.d(TAG, "onDataChange: Sender: " + e.getMessage());
                            Toast.makeText(MessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: DatabaseError: " + error);
                    }
                });

    }

    private void sendMessage(){
        Log.d(TAG, "sendMessage: init");
        String msg = mEdit.getText().toString().trim();
        if (msg.isEmpty())  return;
        if (mReceiver == null || mSender == null){
            Toast.makeText(this, "Failed to load initial data", Toast.LENGTH_SHORT).show();
            return;
        }
        String id = mMessageDatabaseReference.push().getKey();
        Message message = new Message(id, mSender.getUserID(), mReceiver.getUserID(), msg, "Time", "Date");
        mMessageDatabaseReference.child(id).setValue(message)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        try {
                            mUserDatabaseReference.child(mSender.getUserID()).child(getString(R.string.firebase_chat)).child(mReceiver.getUserID()).child(id).setValue(id)
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG, "onFailure: sendMessage: update sender data: " + e.getMessage());
                                        }
                                    });
                            mUserDatabaseReference.child(mReceiver.getUserID()).child(getString(R.string.firebase_chat)).child(mSender.getUserID()).child(id).setValue(id)
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG, "onFailure: sendMessage: update receiver data: " + e.getMessage());
                                        }
                                    });
                        }catch (Exception e){
                            Log.d(TAG, "onSuccess: sendMessage Exception: " + e.getMessage());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        Log.d(TAG, "onFailure: sendMessage: " + e.getMessage());
                    }
                });
        mEdit.setText("");
//        Update stuff
    }

    public void loadMessageIDs(){
        Log.d(TAG, "loadMessageIDs: init");
        mUserDatabaseReference.child(mSender.getUserID()).child(getString(R.string.firebase_chat)).child(mReceiver.getUserID())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot msgID : snapshot.getChildren()){
                            try {
                                loadMessage(Objects.requireNonNull(msgID.getValue()).toString());
                            }catch (Exception e){
                                Log.d(TAG, "onDataChange: Exception: " + e.getMessage());
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: loadMessageIDs" + error.getMessage());
                    }
                });
    }

    public void loadMessage(String id){
        Log.d(TAG, "loadMessage: init");
        if (id == null){
            Log.d(TAG, "loadMessage: null id");
            return;
        }
        mMessageDatabaseReference.child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        if (!snapshot.exists()) return;
                        Message message = snapshot.getValue(Message.class);
                        if (isNew(message)){
                            mList.add(message);
                            Log.d(TAG, "onDataChange: message added: " + message.getId());
                            mMessageAdapter.notifyItemInserted(mList.size()-1);
                            mRecycler.scrollToPosition(mList.size()-1);
                        }
                    }catch (Exception e){
                        Log.d(TAG, "onDataChange: Exception: loadMessage: " + e.getMessage());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, "onCancelled: Error: loadMessage: " + error.getMessage());
                }
            });
    }

    private boolean isNew(Message message) {
        Log.d(TAG, "isNew: init");
        for (int i=mList.size()-1; i>=0; i--){
            Message msg = mList.get(i);
            if (msg.getId().equals(message.getId())) return false;
        }
        return true;
    }



}