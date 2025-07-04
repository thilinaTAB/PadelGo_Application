package com.example.padelgo.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MyRides extends AppCompatActivity {
    TextView txt_Bicycle, txt_Location, txt_Plan, txt_Amount, txt_Date, txt_Paid, txt_Timer, txt_wait;
    Button btn_Cancel, btn_Pay, btn_Start, btn_End;
    ImageView imgbtn_Back;
    CardView view_MyRide, view_NoRideData;
    private FirebaseAuth fAuth;
    private FirebaseFirestore db;
    private DatabaseReference realtimeDB;
    private DatabaseReference userReleaseBikeRef;
    private DatabaseReference rideTimerRef;

    private static final String TAG = "MyRides";
    private Handler timerHandler = new Handler(Looper.getMainLooper());

    private long rideStartTimeMillis = 0;
    private boolean isTimerRunning = false;
    private ValueEventListener bikeReleaseListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_my_rides);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d(TAG, "onCreate called");

        txt_Bicycle = findViewById(R.id.TXT_Bicycle);
        txt_Location = findViewById(R.id.TXT_Location);
        txt_Plan = findViewById(R.id.TXT_Plan);
        txt_Amount = findViewById(R.id.TXT_Amount);
        txt_Date = findViewById(R.id.TXT_Date);
        txt_Paid = findViewById(R.id.TXT_Paid);
        txt_Timer = findViewById(R.id.TXT_Timer);
        txt_wait = findViewById(R.id.TXT_Wait);
        btn_Cancel = findViewById(R.id.BTN_Cancel);
        btn_Pay = findViewById(R.id.BTN_Pay);
        btn_Start = findViewById(R.id.BTN_Start);
        btn_End = findViewById(R.id.BTN_End);
        view_MyRide = findViewById(R.id.View_MyRide);
        view_NoRideData = findViewById(R.id.View_NoRideData);
        imgbtn_Back = findViewById(R.id.IMGBTN_Back);

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        realtimeDB = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            userReleaseBikeRef = realtimeDB.child("release_bicycle").child(uid);
            rideTimerRef = userReleaseBikeRef.child("rideTimer");
        }


        imgbtn_Back.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            startActivity(new Intent(MyRides.this, UserDashboard.class));
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "onBackPressed handled by callback");
                mainMenu();
            }
        });

        btn_Cancel.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked");
            showDeleteConfirmationDialog();
        });

        btn_Pay.setOnClickListener(v -> {
            Log.d(TAG, "Pay button clicked");
            Intent intent = new Intent(MyRides.this, PaymentGateway.class);
            FirebaseUser user = fAuth.getCurrentUser();
            if (user != null) {
                db.collection("RideHistory").document(user.getUid())
                        .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                        .limit(1).get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                                Log.d(TAG, "Payment: Found ride " + documentSnapshot.getId() + " with amount " + documentSnapshot.getString("amount"));
                                intent.putExtra("documentId", documentSnapshot.getId());
                                intent.putExtra("amount", documentSnapshot.getString("amount"));
                                startActivity(intent);
                            } else {
                                Log.w(TAG, "Payment: No ride found to pay for.");
                                Toast.makeText(MyRides.this, "No ride found to pay for.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Payment: Error fetching ride details: " + e.getMessage(), e);
                            Toast.makeText(MyRides.this, "Error fetching ride details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Log.w(TAG, "Payment: Current user is null.");
            }
        });

        btn_Start.setOnClickListener(v -> {
            Log.d(TAG, "Start button clicked");
            showStartRideConfirmationDialog();
        });

        btn_End.setOnClickListener(v -> {
            Log.d(TAG, "End Ride button clicked");
            showEndRideConfirmationDialog();
        });

        loadRideInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            if (userReleaseBikeRef == null) {
                userReleaseBikeRef = realtimeDB.child("release_bicycle").child(uid);
                rideTimerRef = userReleaseBikeRef.child("rideTimer");
            }
        }
        Log.d(TAG, "onResume called. isTimerRunning: " + isTimerRunning);
        loadRideInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called.");
        if (userReleaseBikeRef != null && bikeReleaseListener != null) {
            Log.d(TAG, "onPause: Removing bikeReleaseListener.");
            // Important: Remove listener only from the specific path it was added to.
            userReleaseBikeRef.child("bikeReleased").removeEventListener(bikeReleaseListener);
            // bikeReleaseListener = null; // Let it be re-assigned in checkBicycleRelease
        }
        if (isTimerRunning) {
            Log.d(TAG, "onPause: Timer was running, stopping local display updates.");
            timerHandler.removeCallbacksAndMessages(null);
        }
    }


    private void loadRideInfo() {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "loadRideInfo: Current user is null. Cannot load ride info.");
            view_MyRide.setVisibility(View.GONE);
            view_NoRideData.setVisibility(View.VISIBLE);
            btn_Pay.setVisibility(View.GONE);
            btn_Cancel.setVisibility(View.GONE);
            btn_Start.setVisibility(View.GONE);
            btn_End.setVisibility(View.GONE);
            txt_Paid.setVisibility(View.GONE);
            txt_Timer.setVisibility(View.GONE);
            txt_wait.setVisibility(View.GONE);
            if (isTimerRunning) startTimerDisplay(false);
            return;
        }

        String uid = user.getUid();
        if (userReleaseBikeRef == null) {
            userReleaseBikeRef = realtimeDB.child("release_bicycle").child(uid);
            rideTimerRef = userReleaseBikeRef.child("rideTimer");
        }
        Log.d(TAG, "loadRideInfo: Loading ride info for user " + uid);

        db.collection("RideHistory").document(uid)
                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "loadRideInfo: Firestore success. Snapshot empty: " + snapshot.isEmpty());
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        String firestoreRideDocId = doc.getId();
                        Log.d(TAG, "loadRideInfo: Firestore Document ID: " + firestoreRideDocId);
                        txt_Bicycle.setText(doc.getString("bikeType"));
                        txt_Location.setText(doc.getString("location"));
                        txt_Plan.setText(doc.getString("plan"));
                        txt_Amount.setText("LKR " + doc.getString("amount") + ".00");
                        txt_Date.setText(doc.getString("dateAndTime"));

                        String payStatus = doc.getString("payment");
                        String rideStatus = doc.getString("rideStatus"); // Get rideStatus from Firestore
                        Boolean rideStartRequestFirestore = doc.getBoolean("rideStartRequest");
                        Long elapsedTimeFirestore = doc.getLong("elapsedTime"); // Get final elapsedTime

                        Log.d(TAG, "loadRideInfo: payStatus=" + payStatus +
                                ", rideStatus=" + rideStatus +
                                ", rideStartRequestFirestore=" + rideStartRequestFirestore +
                                ", elapsedTimeFirestore=" + elapsedTimeFirestore);

                        // Reset UI elements
                        txt_Paid.setVisibility(View.GONE);
                        btn_Pay.setVisibility(View.GONE);
                        btn_Cancel.setVisibility(View.GONE);
                        btn_Start.setVisibility(View.GONE);
                        btn_End.setVisibility(View.GONE);
                        txt_Timer.setVisibility(View.GONE);
                        txt_wait.setVisibility(View.GONE);
                        if (isTimerRunning) startTimerDisplay(false); // Stop any local timer first


                        if ("Completed".equalsIgnoreCase(rideStatus)) {
                            Log.d(TAG, "loadRideInfo: Ride is COMPLETED.");
                            txt_Paid.setVisibility(View.VISIBLE); // Assume paid if completed
                            txt_Timer.setVisibility(View.VISIBLE);
                            if (elapsedTimeFirestore != null) {
                                txt_Timer.setText("Ride Duration: " + formatSecondsToDisplay(elapsedTimeFirestore));
                            } else {
                                txt_Timer.setText("Ride Completed (duration unavailable)");
                            }
                            // No buttons for a completed ride, except maybe "Cancel" if that logic remains for completed rides
                            // For now, only cancel button if payment is not "Paid"
                            if (!"Paid".equalsIgnoreCase(payStatus)) {
                                btn_Cancel.setVisibility(View.VISIBLE); // Or hide if cancel is not allowed for completed rides
                            }
                            view_MyRide.setVisibility(View.VISIBLE);
                            view_NoRideData.setVisibility(View.GONE);
                            return; // Early exit, no need to check RTDB for an already completed ride.
                        }


                        if ("Paid".equalsIgnoreCase(payStatus)) {
                            Log.d(TAG, "loadRideInfo: Ride is Paid (but not yet completed).");
                            txt_Paid.setVisibility(View.VISIBLE);

                            userReleaseBikeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot releaseSnapshot) {
                                    if (!releaseSnapshot.exists()){
                                        Log.w(TAG, "loadRideInfo: release_bicycle node does not exist in RTDB for user " + uid);
                                        // Ride paid, not completed, no RTDB node. Means start was not fully initiated or RTDB cleared.
                                        if (Boolean.TRUE.equals(rideStartRequestFirestore)) {
                                            Log.e(TAG, "Inconsistent state: Firestore rideStartRequest=true, but no RTDB release_bicycle node.");
                                            txt_wait.setText("Error: Ride status unclear. Please retry.");
                                            txt_wait.setVisibility(View.VISIBLE);
                                        } else {
                                            btn_Start.setVisibility(View.VISIBLE); // Show start button
                                        }
                                        return;
                                    }

                                    Boolean bikeReleasedFromRTDB = releaseSnapshot.child("bikeReleased").getValue(Boolean.class);
                                    DataSnapshot timerSnapshot = releaseSnapshot.child("rideTimer");
                                    String timerStatusFromRTDB = timerSnapshot.child("status").getValue(String.class);
                                    Long startTimeFromRTDB = timerSnapshot.child("startTimeMillis").getValue(Long.class);

                                    Log.d(TAG, "loadRideInfo (RTDB check): bikeReleased=" + bikeReleasedFromRTDB +
                                            ", timerStatus=" + timerStatusFromRTDB + ", startTime=" + startTimeFromRTDB);

                                    if (Boolean.TRUE.equals(bikeReleasedFromRTDB)) {
                                        Log.d(TAG, "loadRideInfo: Bike is RELEASED (from RTDB).");
                                        btn_End.setVisibility(View.VISIBLE);
                                        txt_Timer.setVisibility(View.VISIBLE);
                                        btn_Start.setVisibility(View.GONE);

                                        if ("running".equals(timerStatusFromRTDB) && startTimeFromRTDB != null && startTimeFromRTDB > 0) {
                                            rideStartTimeMillis = startTimeFromRTDB;
                                            if (!isTimerRunning) {
                                                Log.d(TAG, "loadRideInfo: Timer was not running locally. Starting display timer.");
                                                startTimerDisplay(true);
                                            } else {
                                                Log.d(TAG, "loadRideInfo: Timer already considered running locally. Restarting for consistency.");
                                                startTimerDisplay(true); // Restart to ensure it uses fresh startTime
                                            }
                                        } else if ("ended".equals(timerStatusFromRTDB) || "ended_fs_error".equals(timerStatusFromRTDB) || "ended_fs_missing".equals(timerStatusFromRTDB)) {
                                            Log.d(TAG, "loadRideInfo: Bike released, but timer is 'ended' in RTDB. This state should have been caught by 'Completed' rideStatus earlier.");
                                            // This case might mean Firestore 'rideStatus' hasn't updated yet or there's an inconsistency.
                                            // Force a reload which should pick up "Completed" status if Firestore updated.
                                            // For now, just show timer as ended and hide end button.
                                            txt_Timer.setText("Ride Ended (processing...)");
                                            btn_End.setVisibility(View.GONE);
                                            // Potentially call loadRideInfo() again after a short delay if expecting Firestore to catch up.
                                        } else {
                                            Log.w(TAG, "loadRideInfo: Bike released, but timer not 'running' or 'ended' or startTime invalid. RTDB state: " + timerStatusFromRTDB);
                                            // This is an ambiguous state. Could be an error during start.
                                            // Let's attach the listener to sync up.
                                            checkBicycleRelease();
                                        }
                                    } else if (Boolean.TRUE.equals(rideStartRequestFirestore)) {
                                        Log.d(TAG, "loadRideInfo: Ride start REQUESTED (Firestore), bike not yet released (RTDB).");
                                        txt_wait.setText("Bike release pending. Please wait.");
                                        txt_wait.setVisibility(View.VISIBLE);
                                        btn_Start.setVisibility(View.GONE);
                                        checkBicycleRelease(); // Start listening for RTDB changes
                                    } else {
                                        Log.d(TAG, "loadRideInfo: Ride Paid, but not started/released and no start request. Show START button.");
                                        btn_Start.setVisibility(View.VISIBLE);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "loadRideInfo: Error fetching release_bicycle data from RTDB", error.toException());
                                    Toast.makeText(MyRides.this, "Error checking ride status.", Toast.LENGTH_SHORT).show();
                                    btn_Start.setVisibility(View.VISIBLE); // Fallback: show start
                                }
                            });
                        } else { // Not Paid
                            Log.d(TAG, "loadRideInfo: Ride is NOT Paid.");
                            btn_Pay.setVisibility(View.VISIBLE);
                            btn_Cancel.setVisibility(View.VISIBLE);
                        }
                        view_MyRide.setVisibility(View.VISIBLE);
                        view_NoRideData.setVisibility(View.GONE);
                    } else {
                        Log.d(TAG, "loadRideInfo: No ride history found for user.");
                        view_MyRide.setVisibility(View.GONE);
                        view_NoRideData.setVisibility(View.VISIBLE);
                        // All buttons and text views remain hidden as per initial reset
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "loadRideInfo: Error loading ride info from Firestore", e);
                    view_MyRide.setVisibility(View.GONE);
                    view_NoRideData.setVisibility(View.VISIBLE);
                    Toast.makeText(MyRides.this, "Failed to load ride data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ride")
                .setMessage("Are you sure you want to cancel your last ride booking? This action cannot be undone if the ride has not started.")
                .setPositiveButton(R.string.yes, (d, w) -> {
                    Log.d(TAG, "showDeleteConfirmationDialog: Yes clicked.");
                    deleteLastRide();
                })
                .setNegativeButton(R.string.no, (d,w) -> Log.d(TAG, "showDeleteConfirmationDialog: No clicked."))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteLastRide() {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "deleteLastRide: User is null.");
            return;
        }
        String uid = user.getUid();
        Log.d(TAG, "deleteLastRide: Attempting to delete last ride for user " + uid);
        db.collection("RideHistory").document(uid)
                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        DocumentSnapshot rideDoc = snap.getDocuments().get(0);
                        String docId = rideDoc.getId();
                        String rideStatus = rideDoc.getString("rideStatus");
                        String paymentStatus = rideDoc.getString("payment");

                        // Add a condition: only allow deletion if ride is not "Completed" or if it's "Paid" but not started.
                        // For simplicity now, just checking if it's not "Paid"
                        if ("Paid".equalsIgnoreCase(paymentStatus) && !"Completed".equalsIgnoreCase(rideStatus)) {
                            // Or if rideStatus is null/empty, or not "Pending Payment" etc.
                            // Add more specific logic based on your ride lifecycle.
                            // For instance, you might not want to allow cancellation of an active ride here.
                            // This delete is more for "undoing" a booking.
                            Toast.makeText(this, "Cannot cancel a ride that is already paid or in progress. Contact support if needed.", Toast.LENGTH_LONG).show();
                            Log.w(TAG, "deleteLastRide: Attempted to cancel a ride that is already paid or was active. Ride ID: " + docId);
                            return;
                        }


                        Log.d(TAG, "deleteLastRide: Found ride " + docId + " to delete.");
                        rideDoc.getReference().delete()
                                .addOnSuccessListener(a -> {
                                    Log.d(TAG, "deleteLastRide: Ride " + docId + " deleted successfully from RideHistory.");
                                    Toast.makeText(this, "Last ride booking cancelled.", Toast.LENGTH_SHORT).show();

                                    // Also delete from AllHistory
                                    Long bookingTimestamp = rideDoc.getLong("bookingTimestamp");
                                    if (bookingTimestamp != null) {
                                        db.collection("AllHistory")
                                                .whereEqualTo("userId", uid)
                                                .whereEqualTo("bookingTimestamp", bookingTimestamp)
                                                .limit(1).get()
                                                .addOnSuccessListener(allHistorySnap -> {
                                                    if (!allHistorySnap.isEmpty()) {
                                                        allHistorySnap.getDocuments().get(0).getReference().delete()
                                                                .addOnSuccessListener(v -> Log.i(TAG, "Also deleted ride from AllHistory."))
                                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete ride from AllHistory.", e));
                                                    }
                                                }).addOnFailureListener(e -> Log.e(TAG, "Error finding ride in AllHistory for deletion.",e));
                                    }
                                    // Also clear relevant RTDB data if a start was requested
                                    if (userReleaseBikeRef != null) {
                                        userReleaseBikeRef.removeValue() // Removes the entire release_bicycle/userId node
                                                .addOnSuccessListener(unused -> Log.i(TAG, "Cleared release_bicycle RTDB node for user after ride cancellation."))
                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to clear release_bicycle RTDB node.", e));
                                    }

                                    loadRideInfo(); // Refresh UI
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "deleteLastRide: Failed to delete ride " + docId, e);
                                    Toast.makeText(this, "Failed to cancel ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.w(TAG, "deleteLastRide: No ride history to cancel for user " + uid);
                        Toast.makeText(this, "No ride history to cancel.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteLastRide: Error finding ride to cancel for user " + uid, e);
                    Toast.makeText(this, "Error finding ride to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showStartRideConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Start Ride")
                .setMessage("Are you ready to start your ride?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Log.d(TAG, "showStartRideConfirmationDialog: Yes clicked.");
                    startRideAction();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> Log.d(TAG, "showStartRideConfirmationDialog: No clicked."))
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void startRideAction() {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "startRideAction: User is null.");
            return;
        }
        String uid = user.getUid();
        if (userReleaseBikeRef == null) {
            userReleaseBikeRef = realtimeDB.child("release_bicycle").child(uid);
            rideTimerRef = userReleaseBikeRef.child("rideTimer");
        }
        Log.d(TAG, "startRideAction: Attempting to start ride for user " + uid);

        db.collection("Users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String userFullName = userDoc.getString("Full Name");
                        if (userFullName == null) {
                            userFullName = "Unknown User"; // Provide a default
                            Log.w(TAG, "User fullName not found in Firestore. Using default.");
                        }
                        final String finalUserFullName = userFullName;

                        db.collection("RideHistory").document(uid)
                                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                                .limit(1).get()
                                .addOnSuccessListener(rideHistorySnapshot -> {
                                    if (!rideHistorySnapshot.isEmpty()) {
                                        DocumentSnapshot rideHistoryDoc = rideHistorySnapshot.getDocuments().get(0);
                                        String rideHistoryDocId = rideHistoryDoc.getId();
                                        Long bookingTimestamp = rideHistoryDoc.getLong("bookingTimestamp");

                                        Log.d(TAG, "startRideAction: Found RideHistory doc " + rideHistoryDocId + " to update.");

                                        Map<String, Object> rideHistoryUpdates = new HashMap<>();
                                        rideHistoryUpdates.put("rideStartRequest", true);
                                        rideHistoryUpdates.put("bikeReleased", false); // Default to false, server/admin will set true
                                        rideHistoryUpdates.put("elapsedTime", 0L); // Reset elapsedTime
                                        rideHistoryUpdates.put("rideStatus", "Active"); // Or "Pending Release"

                                        db.collection("RideHistory").document(uid)
                                                .collection("rides").document(rideHistoryDocId)
                                                .update(rideHistoryUpdates)
                                                .addOnSuccessListener(a -> {
                                                    Log.i(TAG, "startRideAction: RideHistory updated for doc " + rideHistoryDocId);

                                                    if (bookingTimestamp != null) {
                                                        Map<String, Object> allHistoryUpdates = new HashMap<>();
                                                        allHistoryUpdates.put("rideStartRequest", true);
                                                        allHistoryUpdates.put("rideStatus", "Active"); // Or "Pending Release"

                                                        db.collection("AllHistory")
                                                                .whereEqualTo("userId", uid)
                                                                .whereEqualTo("bookingTimestamp", bookingTimestamp)
                                                                .limit(1)
                                                                .get()
                                                                .addOnSuccessListener(allHistorySnapshot -> {
                                                                    if (!allHistorySnapshot.isEmpty()) {
                                                                        String allHistoryDocId = allHistorySnapshot.getDocuments().get(0).getId();
                                                                        db.collection("AllHistory").document(allHistoryDocId)
                                                                                .update(allHistoryUpdates)
                                                                                .addOnSuccessListener(aVoid -> Log.i(TAG, "startRideAction: AllHistory updated for doc " + allHistoryDocId))
                                                                                .addOnFailureListener(e -> Log.e(TAG, "startRideAction: Failed to update AllHistory for doc " + allHistoryDocId, e));
                                                                    } else {
                                                                        Log.w(TAG, "startRideAction: Could not find matching document in AllHistory. UserID: " + uid + ", BookingTimestamp: " + bookingTimestamp);
                                                                    }
                                                                })
                                                                .addOnFailureListener(e -> Log.e(TAG, "startRideAction: Error querying AllHistory", e));
                                                    } else {
                                                        Log.w(TAG, "startRideAction: bookingTimestamp is null in RideHistory doc (" + rideHistoryDocId + ").");
                                                    }

                                                    Map<String, Object> releaseData = new HashMap<>();
                                                    releaseData.put("rideStartRequest", true);
                                                    releaseData.put("bikeReleased", false);
                                                    releaseData.put("fullName", finalUserFullName);

                                                    Map<String, Object> initialTimerData = new HashMap<>();
                                                    initialTimerData.put("status", "pending_release");
                                                    initialTimerData.put("startTimeMillis", 0L);
                                                    releaseData.put("rideTimer", initialTimerData);

                                                    userReleaseBikeRef.setValue(releaseData)
                                                            .addOnSuccessListener(unused -> {
                                                                Log.i(TAG, "startRideAction: Realtime DB updated for user " + uid);
                                                                btn_Start.setVisibility(View.GONE);
                                                                txt_wait.setText("Bike release pending. Please wait.");
                                                                txt_wait.setVisibility(View.VISIBLE);
                                                                txt_Timer.setVisibility(View.GONE); // Hide timer until bike is released
                                                                checkBicycleRelease();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Log.e(TAG, "startRideAction: Failed to update Realtime DB for user " + uid, e);
                                                                Toast.makeText(MyRides.this, "Failed to communicate ride start. Try again.", Toast.LENGTH_SHORT).show();
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "startRideAction: Failed to update RideHistory for doc " + rideHistoryDocId, e);
                                                    Toast.makeText(MyRides.this, "Failed to start ride. Please try again.", Toast.LENGTH_SHORT).show();
                                                });
                                    } else {
                                        Log.w(TAG, "startRideAction: No ride found in RideHistory to start for user " + uid);
                                        Toast.makeText(MyRides.this, "No ride found to start.", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "startRideAction: Failed to get RideHistory for user " + uid, e);
                                    Toast.makeText(MyRides.this, "Error starting ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.w(TAG, "startRideAction: User document not found in Firestore.");
                        Toast.makeText(MyRides.this, "User profile not found, cannot start ride.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "startRideAction: Failed to fetch user fullName from Firestore.", e);
                    Toast.makeText(MyRides.this, "Error fetching user details for ride start.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkBicycleRelease() {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null || userReleaseBikeRef == null) {
            Log.w(TAG, "checkBicycleRelease: User or userReleaseBikeRef is null.");
            return;
        }
        final String uid = user.getUid();
        Log.d(TAG, "checkBicycleRelease: Setting up listener for bikeReleased for user " + uid);

        if (bikeReleaseListener != null) {
            userReleaseBikeRef.child("bikeReleased").removeEventListener(bikeReleaseListener);
            Log.d(TAG, "checkBicycleRelease: Removed existing bikeReleaseListener.");
        }
        DatabaseReference bikeReleasedStatusRef = userReleaseBikeRef.child("bikeReleased");

        bikeReleaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean released = snapshot.getValue(Boolean.class);
                Log.d(TAG, "checkBicycleRelease - onDataChange: bikeReleased from RTDB = " + released);

                txt_wait.setVisibility(View.GONE);

                if (Boolean.TRUE.equals(released)) {
                    Log.d(TAG, "checkBicycleRelease - onDataChange: Bike IS RELEASED.");
                    txt_Paid.setVisibility(View.VISIBLE);
                    btn_End.setVisibility(View.VISIBLE);
                    btn_Start.setVisibility(View.GONE);
                    txt_Timer.setVisibility(View.VISIBLE);

                    rideTimerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot timerNodeSnapshot) {
                            Long existingStartTime = timerNodeSnapshot.child("startTimeMillis").getValue(Long.class);
                            String currentStatus = timerNodeSnapshot.child("status").getValue(String.class);

                            if (!"running".equals(currentStatus) || existingStartTime == null || existingStartTime == 0L) {
                                Log.d(TAG, "Bike released, startTimeMillis not set or status not 'running'. Setting it now.");
                                Map<String, Object> timerUpdate = new HashMap<>();
                                timerUpdate.put("startTimeMillis", ServerValue.TIMESTAMP);
                                timerUpdate.put("status", "running");
                                rideTimerRef.updateChildren(timerUpdate)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Set startTimeMillis and status=running in RTDB.");
                                            rideTimerRef.child("startTimeMillis").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot newStartTimeSnap) {
                                                    rideStartTimeMillis = Objects.requireNonNullElse(newStartTimeSnap.getValue(Long.class), System.currentTimeMillis());
                                                    if (!isTimerRunning) startTimerDisplay(true);
                                                    Toast.makeText(MyRides.this, "Ride Started!", Toast.LENGTH_SHORT).show();
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Log.e(TAG, "Failed to fetch ServerValue.TIMESTAMP after setting.", error.toException());
                                                    rideStartTimeMillis = System.currentTimeMillis();
                                                    if (!isTimerRunning) startTimerDisplay(true);
                                                    Toast.makeText(MyRides.this, "Ride Started (local time).", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to set startTimeMillis in RTDB", e);
                                            rideStartTimeMillis = System.currentTimeMillis();
                                            if (!isTimerRunning) startTimerDisplay(true);
                                            Toast.makeText(MyRides.this, "Ride Started (error setting start time).", Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Log.d(TAG, "Bike released, startTimeMillis already exists in RTDB: " + existingStartTime + ", status: " + currentStatus);
                                rideStartTimeMillis = existingStartTime;
                                if (!isTimerRunning) startTimerDisplay(true);
                                else startTimerDisplay(true);
                                Toast.makeText(MyRides.this, "Ride Resumed/Started.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error fetching rideTimer node on bike release", error.toException());
                            rideStartTimeMillis = System.currentTimeMillis();
                            if (!isTimerRunning) startTimerDisplay(true);
                            Toast.makeText(MyRides.this, "Ride Started (error checking timer state).", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.d(TAG, "checkBicycleRelease - onDataChange: Bike IS NOT RELEASED (or value is null).");
                    db.collection("RideHistory").document(uid)
                            .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                            .limit(1).get().addOnSuccessListener(rideSnap -> {
                                if (!rideSnap.isEmpty()) {
                                    Boolean rideStartRequestedFirestore = rideSnap.getDocuments().get(0).getBoolean("rideStartRequest");
                                    String rideStatusFirestore = rideSnap.getDocuments().get(0).getString("rideStatus");

                                    if ("Completed".equalsIgnoreCase(rideStatusFirestore)) {
                                        // If Firestore says completed, then we shouldn't be in this listener path ideally
                                        // but if we are, just load info to show completed state.
                                        Log.d(TAG, "checkBicycleRelease: Bike not released, but Firestore says ride is Completed. Reloading.");
                                        if (isTimerRunning) startTimerDisplay(false);
                                        loadRideInfo(); // Reload to show completed state
                                        return;
                                    }

                                    if (Boolean.TRUE.equals(rideStartRequestedFirestore)) {
                                        txt_wait.setText("Bike release pending. Please wait.");
                                        txt_wait.setVisibility(View.VISIBLE);
                                        txt_Paid.setVisibility(View.VISIBLE);
                                        btn_End.setVisibility(View.GONE);
                                        txt_Timer.setVisibility(View.GONE);
                                        btn_Start.setVisibility(View.GONE);
                                        if (isTimerRunning) startTimerDisplay(false);
                                    } else {
                                        if (isTimerRunning) startTimerDisplay(false);
                                        loadRideInfo(); // Reload to show appropriate state (e.g. Start button if eligible)
                                    }
                                } else {
                                    if (isTimerRunning) startTimerDisplay(false);
                                    loadRideInfo();
                                }
                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "checkBicycleRelease - onDataChange (bike not released): Error fetching ride doc from Firestore.", e);
                                if (isTimerRunning) startTimerDisplay(false);
                                loadRideInfo();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "checkBicycleRelease - onCancelled: Firebase Realtime DB error: " + error.getMessage(), error.toException());
                txt_wait.setText("Error checking bike status.");
                txt_wait.setVisibility(View.VISIBLE);
                btn_End.setVisibility(View.GONE);
                txt_Timer.setVisibility(View.GONE);
                btn_Start.setVisibility(View.GONE);
                if (isTimerRunning) startTimerDisplay(false);
            }
        };
        bikeReleasedStatusRef.addValueEventListener(bikeReleaseListener);
    }

    private void mainMenu() {
        Log.d(TAG, "mainMenu called. Navigating to UserDashboard.");
        startActivity(new Intent(this, UserDashboard.class));
        finish();
    }

    private void startTimerDisplay(boolean shouldRun) {
        timerHandler.removeCallbacksAndMessages(null);
        isTimerRunning = shouldRun;

        Log.d(TAG, "startTimerDisplay() called. shouldRun = " + shouldRun + ", isTimerRunning = " + isTimerRunning + ", rideStartTimeMillis = " + rideStartTimeMillis);

        if (isTimerRunning && rideStartTimeMillis > 0) {
            if (txt_Timer.getVisibility() != View.VISIBLE) {
                Log.d(TAG, "startTimerDisplay: txt_Timer was not visible, making it visible now.");
                txt_Timer.setVisibility(View.VISIBLE);
            }

            Runnable uiTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isTimerRunning || rideStartTimeMillis == 0) {
                        Log.d(TAG, "UI Timer runnable: conditions not met, stopping. isTimerRunning=" + isTimerRunning + ", rideStartTimeMillis=" + rideStartTimeMillis);
                        timerHandler.removeCallbacksAndMessages(null);
                        return;
                    }

                    long currentElapsedTimeMillis = System.currentTimeMillis() - rideStartTimeMillis;
                    if (currentElapsedTimeMillis < 0) currentElapsedTimeMillis = 0;

                    int totalElapsedSeconds = (int) (currentElapsedTimeMillis / 1000);

                    int days = totalElapsedSeconds / 86400;
                    int hours = (totalElapsedSeconds % 86400) / 3600;
                    int minutes = (totalElapsedSeconds % 3600) / 60;
                    int seconds = totalElapsedSeconds % 60;
                    String formattedTime = String.format(Locale.getDefault(), "%02d day %02d hrs %02d min %02d sec", days, hours, minutes, seconds);
                    txt_Timer.setText("Ride Time: " + formattedTime);

                    timerHandler.postDelayed(this, 1000);
                }
            };
            timerHandler.post(uiTimerRunnable);
        } else {
            Log.d(TAG, "startTimerDisplay: Timer explicitly stopped or rideStartTimeMillis invalid. isTimerRunning=" + isTimerRunning + ", rideStartTimeMillis=" + rideStartTimeMillis);
            isTimerRunning = false;
            // txt_Timer.setText("Ride Time: 00 day 00 hrs 00 min 00 sec"); // Keep this if you want to show 0 when stopped
            // txt_Timer.setVisibility(View.GONE); // Or hide it
            if (shouldRun && rideStartTimeMillis == 0) { // Specific case: tried to start but no valid time
                Log.e(TAG, "startTimerDisplay: Attempted to start timer display but rideStartTimeMillis is 0!");
                txt_Timer.setVisibility(View.GONE);
            } else if (!shouldRun) { // Explicitly stopping
                // If not running, and not trying to start with 0 time, let loadRideInfo decide visibility/text for completed rides.
                // txt_Timer.setText("Ride Time: 00 day 00 hrs 00 min 00 sec");
                // txt_Timer.setVisibility(View.GONE); // Let loadRideInfo handle visibility based on rideStatus
            }
        }
    }

    private void showEndRideConfirmationDialog() {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null || rideStartTimeMillis == 0 || !isTimerRunning) {
            Toast.makeText(this, "No active ride to end.", Toast.LENGTH_SHORT).show();
            Log.w(TAG,"showEndRideConfirmationDialog: No active ride criteria met. User: " + (user!=null) + ", startTime: " +rideStartTimeMillis + ", isTimerRunning: " + isTimerRunning);
            loadRideInfo();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("End Ride")
                .setMessage("Are you sure you want to end your current ride?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Log.d(TAG, "showEndRideConfirmationDialog: Yes clicked.");
                    endRideAction();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> Log.d(TAG, "showEndRideConfirmationDialog: No clicked."))
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void endRideAction() {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null || rideStartTimeMillis == 0 || userReleaseBikeRef == null || rideTimerRef == null) {
            Log.w(TAG, "endRideAction: Pre-conditions not met (user, startTime, or RTDB refs null).");
            Toast.makeText(this, "Cannot end ride. Session error.", Toast.LENGTH_SHORT).show();
            return;
        }
        final String uid = user.getUid();

        startTimerDisplay(false);

        long rideEndTimeMillis = System.currentTimeMillis();
        long finalElapsedTimeSeconds = (rideEndTimeMillis - rideStartTimeMillis) / 1000;
        if (finalElapsedTimeSeconds < 0) finalElapsedTimeSeconds = 0;

        Log.d(TAG, "endRideAction: Ride ended. StartTime(ms): " + rideStartTimeMillis +
                ", EndTime(ms): " + rideEndTimeMillis + ", Duration (sec): " + finalElapsedTimeSeconds);

        final long finalDurationForFirestore = finalElapsedTimeSeconds;
        db.collection("RideHistory").document(uid)
                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot rideDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String rideDocId = rideDoc.getId();

                        Map<String, Object> rideUpdates = new HashMap<>();
                        rideUpdates.put("elapsedTime", finalDurationForFirestore);
                        rideUpdates.put("rideStatus", "Completed"); // CRITICAL: Set status to Completed
                        rideUpdates.put("rideEndTime", rideEndTimeMillis);

                        rideDoc.getReference().update(rideUpdates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.i(TAG, "endRideAction: RideHistory/" + rideDocId + " updated in Firestore with final duration: " + finalDurationForFirestore + " and status: Completed");

                                    Long bookingTimestamp = rideDoc.getLong("bookingTimestamp");
                                    if (bookingTimestamp != null) {
                                        Map<String, Object> allHistoryUpdates = new HashMap<>();
                                        allHistoryUpdates.put("elapsedTime", finalDurationForFirestore);
                                        allHistoryUpdates.put("rideStatus", "Completed");
                                        allHistoryUpdates.put("rideEndTime", rideEndTimeMillis);
                                        db.collection("AllHistory")
                                                .whereEqualTo("userId", uid)
                                                .whereEqualTo("bookingTimestamp", bookingTimestamp)
                                                .limit(1).get()
                                                .addOnSuccessListener(allHistorySnap -> {
                                                    if(!allHistorySnap.isEmpty()){
                                                        allHistorySnap.getDocuments().get(0).getReference().update(allHistoryUpdates)
                                                                .addOnSuccessListener(v -> Log.i(TAG, "AllHistory also updated for ride end."))
                                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update AllHistory for ride end.",e));
                                                    }
                                                }).addOnFailureListener(e -> Log.e(TAG, "Error finding AllHistory doc for ride end update.",e));
                                    }

                                    Map<String, Object> timerEndUpdate = new HashMap<>();
                                    timerEndUpdate.put("status", "ended");
                                    rideTimerRef.updateChildren(timerEndUpdate)
                                            .addOnSuccessListener(unused -> Log.i(TAG, "endRideAction: rideTimer status set to 'ended' in RTDB."))
                                            .addOnFailureListener(e -> Log.e(TAG, "endRideAction: Failed to update rideTimer status in RTDB.", e));

                                    userReleaseBikeRef.child("bikeReleased").setValue(false);
                                    userReleaseBikeRef.child("rideStartRequest").setValue(false);

                                    Toast.makeText(MyRides.this, "Ride Ended. Duration: " + formatSecondsToDisplay(finalDurationForFirestore), Toast.LENGTH_LONG).show();
                                    rideStartTimeMillis = 0;
                                    loadRideInfo(); // Reload to display completed state
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "endRideAction: Failed to update RideHistory in Firestore.", e);
                                    Toast.makeText(MyRides.this, "Error finalizing ride. Please check history.", Toast.LENGTH_SHORT).show();
                                    rideTimerRef.child("status").setValue("ended_fs_error");
                                    userReleaseBikeRef.child("bikeReleased").setValue(false);
                                    // Still try to load info, it might pick up partial updates or show an error state from RTDB
                                    loadRideInfo();
                                });
                    } else {
                        Log.w(TAG, "endRideAction: No active ride document found in Firestore to update.");
                        Toast.makeText(MyRides.this, "Could not find ride to finalize.", Toast.LENGTH_SHORT).show();
                        rideTimerRef.child("status").setValue("ended_fs_missing");
                        userReleaseBikeRef.child("bikeReleased").setValue(false);
                        loadRideInfo();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "endRideAction: Error fetching ride document from Firestore.", e);
                    Toast.makeText(MyRides.this, "Error accessing ride data to end.", Toast.LENGTH_SHORT).show();
                    // Attempt to load info to reset UI even on failure
                    loadRideInfo();
                });
    }

    private String formatSecondsToDisplay(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        int days = (int) (totalSeconds / 86400);
        int hours = (int) ((totalSeconds % 86400) / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        int seconds = (int) (totalSeconds % 60);
        if (days > 0) {
            return String.format(Locale.getDefault(), "%d days %02d hrs %02d mins %02d sec", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d hrs %02d mins %02d sec", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%02d mins %02d sec", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d sec", seconds);
        }
    }
}