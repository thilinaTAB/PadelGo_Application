package com.example.padelgo.stationOfficer;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.padelgo.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class AdminRents extends AppCompatActivity {

    private TextView txt_All;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, String> userNames = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_rents);
        txt_All = findViewById(R.id.TXT_All);

        loadAllRideHistory();
    }

    private void loadAllRideHistory() {
        db.collection("AllHistory")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    StringBuilder allRideData = new StringBuilder();
                    if (querySnapshot.isEmpty()) {
                        txt_All.setText("No ride history found.");
                        return;
                    }

                    final int[] rideCount = {0};
                    final int totalRides = querySnapshot.size();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Map<String, Object> ride = document.getData();
                        String userId = (String) ride.get("userId");

                        if (userId != null && !userNames.containsKey(userId)) {

                            fetchUserName(userId, ride, allRideData, totalRides, rideCount);
                        } else {

                            displayRideData(ride, allRideData, totalRides, rideCount);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRents", "Error getting all ride history: ", e);
                    txt_All.setText("Error retrieving ride history.");
                });
    }

    private void fetchUserName(String userId, Map<String, Object> ride, StringBuilder allRideData, int totalRides, int[] rideCount) {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(userDocument -> {
                    String fullName = userDocument.getString("Full Name");
                    String userName = fullName != null ? fullName : "Unknown User";
                    userNames.put(userId, userName);
                    displayRideData(ride, allRideData, totalRides, rideCount);
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRents", "Error fetching user name for userId: " + userId, e);
                    userNames.put(userId, "Unknown User");
                    displayRideData(ride, allRideData, totalRides, rideCount);
                });
    }

    private void displayRideData(Map<String, Object> ride, StringBuilder allRideData, int totalRides, int[] rideCount) {
        String userId = (String) ride.get("userId");
        String userName = userId != null && userNames.containsKey(userId) ? userNames.get(userId) : "Unknown User";
        String bikeType = ride.get("bikeType") != null ? ride.get("bikeType").toString() : "Unknown Bike";
        String location = ride.get("location") != null ? ride.get("location").toString() : "Unknown Location";
        String plan = ride.get("plan") != null ? ride.get("plan").toString() : "Unknown Plan";
        String amount = ride.get("amount") != null ? ride.get("amount").toString() : "Unknown Amount";
        String dateAndTime = ride.get("dateAndTime") != null ? ride.get("dateAndTime").toString() : "Unknown Date/Time";

        allRideData.append("User: ").append(userName).append("\n");
        allRideData.append("Bike: ").append(bikeType).append("\n");
        allRideData.append("Location: ").append(location).append("\n");
        allRideData.append("Plan: ").append(plan).append("\n");
        allRideData.append("Amount: ").append(amount).append("\n");
        allRideData.append("Date/Time: ").append(dateAndTime).append("\n");
        allRideData.append("-------------------------------------------\n\n");

        rideCount[0]++;
        if (rideCount[0] == totalRides) {
            txt_All.setText(allRideData.toString());
        }
    }
}