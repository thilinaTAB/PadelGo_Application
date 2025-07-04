package com.example.padelgo.user;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.padelgo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit; // Import TimeUnit

public class RideHistory extends AppCompatActivity {

    private RecyclerView recyclerViewUserHistory;
    private TextView txtUserHistoryMessage;
    private ImageView imgbtn_Back;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final String TAG = "RideHistory";

    private UserRideHistoryAdapter adapter;
    private List<RideHistoryItem> rideHistoryList;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_ride_history);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerViewUserHistory = findViewById(R.id.recycler_view_user_history);
        txtUserHistoryMessage = findViewById(R.id.txt_user_history_message);
        imgbtn_Back = findViewById(R.id.IMGBTN_Back);

        imgbtn_Back.setOnClickListener(view -> {
            Intent intent = new Intent(RideHistory.this, UserDashboard.class);
            startActivity(intent);
            finish();
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rideHistoryList = new ArrayList<>();
        adapter = new UserRideHistoryAdapter(rideHistoryList);
        recyclerViewUserHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUserHistory.setAdapter(adapter);

        loadRideHistory();
    }

    private void loadRideHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            txtUserHistoryMessage.setText("Loading ride history...");
            txtUserHistoryMessage.setVisibility(View.VISIBLE);
            recyclerViewUserHistory.setVisibility(View.GONE);

            db.collection("RideHistory")
                    .document(userId)
                    .collection("rides")
                    .orderBy("serverTimestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            rideHistoryList.clear();
                            if (task.getResult().isEmpty()) {
                                txtUserHistoryMessage.setText("No ride history found.");
                                txtUserHistoryMessage.setVisibility(View.VISIBLE);
                                recyclerViewUserHistory.setVisibility(View.GONE);
                            } else {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    RideHistoryItem item = new RideHistoryItem(document);
                                    rideHistoryList.add(item);
                                }
                                txtUserHistoryMessage.setVisibility(View.GONE);
                                recyclerViewUserHistory.setVisibility(View.VISIBLE);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Error getting ride history: ", task.getException());
                            txtUserHistoryMessage.setText("Error loading ride history.");
                            txtUserHistoryMessage.setVisibility(View.VISIBLE);
                            recyclerViewUserHistory.setVisibility(View.GONE);
                        }
                    });
        } else {
            txtUserHistoryMessage.setText("User not logged in.");
            txtUserHistoryMessage.setVisibility(View.VISIBLE);
            recyclerViewUserHistory.setVisibility(View.GONE);
            Log.w(TAG, "User not logged in, cannot load ride history.");
        }
    }

    private static class RideHistoryItem {
        String bicycleType;
        String location;
        String plan;
        String amount;
        String date;
        String payment;
        String rideStartRequestText;
        String formattedRideDuration;
        String documentId;

        public RideHistoryItem(QueryDocumentSnapshot document) {
            this.documentId = document.getId();
            this.bicycleType = document.getString("bikeType");
            this.location = document.getString("location");
            this.plan = document.getString("plan");
            this.amount = document.getString("amount");
            this.date = document.getString("dateAndTime");
            this.payment = document.getString("payment");

            Boolean rideStartRequestBool = document.getBoolean("rideStartRequest");
            if (Boolean.TRUE.equals(rideStartRequestBool)) {
                this.rideStartRequestText = "Yes";
            } else {
                this.rideStartRequestText = "No";
            }

            Object elapsedTimeObj = document.get("elapsedTime");
            if (elapsedTimeObj instanceof Long) {
                long totalSeconds = (Long) elapsedTimeObj;
                if (totalSeconds > 0) {
                    this.formattedRideDuration = formatDuration(totalSeconds);
                } else if ("Completed".equalsIgnoreCase(document.getString("rideStatus"))) {
                    this.formattedRideDuration = "0s";
                } else {
                    this.formattedRideDuration = "Not Started";
                }
            } else if (elapsedTimeObj instanceof Integer) {
                long totalSeconds = ((Integer) elapsedTimeObj).longValue();
                if (totalSeconds > 0) {
                    this.formattedRideDuration = formatDuration(totalSeconds);
                } else if ("Completed".equalsIgnoreCase(document.getString("rideStatus"))) {
                    this.formattedRideDuration = "0s";
                } else {
                    this.formattedRideDuration = "Not Started";
                }
            } else {
                String rideStatus = document.getString("rideStatus");
                if ("Completed".equalsIgnoreCase(rideStatus)) {
                    this.formattedRideDuration = "N/A";
                } else {
                    this.formattedRideDuration = "Not Started";
                }
            }
        }

        private static String formatDuration(long totalSeconds) {
            if (totalSeconds < 0) totalSeconds = 0;
            long days = TimeUnit.SECONDS.toDays(totalSeconds);
            totalSeconds -= TimeUnit.DAYS.toSeconds(days);
            long hours = TimeUnit.SECONDS.toHours(totalSeconds);
            totalSeconds -= TimeUnit.HOURS.toSeconds(hours);
            long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds);
            totalSeconds -= TimeUnit.MINUTES.toSeconds(minutes);
            long seconds = totalSeconds;

            StringBuilder sb = new StringBuilder();
            if (days > 0) {
                sb.append(days).append("days ");
            }
            if (hours > 0 || days > 0) {
                sb.append(hours).append("hrs ");
            }
            if (minutes > 0 || hours > 0 || days > 0) {
                sb.append(minutes).append("mins ");
            }
            sb.append(seconds).append("sec");

            return sb.toString().trim();
        }

        public String getBicycleType() { return bicycleType != null ? bicycleType : "N/A"; }
        public String getLocation() { return location != null ? location : "N/A"; }
        public String getPlan() { return plan != null ? plan : "N/A"; }
        public String getAmount() { return amount != null ? amount : "N/A"; }
        public String getDate() { return date != null ? date : "N/A"; }
        public String getPayment() { return payment != null ? payment : "N/A"; }
        public String getRideStartRequestText() { return rideStartRequestText; }
        public String getFormattedRideDuration() { return formattedRideDuration != null ? formattedRideDuration : "N/A"; }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RideHistoryItem that = (RideHistoryItem) o;
            return Objects.equals(documentId, that.documentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentId);
        }
    }

    private class UserRideHistoryAdapter extends RecyclerView.Adapter<UserRideHistoryAdapter.RideViewHolder> {

        private List<RideHistoryItem> adapterRideList;

        public UserRideHistoryAdapter(List<RideHistoryItem> rideList) {
            this.adapterRideList = rideList;
        }

        @NonNull
        @Override
        public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_ride_history, parent, false);
            return new RideViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
            RideHistoryItem currentItem = adapterRideList.get(position);

            holder.txtBikeType.setText(currentItem.getBicycleType());
            holder.txtLocation.setText(currentItem.getLocation());
            holder.txtDate.setText("On: " + currentItem.getDate());
            holder.txtPlanAmount.setText(String.format("(For %s, LKR %s)", currentItem.getPlan(), currentItem.getAmount()));
            holder.txtPayment.setText("- Payment: " + currentItem.getPayment());
            holder.txtRideStarted.setText("- Ride Started?: " + currentItem.getRideStartRequestText());
            holder.txtRideTime.setText("- Duration: " + currentItem.getFormattedRideDuration());
            holder.txtRideTime.setVisibility(View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return adapterRideList == null ? 0 : adapterRideList.size();
        }

        class RideViewHolder extends RecyclerView.ViewHolder {
            TextView txtBikeType, txtLocation, txtDate, txtPlanAmount, txtPayment, txtRideStarted, txtRideTime;

            public RideViewHolder(@NonNull View itemView) {
                super(itemView);
                txtBikeType = itemView.findViewById(R.id.item_txt_bike_type);
                txtLocation = itemView.findViewById(R.id.item_txt_location);
                txtDate = itemView.findViewById(R.id.item_txt_date);
                txtPlanAmount = itemView.findViewById(R.id.item_txt_plan_amount);
                txtPayment = itemView.findViewById(R.id.item_txt_payment);
                txtRideStarted = itemView.findViewById(R.id.item_txt_ride_started);
                txtRideTime = itemView.findViewById(R.id.item_txt_ride_time);
            }
        }
    }
}