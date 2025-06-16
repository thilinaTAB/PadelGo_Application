// AdminVerifyNIC.java
package com.example.padelgo.stationOfficer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.padelgo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminVerifyNIC extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NICAdapter adapter;
    private List<UserNIC> userList;
    private DatabaseReference databaseRef;
    private static final String TAG = "AdminVerifyNIC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_verify_nic);

        recyclerView = findViewById(R.id.recyclerNIC);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new NICAdapter();
        recyclerView.setAdapter(adapter);

        databaseRef = FirebaseDatabase.getInstance().getReference("user_verifications");
        fetchNICs();
    }

    private void fetchNICs() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String uid = userSnap.getKey();
                    Map<String, Object> map = (Map<String, Object>) userSnap.getValue();
                    if (map == null) continue;

                    String front = (String) map.get("nic_front_url");
                    String back = (String) map.get("nic_back_url");
                    String status = (String) map.get("status");

                    userList.add(new UserNIC(uid, front, back, status));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });
    }

    class NICAdapter extends RecyclerView.Adapter<NICAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nic_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserNIC user = userList.get(position);
            holder.uidText.setText("User: " + user.uid);
            holder.statusText.setText("Status: " + user.status);
            Picasso.get().load(user.frontUrl).into(holder.imageFront);
            Picasso.get().load(user.backUrl).into(holder.imageBack);

            holder.buttonApprove.setOnClickListener(v -> updateStatus(user.uid, "approved"));
            holder.buttonReject.setOnClickListener(v -> updateStatus(user.uid, "rejected"));
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView uidText, statusText;
            ImageView imageFront, imageBack;
            Button buttonApprove, buttonReject;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                uidText = itemView.findViewById(R.id.textUID);
                statusText = itemView.findViewById(R.id.textStatus);
                imageFront = itemView.findViewById(R.id.imageNICFront);
                imageBack = itemView.findViewById(R.id.imageNICBack);
                buttonApprove = itemView.findViewById(R.id.buttonApprove);
                buttonReject = itemView.findViewById(R.id.buttonReject);
            }
        }
    }

    private void updateStatus(String uid, String newStatus) {
        databaseRef.child(uid).child("status").setValue(newStatus)
                .addOnSuccessListener(unused ->
                        Toast.makeText(AdminVerifyNIC.this, "Status updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(AdminVerifyNIC.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Simple model class
    static class UserNIC {
        String uid, frontUrl, backUrl, status;

        UserNIC(String uid, String frontUrl, String backUrl, String status) {
            this.uid = uid;
            this.frontUrl = frontUrl;
            this.backUrl = backUrl;
            this.status = status;
        }
    }
}
