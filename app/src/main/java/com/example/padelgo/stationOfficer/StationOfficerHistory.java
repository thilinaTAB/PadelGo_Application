package com.example.padelgo.stationOfficer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.padelgo.R;
import com.example.padelgo.user.RideHistory;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class StationOfficerHistory extends AppCompatActivity {

    private static final String TAG = "AdminHistory";

    private RecyclerView recyclerViewAdminHistory;
    private TextView txtAdminNoHistoryMessage;
    private ImageView imgBtnBack;
    private AdminHistoryAdapter adapter;
    private List<RideItem> rideList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, String> userNamesCache = new HashMap<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_officer_history);

        recyclerViewAdminHistory = findViewById(R.id.recycler_view_admin_history);
        txtAdminNoHistoryMessage = findViewById(R.id.txt_admin_no_history_message);
        imgBtnBack = findViewById(R.id.IMGBTN_Back);

        imgBtnBack.setOnClickListener(v -> finish());

        rideList = new ArrayList<>();
        adapter = new AdminHistoryAdapter(rideList);
        recyclerViewAdminHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdminHistory.setAdapter(adapter);

        loadAllRideHistory();
    }

    private void loadAllRideHistory() {
        txtAdminNoHistoryMessage.setText("Loading history...");
        txtAdminNoHistoryMessage.setVisibility(View.VISIBLE);
        recyclerViewAdminHistory.setVisibility(View.GONE);

        db.collection("AllHistory")
                .orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        txtAdminNoHistoryMessage.setText("No ride history found.");
                        txtAdminNoHistoryMessage.setVisibility(View.VISIBLE);
                        recyclerViewAdminHistory.setVisibility(View.GONE);
                        rideList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    List<RideItem> newRides = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Map<String, Object> data = document.getData();
                        RideItem ride = new RideItem(data);
                        newRides.add(ride);
                    }

                    rideList.clear();
                    rideList.addAll(newRides);

                    if (rideList.isEmpty()) {
                        txtAdminNoHistoryMessage.setText("No ride history found.");
                        txtAdminNoHistoryMessage.setVisibility(View.VISIBLE);
                        recyclerViewAdminHistory.setVisibility(View.GONE);
                    } else {
                        txtAdminNoHistoryMessage.setVisibility(View.GONE);
                        recyclerViewAdminHistory.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                    fetchUserNamesForList();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting all ride history: ", e);
                    txtAdminNoHistoryMessage.setText("Error retrieving ride history.");
                    txtAdminNoHistoryMessage.setVisibility(View.VISIBLE);
                    recyclerViewAdminHistory.setVisibility(View.GONE);
                });
    }

    private void fetchUserNamesForList() {
        for (int i = 0; i < rideList.size(); i++) {
            RideItem ride = rideList.get(i);
            String userId = ride.getUserId();
            final int position = i;

            if (userId != null && !userId.isEmpty()) {
                if (userNamesCache.containsKey(userId)) {
                    ride.setUserName(userNamesCache.get(userId));
                    adapter.notifyItemChanged(position);
                } else {
                    db.collection("Users").document(userId).get()
                            .addOnSuccessListener(userDocument -> {
                                if (userDocument.exists()) {
                                    String fullName = userDocument.getString("Full Name");
                                    String userName = (fullName != null && !fullName.isEmpty()) ? fullName : "Unknown User";
                                    userNamesCache.put(userId, userName);
                                    ride.setUserName(userName);
                                } else {
                                    userNamesCache.put(userId, "Unknown User (No Doc)");
                                    ride.setUserName("Unknown User (No Doc)");
                                }
                                adapter.notifyItemChanged(position);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching user name for userId: " + userId, e);
                                userNamesCache.put(userId, "Error Fetching Name");
                                ride.setUserName("Error Fetching Name");
                                adapter.notifyItemChanged(position);
                            });
                }
            } else {
                ride.setUserName("Invalid User ID");
                adapter.notifyItemChanged(position);
            }
        }
    }

    private static class RideItem {
        String userId;
        String userName;
        String bikeType;
        String location;
        String plan;
        String amount;
        String paymentStatus;
        String rideRequestedStatus;
        String dateAndTime;
        String orderPlacedTimestamp;
        String formattedRideDuration;
        Timestamp serverTimestamp;
        String returned;
        String fine;

        public RideItem(Map<String, Object> data) {
            this.userId = (String) data.get("userId");
            this.bikeType = data.get("bikeType") != null ? data.get("bikeType").toString() : "N/A";
            this.location = data.get("location") != null ? data.get("location").toString() : "N/A";
            this.plan = data.get("plan") != null ? data.get("plan").toString() : "N/A";
            this.amount = data.get("amount") != null ? data.get("amount").toString() : "N/A";
            this.paymentStatus = data.get("payment") != null ? data.get("payment").toString() : "N/A";
            this.dateAndTime = data.get("dateAndTime") != null ? data.get("dateAndTime").toString() : "N/A";
            this.returned = data.get("handOverLocation") != null ? data.get("handOverLocation").toString(): "N/A";
            Object fineAmountObj = data.get("fineAmount");
            if (fineAmountObj instanceof Number) {
                this.fine = String.valueOf(fineAmountObj);
            } else if (fineAmountObj instanceof String) {
                this.fine = (String) fineAmountObj;
            } else if (fineAmountObj == null) {
                this.fine = "0";
            } else {
                this.fine = String.valueOf(fineAmountObj);
            }

            Boolean rideStartRequestBool = null;
            Object rideStartRequestObj = data.get("rideStartRequest");
            if (rideStartRequestObj instanceof Boolean) {
                rideStartRequestBool = (Boolean) rideStartRequestObj;
            }
            this.rideRequestedStatus = Boolean.TRUE.equals(rideStartRequestBool) ? "Yes" : "No";

            Object serverTimestampObj = data.get("serverTimestamp");
            if (serverTimestampObj instanceof Timestamp) {
                this.serverTimestamp = (Timestamp) serverTimestampObj;
                Date date = this.serverTimestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                this.orderPlacedTimestamp = sdf.format(date);
            } else {
                this.orderPlacedTimestamp = "N/A";
            }

            Object elapsedTimeObj = data.get("elapsedTime");
            if (elapsedTimeObj instanceof Long) {
                long totalSeconds = (Long) elapsedTimeObj;
                if (totalSeconds > 0) {
                    this.formattedRideDuration = formatDuration(totalSeconds);
                } else if ("Completed".equalsIgnoreCase((String) data.get("rideStatus"))) {
                    this.formattedRideDuration = "0s";
                }
                else {
                    this.formattedRideDuration = "Not Started/Ended";
                }
            } else if (elapsedTimeObj instanceof Integer) {
                long totalSeconds = ((Integer) elapsedTimeObj).longValue();
                if (totalSeconds > 0) {
                    this.formattedRideDuration = formatDuration(totalSeconds);
                } else if ("Completed".equalsIgnoreCase((String) data.get("rideStatus"))) {
                    this.formattedRideDuration = "0s";
                } else {
                    this.formattedRideDuration = "Not Started/Ended";
                }
            }
            else {

                String rideStatus = (String) data.get("rideStatus");
                if ("Completed".equalsIgnoreCase(rideStatus)) {
                    this.formattedRideDuration = "N/A (no duration)";
                } else {
                    this.formattedRideDuration = "Not Ended";
                }
            }

            this.userName = "Loading...";
        }

        private String formatDuration(long totalSeconds) {
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


        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getBikeType() { return bikeType; }
        public String getLocation() { return location; }
        public String getPlan() { return plan; }
        public String getAmount() { return amount; }
        public String getPaymentStatus() { return paymentStatus; }
        public String getRideRequestedStatus() { return rideRequestedStatus; }
        public String getDateAndTime() { return dateAndTime; }
        public String getOrderPlacedTimestamp() { return orderPlacedTimestamp; }
        public String getFormattedRideDuration() { return formattedRideDuration; }
        public String getReturned() { return returned; }
        public String getFine() { return fine; }

        public void setUserName(String userName) { this.userName = userName; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RideItem rideItem = (RideItem) o;
            return Objects.equals(userId, rideItem.userId) &&
                    Objects.equals(orderPlacedTimestamp, rideItem.orderPlacedTimestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, orderPlacedTimestamp);
        }
    }

    private class AdminHistoryAdapter extends RecyclerView.Adapter<AdminHistoryAdapter.RideViewHolder> {

        private List<RideItem> adapterRideList;

        public AdminHistoryAdapter(List<RideItem> rideList) {
            this.adapterRideList = rideList;
        }

        @NonNull
        @Override
        public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_station_officer_ride_history, parent, false);
            return new RideViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
            RideItem currentRide = adapterRideList.get(position);
            holder.txtUserName.setText(currentRide.getUserName());
            holder.txtBikeType.setText(currentRide.getBikeType());
            holder.txtLocation.setText("Location: " + currentRide.getLocation());
            holder.txtPlan.setText("Plan: " + currentRide.getPlan());
            holder.txtAmount.setText("Amount: " + currentRide.getAmount());
            holder.txtDateTime.setText("Booked For: " + currentRide.getDateAndTime());
            holder.txtOrderPlaced.setText("Order Placed: " + currentRide.getOrderPlacedTimestamp());
            holder.txtPaymentStatus.setText("Payment: " + currentRide.getPaymentStatus());
            holder.txtRideRequested.setText("Ride Requested?: " + currentRide.getRideRequestedStatus());
            holder.txtRideTime.setText("Ride Duration: " + currentRide.getFormattedRideDuration());
            holder.txtReturnBike.setText("Returned to: " + currentRide.getReturned());
            holder.txtFine.setText("Fine or other charges: " + currentRide.getFine());
        }

        @Override
        public int getItemCount() {
            return adapterRideList == null ? 0 : adapterRideList.size();
        }

        class RideViewHolder extends RecyclerView.ViewHolder {
            TextView txtUserName, txtBikeType, txtLocation, txtPlan, txtAmount,
                    txtDateTime, txtOrderPlaced, txtPaymentStatus, txtRideRequested, txtRideTime, txtReturnBike,txtFine;
            public RideViewHolder(@NonNull View itemView) {
                super(itemView);
                txtUserName = itemView.findViewById(R.id.item_txt_user_name);
                txtBikeType = itemView.findViewById(R.id.item_txt_bike_type);
                txtLocation = itemView.findViewById(R.id.item_txt_location);
                txtPlan = itemView.findViewById(R.id.item_txt_plan);
                txtAmount = itemView.findViewById(R.id.item_txt_amount);
                txtDateTime = itemView.findViewById(R.id.item_txt_date_time);
                txtOrderPlaced = itemView.findViewById(R.id.item_txt_order_placed);
                txtPaymentStatus = itemView.findViewById(R.id.item_txt_payment_status);
                txtRideRequested = itemView.findViewById(R.id.item_txt_ride_requested);
                txtRideTime = itemView.findViewById(R.id.item_txt_ride_time);
                txtReturnBike = itemView.findViewById(R.id.item_txt_ReturnBike);
                txtFine = itemView.findViewById(R.id.item_txt_Fine);
            }
        }
    }
}