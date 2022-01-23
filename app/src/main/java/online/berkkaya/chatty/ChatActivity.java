package online.berkkaya.chatty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;

import online.berkkaya.chatty.model.Chat;

public class ChatActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView rvChat;
    private EditText edtMessage;
    private  ImageView imvSend;
    private LinearLayoutManager mLayoutManager;
    private FirestoreRecyclerAdapter<Chat, ChatViewHolder> adapter;
    String uid, idChatRoom;
    String uidFriend;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        uid = currentUser.getUid();

        rvChat = findViewById(R.id.rvChat);
        edtMessage = findViewById(R.id.edtMessage);
        imvSend = findViewById(R.id.imvSend);

        idChatRoom = getIntent().getExtras().getString("idChatRoom");
        uidFriend = getIntent().getExtras().getString("uidFriend");

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(false);
        mLayoutManager.setStackFromEnd(true);

        rvChat.setHasFixedSize(true);
        rvChat.setLayoutManager(mLayoutManager);

        FirestoreRecyclerOptions<Chat> options = new FirestoreRecyclerOptions.Builder<Chat>()
                .setQuery(db.collection("chatRoom").document(idChatRoom).collection("chat").orderBy("date"), Chat.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Chat, ChatViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull Chat model) {
                holder.setList(model.getUid(), model.getMessage(), getApplicationContext());
            }

            @NonNull
            @Override
            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_message, parent, false);
                return new ChatViewHolder(view);
            }
        };

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mLayoutManager.smoothScrollToPosition(rvChat, null, adapter.getItemCount());
            }
        });

        rvChat.setAdapter(adapter);
        adapter.startListening();

        imvSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = edtMessage.getText().toString().trim();
                if (TextUtils.isEmpty(message)) {

                } else {
                    HashMap<String, Object> dataMessage = new HashMap<>();
                    dataMessage.put("date", FieldValue.serverTimestamp());
                    dataMessage.put("message", message);
                    dataMessage.put("uid", uid);
                    db.collection("chatRoom").document(idChatRoom).collection("chat").document().set(dataMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            edtMessage.setText("");
                        }
                    });
                }
            }
        });
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {
        View mView;
        ConstraintLayout clMessage;
        TextView txtMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            clMessage = mView.findViewById(R.id.clMessage);
            txtMessage = mView.findViewById(R.id.txtMessage);
        }

        public void setList(String uidMessage, String message, Context context) {
            if (uidMessage.equals(uid)) {
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(clMessage);
                constraintSet.setHorizontalBias(R.id.txtMessage, 1.0f);
                constraintSet.applyTo(clMessage);
                txtMessage.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.bg_message, context.getTheme()));
                txtMessage.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                txtMessage.setText(message);
            } else {
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(clMessage);
                constraintSet.setHorizontalBias(R.id.txtMessage, 0.0f);
                constraintSet.applyTo(clMessage);
                txtMessage.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.bg_message_friend, context.getTheme()));
                txtMessage.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                txtMessage.setText(message);
            }
        }
    }
}