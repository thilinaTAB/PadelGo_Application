package com.example.padelgo.stationOfficer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReleaseBicycleActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RideAdapter adapter;
    private List<Ride> rideList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private DatabaseReference realtimeDb;
    private static final String TAG = "ReleaseBicycleActivity";

    TextView txt_NULL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release_bicycle);

        recyclerView = findViewById(R.id.recyclerRelease);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        txt_NULL = findViewById(R.id.TXT_NULL_RELEASE);
        txt_NULL.setVisibility(View.GONE);

        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference("release_bicycle");

        adapter = new RideAdapter();
        recyclerView.setAdapter(adapter);

        fetchRides();
    }

    private void fetchRides() {
        firestore.collectionGroup("rides")
                .whereEqualTo("payment", "Paid")
                .whereEqualTo("bikeReleased", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    rideList.clear();
                    if (querySnapshot.isEmpty()) {
                        txt_NULL.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                        return;
                    }
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String userId = doc.getReference().getParent().getParent().getId();
                        String docId = doc.getId();
                        Ride ride = new Ride(
                                userId,
                                docId,
                                doc.getString("amount"),
                                doc.getString("location"),
                                doc.getString("plan"),
                                doc.getString("bicycleType")
                        );
                        rideList.add(ride);
                    }
                    adapter.notifyDataSetChanged();
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_release_bike, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Ride ride = rideList.get(position);
            holder.tvUserId.setText("User ID: " + ride.userId);
            holder.tvAmount.setText("Amount: " + ride.amount);
            holder.tvLocation.setText("Location: " + ride.location);
            holder.tvPlan.setText("Plan: " + ride.plan);
            holder.tvBicycleType.setText("Bicycle Type: " + ride.bicycleType);

            holder.btnRelease.setOnClickListener(v -> releaseBike(ride));
        }

        @Override
        public int getItemCount() {
            return rideList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserId, tvAmount, tvLocation, tvPlan, tvBicycleType;
            Button btnRelease;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUserId = itemView.findViewById(R.id.TXT_UID_RELEASE);
                tvAmount = itemView.findViewById(R.id.TXT_Amount_RELEASE);
                tvLocation = itemView.findViewById(R.id.TXT_Location_RELEASE);
                tvPlan = itemView.findViewById(R.id.TXT_Plan_RELEASE);
                tvBicycleType = itemView.findViewById(R.id.TXT_Type_RELEASE);
                btnRelease = itemView.findViewById(R.id.BTN_ReleaseBike);
            }
        }
    }

    private void releaseBike(Ride ride) {
        Map<String, Object> update = new HashMap<>();
        update.put("bikeReleased", true);

        firestore.collection("RideHistory")
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
        String userId, rideId, amount, location, plan, bicycleType;

        Ride(String userId, String rideId, String amount, String location, String plan, String bicycleType) {
            this.userId = userId;
            this.rideId = rideId;
            this.amount = amount;
            this.location = location;
            this.plan = plan;
            this.bicycleType = bicycleType;
        }
    }
}
