package online.berkkaya.chatty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

import online.berkkaya.chatty.model.Friend;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db;
    private FloatingActionButton fabAdd;
    private String uid;
    private RecyclerView rvFriends;
    private LinearLayoutManager mLayoutManager;
    private FirestoreRecyclerAdapter<Friend, FriendViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    Intent i = new Intent(MainActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                } else {
                    OpenMainActivity();
                }
            }
        };
    }
    private void OpenMainActivity() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        uid = currentUser.getUid();

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        rvFriends = findViewById(R.id.rvFriends);
        rvFriends.setHasFixedSize(true);
        rvFriends.setLayoutManager(mLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvFriends.getContext(), mLayoutManager.getOrientation());
        rvFriends.addItemDecoration(dividerItemDecoration);

        FirestoreRecyclerOptions<Friend> options = new FirestoreRecyclerOptions.Builder<Friend>()
                .setQuery(db.collection("user").document(uid).collection("friend"), Friend.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Friend, FriendViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FriendViewHolder holder, int position, @NonNull Friend model) {
                String uidFriend = getSnapshots().getSnapshot(position).getId();
                holder.setList(uidFriend);

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        goChatRoom(model.getIdChatRoom(), uidFriend);
                    }
                });
            }

            @NonNull
            @Override
            public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_friend, parent, false);
                return new FriendViewHolder(view);
            }
        };
        rvFriends.setAdapter(adapter);
        adapter.startListening();

        fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context;
                Dialog dialog = new Dialog(MainActivity.this);
                dialog.setTitle("Enter User Uid: ");
                dialog.setContentView(R.layout.layout_add);
                dialog.show();

                EditText edtID = dialog.findViewById(R.id.edtId);
                Button btnOk = dialog.findViewById(R.id.btnOk);

                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String idUser = edtID.getText().toString();
                        if (TextUtils.isEmpty(idUser)) {
                            edtID.setError("required");
                        } else {
                            db.collection("user").whereEqualTo("id", idUser).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    if (queryDocumentSnapshots.isEmpty()) {
                                        edtID.setError("Id not found!");
                                    } else {
                                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                                            String uidFriend = documentSnapshot.getId();
                                            if (uid.equals(uidFriend)) {
                                                edtID.setError("Wrong ID");
                                            } else {
                                                dialog.cancel();
                                                checkFriendExists(uidFriend);
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void checkFriendExists(String uidFriend) {
        db.collection("user").document(uid).collection("friend").document().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if (documentSnapshot.exists()) {
                        String idChatRoom = documentSnapshot.get("idChatRoom", String.class);
                        goChatRoom(idChatRoom, uidFriend);
                    } else {
                        createNewChatRoom(uidFriend);
                    }
                }
            }
        });
    }

    private void createNewChatRoom(String uidFriend) {
        HashMap<String, Object> dataChatRoom = new HashMap<>();
        dataChatRoom.put("dateAdded", FieldValue.serverTimestamp());
        db.collection("chatRoom").document(uid+uidFriend).set(dataChatRoom).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                // kullanıcı verisine yazma
                HashMap<String, Object> dataFriend = new HashMap<>();
                dataFriend.put("idChatRoom", uid+uidFriend);
                db.collection("user").document(uid).collection("friend").document(uidFriend).set(dataFriend).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // kullanıcının arkadaşının verisine yazma
                        HashMap<String, Object> dataUserFriend = new HashMap<>();
                        dataUserFriend.put("idChatRoom", uid+uidFriend);
                        db.collection("user").document(uidFriend).collection("friend").document(uid).set(dataUserFriend).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                goChatRoom(uid+uidFriend, uidFriend);
                            }
                        });
                    }
                });
            }
        });
    }

    private void goChatRoom(String idChatRoom, String uidFriend) {
        Intent i = new Intent(MainActivity.this, ChatActivity.class);
        i.putExtra("idChatRoom", idChatRoom);
        i.putExtra("uidFriend", uidFriend);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();

            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        } else if (id == R.id.action_profile) {
            Intent i = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
    }

    public class FriendViewHolder extends RecyclerView.ViewHolder {
        View mView;
        ImageView imgProfile;
        TextView txtName;

        public FriendViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            imgProfile = mView.findViewById(R.id.imgProfile);
            txtName = mView.findViewById(R.id.txtName);
        }

        public void setList(String uidFriend) {
            db.collection("user").document(uidFriend).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.get("name", String.class);
                            txtName.setText(name);
                        }
                    }
                }
            });
        }
    }
}