package com.example.padelgo.stationOfficer;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.padelgo.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ReleaseBicycleActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RideAdapter adapter;
    private List<Ride> rideList = new ArrayList<>();
    private FirebaseFirestore fstore;
    private DatabaseReference realtimeDb;
    private static final String TAG = "ReleaseBicycleActivity";

    TextView txt_NULL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_release_bicycle);

        recyclerView = findViewById(R.id.recyclerRelease);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        txt_NULL = findViewById(R.id.TXT_NULL_RELEASE);
        txt_NULL.setVisibility(View.GONE);

        fstore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference("release_bicycle");

        adapter = new RideAdapter();
        recyclerView.setAdapter(adapter);

        fetchRides();
    }

    private void fetchRides() {
        fstore.collectionGroup("rides")
                .whereEqualTo("payment", "Paid")
                .whereEqualTo("bikeReleased", false)
                .orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    rideList.clear();
                    if (querySnapshot.isEmpty()) {
                        txt_NULL.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "No rides found matching criteria.");
                        return;
                    }
                    txt_NULL.setVisibility(View.GONE);
                    List<DocumentSnapshot> rideDocs = querySnapshot.getDocuments();

                    AtomicInteger pendingUserFetches = new AtomicInteger(rideDocs.size());

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String userId = doc.getReference().getParent().getParent().getId();
                        String docId = doc.getId();
                        Ride ride = new Ride(
                                userId,
                                docId,
                                doc.getString("Full Name"),
                                null,
                                doc.getString("amount"),
                                doc.getString("location"),
                                doc.getString("plan"),
                                doc.getString("bikeType")
                        );
                        fstore.collection("Users").document(userId).get()
                                .addOnSuccessListener(userDocumentSnapshot -> {
                                    if (userDocumentSnapshot.exists()) {
                                        ride.userNIC = userDocumentSnapshot.getString("NIC Number");
                                    } else {
                                        Log.w(TAG, "User document not found for userId: " + userId);
                                        ride.userNIC = "N/A";
                                    }
                                    rideList.add(ride);

                                    if (pendingUserFetches.decrementAndGet() == 0) {
                                        adapter.notifyDataSetChanged();
                                        Log.d(TAG, "All rides and user NICs fetched.");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching user details for " + userId + ": " + e.getMessage());
                                    ride.userNIC = "--";
                                    rideList.add(ride);

                                    if (pendingUserFetches.decrementAndGet() == 0) {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching rides: " + e.getMessage());
                    Toast.makeText(this, "Failed to fetch rides", Toast.LENGTH_SHORT).show();
                });
    }

    class RideAdapter extends RecyclerView.Adapter<RideAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_release_bike, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Ride ride = rideList.get(position);
            holder.txt_Name.setText("Name: " + ride.userName);
            holder.txt_NIC.setText("NIC: " + ride.userNIC);
            holder.txt_Amount.setText("Amount: " + ride.amount);
            holder.txt_Location.setText("Location: " + ride.location);
            holder.txt_Plan.setText("Plan: " + ride.plan);
            holder.txt_BicycleType.setText("Bicycle Type: " + ride.bicycleType);

            holder.updateReleaseButtonState();

            holder.sw_CheckNIC.setOnCheckedChangeListener((buttonView, isChecked) -> holder.updateReleaseButtonState());
            holder.sw_BicycleCheck.setOnCheckedChangeListener((buttonView, isChecked) -> holder.updateReleaseButtonState());

            holder.btn_Release.setOnClickListener(v -> {
                releaseBike(ride);
            });
        }

        @Override
        public int getItemCount() {
            return rideList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txt_Name, txt_NIC,txt_Amount, txt_Location, txt_Plan, txt_BicycleType;
            Button btn_Release;
            Switch sw_CheckNIC, sw_BicycleCheck;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                txt_Name = itemView.findViewById(R.id.TXT_Name_RELEASE);
                txt_NIC = itemView.findViewById(R.id.TXT_NIC_RELEASE);
                txt_Amount = itemView.findViewById(R.id.TXT_Amount_RELEASE);
                txt_Location = itemView.findViewById(R.id.TXT_Location_RELEASE);
                txt_Plan = itemView.findViewById(R.id.TXT_Plan_RELEASE);
                txt_BicycleType = itemView.findViewById(R.id.TXT_Type_RELEASE);
                btn_Release = itemView.findViewById(R.id.BTN_ReleaseBicycle);
                sw_CheckNIC = itemView.findViewById(R.id.SW_CheckNIC);
                sw_BicycleCheck = itemView.findViewById(R.id.SW_BicycleCkeck);
            }
            void updateReleaseButtonState() {
                boolean isEnabled = sw_CheckNIC.isChecked() && sw_BicycleCheck.isChecked();
                btn_Release.setEnabled(isEnabled);
                if (isEnabled) {
                    btn_Release.setBackgroundColor(Color.parseColor("#7E206E"));
                }
                else {
                    btn_Release.setBackgroundColor(Color.parseColor("#B6B1B6"));
                }

            }
        }
    }

    private void releaseBike(Ride ride) {
        Map<String, Object> update = new HashMap<>();
        update.put("bikeReleased", true);

        fstore.collection("RideHistory")
                .document(ride.userId)
                .collection("rides")
                .document(ride.rideId)
                .update(update)
                .addOnSuccessListener(unused -> {
                    realtimeDb.child(ride.userId).child("bikeReleased").setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Bike released for " + ride.userId, Toast.LENGTH_SHORT).show();
                                fetchRides();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "RTDB update failed", e);
                                Toast.makeText(this, "RTDB update failed", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore update failed", e);
                    Toast.makeText(this, "Failed to release bike", Toast.LENGTH_SHORT).show();
                });
    }

    static class Ride {
        String userId, rideId, userName, userNIC, amount, location, plan, bicycleType;

        Ride(String userId, String rideId, String userName, String userNIC, String amount, String location, String plan, String bicycleType) {
            this.userId = userId;
            this.rideId = rideId;
            this.userName = userName;
            this.userNIC = userNIC;
            this.amount = amount;
            this.location = location;
            this.plan = plan;
            this.bicycleType = bicycleType;
        }
    }
}
