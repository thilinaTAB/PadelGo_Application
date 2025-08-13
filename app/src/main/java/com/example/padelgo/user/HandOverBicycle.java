package com.example.padelgo.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HandOverBicycle extends AppCompatActivity {

    private static final String TAG = "HandOverBicycle";

    RadioButton rb_Kandy, rb_Katugastota, rb_Peradeniya;
    RadioGroup rg_Stations;
    Button btn_HandOver;

    private FirebaseAuth fAuth;
    private FirebaseFirestore db;
    private DatabaseReference liveDatabase;

    private String bikeReturn;
    private String rideId;
    private String userId;
    private Long bookingtamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_hand_over_bicycle);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        rideId = intent.getStringExtra("rideId");
        userId = intent.getStringExtra("userId");

        if (userId == null || userId.isEmpty()) {
            FirebaseUser currentUser = fAuth.getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getUid();
                Log.w(TAG, "userId not passed in intent, fetched from fAuth: " + userId);
            } else {
                Toast.makeText(this, "Error: User not identified. Cannot proceed.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "userId is null or empty, and no authenticated user. Cannot proceed.");
                finish();
                return;
            }
        }

        if (rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Error: Ride ID not provided. Cannot proceed.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "rideId is null or empty. Cannot proceed.");
            finish();
            return;
        }

        rg_Stations = findViewById(R.id.RG_Stations);
        rb_Kandy = findViewById(R.id.RBTN_Kandy);
        rb_Katugastota = findViewById(R.id.RBTN_Katugastota);
        rb_Peradeniya = findViewById(R.id.RBTN_Peradeniya);
        btn_HandOver = findViewById(R.id.BTN_HandOver);
        btn_HandOver.setEnabled(false);

        fetchBikeTypeFromRideHistory();

        btn_HandOver.setOnClickListener(v -> {
            int selectedRadioButtonId = rg_Stations.getCheckedRadioButtonId();
            if (selectedRadioButtonId == -1) {
                Toast.makeText(HandOverBicycle.this, "Please select a handover station.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (bikeReturn == null || bikeReturn.isEmpty()) {
                Toast.makeText(HandOverBicycle.this, "Bike type not loaded. Please wait or try again.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Handover button clicked but bikeTypeToReturn is still null/empty.");
                return;
            }

            String handoverLocation = "";
            if (selectedRadioButtonId == R.id.RBTN_Kandy) {
                handoverLocation = "Kandy";
            } else if (selectedRadioButtonId == R.id.RBTN_Katugastota) {
                handoverLocation = "Katugastota";
            } else if (selectedRadioButtonId == R.id.RBTN_Peradeniya) {
                handoverLocation = "Peradeniya";
            }

            if (!handoverLocation.isEmpty()) {
                liveDatabase = FirebaseDatabase.getInstance().getReference("bicycleAvailability_" + handoverLocation);
                incrementBikeCountAtStation(handoverLocation);
            } else {
                Toast.makeText(HandOverBicycle.this, "Invalid station selected.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchBikeTypeFromRideHistory() {
        if (userId == null || rideId == null) {
            Log.e(TAG, "fetchBikeTypeFromRideHistory: userId or rideId is null.");
            Toast.makeText(this, "Error: Missing user or ride information.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Fetching bike type for rideId: " + rideId + " and userId: " + userId);
        db.collection("RideHistory").document(userId)
                .collection("rides").document(rideId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        bikeReturn = documentSnapshot.getString("bikeType");
                        bookingtamp = documentSnapshot.getLong("bookingTimestamp");

                        if (bikeReturn != null && !bikeReturn.isEmpty()) {
                            Log.i(TAG, "Successfully fetched bikeType: " + bikeReturn);
                            if (bookingtamp != null && bookingtamp > 0) { // Check if timestamp is valid
                                Log.i(TAG, "Successfully fetched bookingTimestamp: " + bookingtamp);
                                btn_HandOver.setEnabled(true);
                            } else {
                                Log.e(TAG, "bookingTimestamp field is null, missing, or invalid in Firestore document for rideId: " + rideId);
                                Toast.makeText(HandOverBicycle.this, "Error: Critical ride data (timestamp) missing.", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        } else {
                            Log.e(TAG, "bikeType field is null or empty in Firestore document for rideId: " + rideId);
                            Toast.makeText(HandOverBicycle.this, "Error: Could not determine bike type for this ride.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        Log.e(TAG, "Ride document does not exist in RideHistory for rideId: " + rideId + " and userId: " + userId);
                        Toast.makeText(HandOverBicycle.this, "Error: Ride details not found.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching bike type from Firestore for rideId: " + rideId, e);
                    Toast.makeText(HandOverBicycle.this, "Failed to load ride details. Please try again.", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void incrementBikeCountAtStation(String stationName) {
        if (bikeReturn == null || bikeReturn.isEmpty()) {
            Log.e(TAG, "incrementBikeCountAtStation: bikeTypeToReturn is null or empty. Cannot proceed.");
            Toast.makeText(HandOverBicycle.this, "Handover failed: Bike type unknown.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (liveDatabase == null) {
            Log.e(TAG, "incrementBikeCountAtStation: liveDatabase is null (station not selected or invalid?). Cannot proceed.");
            Toast.makeText(HandOverBicycle.this, "Handover failed: Station data error.", Toast.LENGTH_SHORT).show();
            return;
        }

        String bikeTypeKey = bikeReturn.replace(" bicycle", "").trim();

        Log.d(TAG, "Attempting to increment count for bikeTypeKey: " + bikeTypeKey + " at station: " + stationName);

        liveDatabase.child(bikeTypeKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer currentValue = dataSnapshot.getValue(Integer.class);
                if (currentValue != null) {
                    int newValue = currentValue + 1;
                    dataSnapshot.getRef().setValue(newValue)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Bike count incremented to " + newValue + " for " + bikeTypeKey + " at " + stationName);
                                Toast.makeText(HandOverBicycle.this, "Bicycle handed over at " + stationName, Toast.LENGTH_LONG).show();
                                updateRideAndHandOverStatusInFirestore(stationName); // Update status after successful RTDB update
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error incrementing bike count for " + bikeTypeKey + ": " + e.getMessage(), e);
                                Toast.makeText(HandOverBicycle.this, "Error updating bike count. Please try again.", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Log.w(TAG, "Could not read current bike count for " + bikeTypeKey + " at " + stationName + ". Path: " + dataSnapshot.getRef().toString() + ". Setting to 1.");
                    dataSnapshot.getRef().setValue(1)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Bike count set to 1 for " + bikeTypeKey + " at " + stationName);
                                Toast.makeText(HandOverBicycle.this, "Bicycle handed over at " + stationName, Toast.LENGTH_LONG).show();
                                updateRideAndHandOverStatusInFirestore(stationName);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error setting initial bike count for " + bikeTypeKey + ": " + e.getMessage(), e);
                                Toast.makeText(HandOverBicycle.this, "Error updating bike count. Please try again.", Toast.LENGTH_SHORT).show();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching bike count for increment at " + stationName + ", bikeTypeKey " + bikeTypeKey + ": " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(HandOverBicycle.this, "Error fetching bike data. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRideAndHandOverStatusInFirestore(String handoverLocationName) {
        if (rideId == null || rideId.isEmpty()) {
            Log.w(TAG, "rideId is null, cannot update statuses in Firestore.");
            navigateToDashboard("Handover complete. Ride status update skipped (no rideId).");
            return;
        }

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null, cannot update statuses in Firestore.");
            navigateToDashboard("Handover complete. Ride status update skipped (no userId).");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("rideStatus", "Completed");
        updates.put("handOverStatus", "Completed");
        updates.put("handOverLocation", handoverLocationName);
        updates.put("handOverTimestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // Update in AllHistory
        if (userId != null && bookingtamp != null) {
            db.collection("AllHistory")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("bookingTimestamp", bookingtamp)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot allHistoryDoc = queryDocumentSnapshots.getDocuments().get(0);
                            Log.d(TAG, "Found AllHistory document: " + allHistoryDoc.getId() + " via query. Updating it.");
                            allHistoryDoc.getReference().update(updates)
                                    .addOnFailureListener(e -> Log.e(TAG, "Error updating statuses in queried AllHistory for ride", e));
                        } else {
                            Log.w(TAG, "Could not find corresponding document in AllHistory to update via query for userId: " + userId + " and bookingTimestamp: " + bookingtamp);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error querying AllHistory", e));
        } else {
            Log.w(TAG, "Missing userId or bookingTimestamp, cannot query AllHistory.");
        }

        // Update in user's RideHistory
        db.collection("RideHistory").document(userId).collection("rides").document(rideId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ride & Handover status updated in user's RideHistory for ride: " + rideId);
                    navigateToDashboard("Bicycle handover successful at " + handoverLocationName + "!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating statuses in user's RideHistory for ride: " + rideId, e);
                    navigateToDashboard("Handover complete. Error updating some ride details.");
                });

    }

    private void navigateToDashboard(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(HandOverBicycle.this, UserDashboard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
