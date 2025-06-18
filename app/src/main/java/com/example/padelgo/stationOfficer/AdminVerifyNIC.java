package com.example.padelgo.stationOfficer;

import android.content.Intent;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.padelgo.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminVerifyNIC extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NICAdapter adapter;
    private List<UserNIC> userList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private DatabaseReference realtimeRef;
    private static final String TAG = "AdminVerifyNIC";

    TextView txt_NULL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_verify_nic);

        Log.d(TAG, "AdminVerifyNIC Activity opened");

        recyclerView = findViewById(R.id.recyclerNIC);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        txt_NULL = findViewById(R.id.TXT_NULL);
        txt_NULL.setVisibility(View.GONE);

        firestore = FirebaseFirestore.getInstance();
        realtimeRef = FirebaseDatabase.getInstance().getReference("user_verifications");

        adapter = new NICAdapter();
        recyclerView.setAdapter(adapter);


        fetchUsers();
    }

    private void fetchUsers() {
        firestore.collection("Users")
                .whereEqualTo("verificationStatus", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userList.clear();
                    Log.d(TAG, "Users fetched: " + querySnapshot.size());

                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No pending users", Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                        txt_NULL.setVisibility(View.VISIBLE);
                        return;
                    }

                    AtomicInteger loadedCount = new AtomicInteger();
                    int total = querySnapshot.size();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String uid = doc.getId();
                        String fullName = doc.getString("Full Name");
                        String nic = doc.getString("NIC Number");
                        String mobile = doc.getString("Mobile Number");

                        fetchNICImages(uid, fullName, nic, mobile, () -> {
                            if (loadedCount.incrementAndGet() == total) {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore fetch failed: " + e.getMessage());
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchNICImages(String uid, String fullName, String nic, String mobile, Runnable onComplete) {
        realtimeRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String frontUrl = snapshot.child("nic_front_url").getValue(String.class);
                    String backUrl = snapshot.child("nic_back_url").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);

                    Log.d(TAG, "NIC Data for " + uid + ": " + frontUrl + ", " + backUrl);

                    userList.add(new UserNIC(uid, fullName, nic, mobile, frontUrl, backUrl, status));
                } else {
                    Log.w(TAG, "No NIC data for UID: " + uid);
                }
                onComplete.run();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Realtime DB error: " + error.getMessage());
                onComplete.run();
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
            holder.uidText.setText("UID: " + user.uid);
            holder.nameText.setText("Name: " + user.fullName);
            holder.nicText.setText("NIC: " + user.nic);
            holder.mobileText.setText("Mobile: " + user.mobile);
            holder.statusText.setText("Status: " + user.status);

            Picasso.get().load(user.frontUrl).into(holder.imageFront);
            Picasso.get().load(user.backUrl).into(holder.imageBack);

            holder.buttonApprove.setOnClickListener(v -> {
                new AlertDialog.Builder(v.getContext()) // Use itemView's context
                        .setTitle("Confirm Approval")
                        .setMessage("Are you sure you want to approve this user's NIC?")
                        .setPositiveButton("Approve", (dialog, which) -> {
                            updateStatus(user.uid, "approved");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            holder.buttonReject.setOnClickListener(v -> {
                new AlertDialog.Builder(v.getContext()) // Use itemView's context
                        .setTitle("Confirm Rejection")
                        .setMessage("Are you sure you want to reject this user's NIC?")
                        .setPositiveButton("Reject", (dialog, which) -> {
                            updateStatus(user.uid, "rejected");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView uidText, nameText, nicText, mobileText, statusText;
            ImageView imageFront, imageBack;
            Button buttonApprove, buttonReject;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                uidText = itemView.findViewById(R.id.textUID);
                nameText = itemView.findViewById(R.id.textName);
                nicText = itemView.findViewById(R.id.TXT_NIC);
                mobileText = itemView.findViewById(R.id.TXT_Mobile);
                statusText = itemView.findViewById(R.id.TXT_Status);
                imageFront = itemView.findViewById(R.id.IMG_NICFront);
                imageBack = itemView.findViewById(R.id.IMG_NICBack);
                buttonApprove = itemView.findViewById(R.id.BTN_Approve);
                buttonReject = itemView.findViewById(R.id.BTN_Reject);
            }
        }
    }

    private void updateStatus(String uid, String newStatus) {
        realtimeRef.child(uid).child("status").setValue(newStatus);
        firestore.collection("Users").document(uid)
                .update("verificationStatus", newStatus)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                    fetchUsers(); // Refresh the list after update
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    static class UserNIC {
        String uid, fullName, nic, mobile, frontUrl, backUrl, status;

        UserNIC(String uid, String fullName, String nic, String mobile, String frontUrl, String backUrl, String status) {
            this.uid = uid;
            this.fullName = fullName;
            this.nic = nic;
            this.mobile = mobile;
            this.frontUrl = frontUrl;
            this.backUrl = backUrl;
            this.status = status;
        }
    }
}
