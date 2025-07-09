package com.example.padelgo.user;

import android.annotation.SuppressLint;
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
import java.util.concurrent.TimeUnit;

public class MyRides extends AppCompatActivity {
    TextView txt_Bicycle, txt_Location, txt_Plan, txt_Amount, txt_Date, txt_Paid, txt_Timer, txt_wait,txt_ExtraTimer,txt_ExtraCharge;
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
        txt_ExtraTimer = findViewById(R.id.TXT_ExtraTimer);
        txt_ExtraCharge = findViewById(R.id.TXT_ExtraCharge);

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
            userReleaseBikeRef.child("bikeReleased").removeEventListener(bikeReleaseListener);
        }
        if (isTimerRunning) {
            Log.d(TAG, "onPause: Timer was running, stopping local display updates.");
            timerHandler.removeCallbacksAndMessages(null);
        }
    }

    @SuppressLint("SetTextI18n")
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
            txt_ExtraTimer.setVisibility(View.GONE);
            if (isTimerRunning) startTimerDisplay(false, null);
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
                        final String planString = doc.getString("plan");
                        txt_Plan.setText(planString);
                        txt_Amount.setText("LKR " + doc.getString("amount") + ".00");
                        txt_Date.setText(doc.getString("dateAndTime"));

                        String payStatus = doc.getString("payment");
                        String rideStatus = doc.getString("rideStatus");
                        Boolean rideStartRequestFirestore = doc.getBoolean("rideStartRequest");
                        Long elapsedTimeFirestore = doc.getLong("elapsedTime");
                        Long extraTimeFirestore = doc.getLong("extraTime");

                        Log.d(TAG, "loadRideInfo: payStatus=" + payStatus +
                                ", rideStatus=" + rideStatus +
                                ", rideStartRequestFirestore=" + rideStartRequestFirestore +
                                ", elapsedTimeFirestore=" + elapsedTimeFirestore +
                                ", extraTimeFirestore=" + extraTimeFirestore);

                        txt_Paid.setVisibility(View.GONE);
                        btn_Pay.setVisibility(View.GONE);
                        btn_Cancel.setVisibility(View.GONE);
                        btn_Start.setVisibility(View.GONE);
                        btn_End.setVisibility(View.GONE);
                        txt_Timer.setVisibility(View.GONE);
                        txt_wait.setVisibility(View.GONE);
                        txt_ExtraTimer.setVisibility(View.GONE);
                        if (isTimerRunning) startTimerDisplay(false, null);

                        if ("Completed".equalsIgnoreCase(rideStatus)) {
                            Log.d(TAG, "loadRideInfo: Ride is COMPLETED.");
                            txt_Paid.setVisibility(View.VISIBLE);
                            txt_Timer.setVisibility(View.VISIBLE);
                            if (elapsedTimeFirestore != null) {
                                txt_Timer.setText("Ride Duration: " + formatSecondsToDisplay(elapsedTimeFirestore));
                                if (extraTimeFirestore != null && extraTimeFirestore > 0) {
                                    txt_ExtraTimer.setText("Extra Time: " + formatSecondsToDisplay(extraTimeFirestore));
                                    txt_ExtraTimer.setVisibility(View.VISIBLE);
                                        txt_Paid.setVisibility(View.GONE);
                                        txt_ExtraCharge.setVisibility(View.VISIBLE);
                                }
                            } else {
                                txt_Timer.setText("Ride Completed (duration unavailable)");
                            }
                            if (!"Paid".equalsIgnoreCase(payStatus)) {
                                btn_Cancel.setVisibility(View.VISIBLE);
                            }
                            view_MyRide.setVisibility(View.VISIBLE);
                            view_NoRideData.setVisibility(View.GONE);
                            return;
                        }

                        if ("Paid".equalsIgnoreCase(payStatus)) {
                            Log.d(TAG, "loadRideInfo: Ride is Paid (but not yet completed).");
                            txt_Paid.setVisibility(View.VISIBLE);
                            userReleaseBikeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot releaseSnapshot) {
                                    if (!releaseSnapshot.exists()){
                                        Log.w(TAG, "loadRideInfo: release_bicycle node does not exist in RTDB for user " + uid);
                                        if (Boolean.TRUE.equals(rideStartRequestFirestore)) {
                                            Log.e(TAG, "Inconsistent state: Firestore rideStartRequest=true, but no RTDB release_bicycle node.");
                                            txt_wait.setText("Error: Ride status unclear. Please retry.");
                                            txt_wait.setVisibility(View.VISIBLE);
                                        } else {
                                            btn_Start.setVisibility(View.VISIBLE);
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
                                                startTimerDisplay(true, planString);
                                            } else {
                                                Log.d(TAG, "loadRideInfo: Timer already considered running locally. Restarting for consistency.");
                                                startTimerDisplay(true, planString);
                                            }
                                        } else if ("ended".equals(timerStatusFromRTDB) || "ended_fs_error".equals(timerStatusFromRTDB) || "ended_fs_missing".equals(timerStatusFromRTDB)) {
                                            Log.d(TAG, "loadRideInfo: Bike released, but timer is 'ended' in RTDB. This state should have been caught by 'Completed' rideStatus earlier.");
                                            txt_Timer.setText("Ride Ended (processing...)");
                                            btn_End.setVisibility(View.GONE);
                                        } else {
                                            Log.w(TAG, "loadRideInfo: Bike released, but timer not 'running' or 'ended' or startTime invalid. RTDB state: " + timerStatusFromRTDB + ". Checking release.");
                                            checkBicycleRelease();
                                        }
                                    } else if (Boolean.TRUE.equals(rideStartRequestFirestore)) {
                                        Log.d(TAG, "loadRideInfo: Ride start REQUESTED (Firestore), bike not yet released (RTDB).");
                                        txt_wait.setText("Bike release pending. Please wait.");
                                        txt_wait.setVisibility(View.VISIBLE);
                                        btn_Start.setVisibility(View.GONE);
                                        checkBicycleRelease();
                                    } else {
                                        Log.d(TAG, "loadRideInfo: Ride Paid, but not started/released and no start request. Show START button.");
                                        btn_Start.setVisibility(View.VISIBLE);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "loadRideInfo: Error fetching release_bicycle data from RTDB", error.toException());
                                    Toast.makeText(MyRides.this, "Error checking ride status.", Toast.LENGTH_SHORT).show();
                                    btn_Start.setVisibility(View.VISIBLE);
                                }
                            });
                        } else {
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

                        if ("Paid".equalsIgnoreCase(paymentStatus) && !"Completed".equalsIgnoreCase(rideStatus)) {
                            Toast.makeText(this, "Cannot cancel a ride that is already paid or in progress. Contact support if needed.", Toast.LENGTH_LONG).show();
                            Log.w(TAG, "deleteLastRide: Attempted to cancel a ride that is already paid or was active. Ride ID: " + docId);
                            return;
                        }
                        Log.d(TAG, "deleteLastRide: Found ride " + docId + " to delete.");
                        rideDoc.getReference().delete()
                                .addOnSuccessListener(a -> {
                                    Log.d(TAG, "deleteLastRide: Ride " + docId + " deleted successfully from RideHistory.");
                                    Toast.makeText(this, "Last ride booking cancelled.", Toast.LENGTH_SHORT).show();

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
                                    if (userReleaseBikeRef != null) {
                                        userReleaseBikeRef.removeValue()
                                                .addOnSuccessListener(unused -> Log.i(TAG, "Cleared release_bicycle RTDB node for user after ride cancellation."))
                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to clear release_bicycle RTDB node.", e));
                                    }
                                    loadRideInfo();
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
                            userFullName = "Unknown User";
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
                                        final String planString = rideHistoryDoc.getString("plan");
                                        Log.d(TAG, "startRideAction: Found RideHistory doc " + rideHistoryDocId + " to update.");

                                        Map<String, Object> rideHistoryUpdates = new HashMap<>();
                                        rideHistoryUpdates.put("rideStartRequest", true);
                                        rideHistoryUpdates.put("bikeReleased", false);
                                        rideHistoryUpdates.put("elapsedTime", 0L);
                                        rideHistoryUpdates.put("extraTime", 0L);
                                        rideHistoryUpdates.put("rideStatus", "Active");

                                        db.collection("RideHistory").document(uid)
                                                .collection("rides").document(rideHistoryDocId)
                                                .update(rideHistoryUpdates)
                                                .addOnSuccessListener(a -> {
                                                    Log.i(TAG, "startRideAction: RideHistory updated for doc " + rideHistoryDocId);

                                                    if (bookingTimestamp != null) {
                                                        Map<String, Object> allHistoryUpdates = new HashMap<>();
                                                        allHistoryUpdates.put("rideStartRequest", true);
                                                        allHistoryUpdates.put("rideStatus", "Active");
                                                        allHistoryUpdates.put("extraTime", 0L); // Initialize extra time

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
                                                    // Update Realtime Database
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
                                                                txt_Timer.setVisibility(View.GONE);
                                                                txt_ExtraTimer.setVisibility(View.GONE);
                                                                checkBicycleRelease(); // Start listening for bike release
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
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean released = snapshot.getValue(Boolean.class);
                Log.d(TAG, "checkBicycleRelease - onDataChange: bikeReleased from RTDB = " + released);

                txt_wait.setVisibility(View.GONE);

                if (Boolean.TRUE.equals(released)) {
                    Log.d(TAG, "checkBicycleRelease - onDataChange: Bike IS RELEASED.");
                    txt_Paid.setText("Ride Started 🚴");
                    txt_Paid.setVisibility(View.VISIBLE);
                    btn_End.setVisibility(View.VISIBLE);
                    btn_Start.setVisibility(View.GONE);
                    txt_Timer.setVisibility(View.VISIBLE);

                    db.collection("RideHistory").document(uid)
                            .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                            .limit(1).get()
                            .addOnSuccessListener(rideSnapshot -> {
                                if (!rideSnapshot.isEmpty()) {
                                    final String planString = rideSnapshot.getDocuments().get(0).getString("plan");

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
                                                                    if (!isTimerRunning) startTimerDisplay(true, planString);
                                                                    Toast.makeText(MyRides.this, "Ride Started!", Toast.LENGTH_SHORT).show();
                                                                }
                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError error) {
                                                                    Log.e(TAG, "Failed to fetch ServerValue.TIMESTAMP after setting.", error.toException());
                                                                    rideStartTimeMillis = System.currentTimeMillis(); // Fallback to client time
                                                                    if (!isTimerRunning) startTimerDisplay(true, planString);
                                                                    Toast.makeText(MyRides.this, "Ride Started (local time).", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "Failed to set startTimeMillis in RTDB", e);
                                                            rideStartTimeMillis = System.currentTimeMillis(); // Fallback
                                                            if (!isTimerRunning) startTimerDisplay(true, planString);
                                                            Toast.makeText(MyRides.this, "Ride Started (error setting start time).", Toast.LENGTH_SHORT).show();
                                                        });
                                            } else {
                                                Log.d(TAG, "Bike released, startTimeMillis already exists in RTDB: " + existingStartTime + ", status: " + currentStatus);
                                                rideStartTimeMillis = existingStartTime;
                                                if (!isTimerRunning) startTimerDisplay(true, planString);
                                                else startTimerDisplay(true, planString); // Restart to ensure it's using the correct plan
                                                Toast.makeText(MyRides.this, "Ride Resumed/Started.", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e(TAG, "Error fetching rideTimer node on bike release", error.toException());
                                            rideStartTimeMillis = System.currentTimeMillis(); // Fallback
                                            if (!isTimerRunning) startTimerDisplay(true, planString);
                                            Toast.makeText(MyRides.this, "Ride Started (error checking timer state).", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    Log.w(TAG, "checkBicycleRelease: RideHistory document not found when trying to get plan for timer.");
                                    rideTimerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot timerNodeSnapshot) {
                                            Long existingStartTime = timerNodeSnapshot.child("startTimeMillis").getValue(Long.class);
                                            String currentStatus = timerNodeSnapshot.child("status").getValue(String.class);
                                            if (!"running".equals(currentStatus) || existingStartTime == null || existingStartTime == 0L) {
                                                Map<String, Object> timerUpdate = new HashMap<>();
                                                timerUpdate.put("startTimeMillis", ServerValue.TIMESTAMP);
                                                timerUpdate.put("status", "running");
                                                rideTimerRef.updateChildren(timerUpdate).addOnSuccessListener(aVoid ->
                                                        rideTimerRef.child("startTimeMillis").addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot newStartTimeSnap) {
                                                                rideStartTimeMillis = Objects.requireNonNullElse(newStartTimeSnap.getValue(Long.class), System.currentTimeMillis());
                                                                if (!isTimerRunning) startTimerDisplay(true, null);
                                                                Toast.makeText(MyRides.this, "Ride Started!", Toast.LENGTH_SHORT).show();
                                                            }
                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                                rideStartTimeMillis = System.currentTimeMillis();
                                                                if (!isTimerRunning) startTimerDisplay(true, null);
                                                                Toast.makeText(MyRides.this, "Ride Started (local time).", Toast.LENGTH_SHORT).show();
                                                            }
                                                        }));
                                            } else {
                                                rideStartTimeMillis = existingStartTime;
                                                if (!isTimerRunning) startTimerDisplay(true, null);
                                                else startTimerDisplay(true, null);
                                                Toast.makeText(MyRides.this, "Ride Resumed/Started.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            rideStartTimeMillis = System.currentTimeMillis();
                                            if (!isTimerRunning) startTimerDisplay(true, null);
                                            Toast.makeText(MyRides.this, "Ride Started (error timer state).", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "checkBicycleRelease: Failed to get RideHistory for planString.", e);
                                rideTimerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot timerNodeSnapshot) {
                                        Long existingStartTime = timerNodeSnapshot.child("startTimeMillis").getValue(Long.class);
                                        String currentStatus = timerNodeSnapshot.child("status").getValue(String.class);
                                        if (!"running".equals(currentStatus) || existingStartTime == null || existingStartTime == 0L) {
                                            Map<String, Object> timerUpdate = new HashMap<>();
                                            timerUpdate.put("startTimeMillis", ServerValue.TIMESTAMP);
                                            timerUpdate.put("status", "running");
                                            rideTimerRef.updateChildren(timerUpdate).addOnSuccessListener(aVoid ->
                                                    rideTimerRef.child("startTimeMillis").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot newStartTimeSnap) {
                                                            rideStartTimeMillis = Objects.requireNonNullElse(newStartTimeSnap.getValue(Long.class), System.currentTimeMillis());
                                                            if (!isTimerRunning) startTimerDisplay(true, null);
                                                            Toast.makeText(MyRides.this, "Ride Started!", Toast.LENGTH_SHORT).show();
                                                        }
                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                            rideStartTimeMillis = System.currentTimeMillis();
                                                            if (!isTimerRunning) startTimerDisplay(true, null);
                                                            Toast.makeText(MyRides.this, "Ride Started (local time).", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }));
                                        } else {
                                            rideStartTimeMillis = existingStartTime;
                                            if (!isTimerRunning) startTimerDisplay(true, null);
                                            else startTimerDisplay(true, null);
                                            Toast.makeText(MyRides.this, "Ride Resumed/Started.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        rideStartTimeMillis = System.currentTimeMillis();
                                        if (!isTimerRunning) startTimerDisplay(true, null);
                                        Toast.makeText(MyRides.this, "Ride Started (error timer state).", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });

                } else {
                    Log.d(TAG, "checkBicycleRelease - onDataChange: Bike IS NOT RELEASED (or value is null/false).");
                    db.collection("RideHistory").document(uid)
                            .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                            .limit(1).get().addOnSuccessListener(rideSnap -> {
                                if (!rideSnap.isEmpty()) {
                                    Boolean rideStartRequestedFirestore = rideSnap.getDocuments().get(0).getBoolean("rideStartRequest");
                                    String rideStatusFirestore = rideSnap.getDocuments().get(0).getString("rideStatus");

                                    if ("Completed".equalsIgnoreCase(rideStatusFirestore)) {
                                        Log.d(TAG, "checkBicycleRelease: Bike not released, but Firestore says ride is Completed. Reloading.");
                                        if (isTimerRunning) startTimerDisplay(false, null);
                                        loadRideInfo();
                                        return;
                                    }

                                    if (Boolean.TRUE.equals(rideStartRequestedFirestore)) {
                                        txt_wait.setText("Bike release pending. Please wait.");
                                        txt_wait.setVisibility(View.VISIBLE);
                                        txt_Paid.setVisibility(View.VISIBLE);
                                        btn_End.setVisibility(View.GONE);
                                        txt_Timer.setVisibility(View.GONE);
                                        txt_ExtraTimer.setVisibility(View.GONE);
                                        btn_Start.setVisibility(View.GONE);
                                        if (isTimerRunning) startTimerDisplay(false, null);
                                    } else {
                                        Log.d(TAG, "checkBicycleRelease: Bike not released, no start request in Firestore. Reloading info.");
                                        if (isTimerRunning) startTimerDisplay(false, null);
                                        loadRideInfo();
                                    }
                                } else {
                                    Log.w(TAG, "checkBicycleRelease: Bike not released, and no ride history found in Firestore.");
                                    if (isTimerRunning) startTimerDisplay(false, null);
                                    loadRideInfo();
                                }
                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "checkBicycleRelease - onDataChange (bike not released): Error fetching ride doc from Firestore.", e);
                                if (isTimerRunning) startTimerDisplay(false, null);
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
                txt_ExtraTimer.setVisibility(View.GONE);
                btn_Start.setVisibility(View.GONE); // Hide start as status is unknown
                if (isTimerRunning) startTimerDisplay(false, null);
            }
        };
        bikeReleasedStatusRef.addValueEventListener(bikeReleaseListener);
    }

    private void mainMenu() {
        Log.d(TAG, "mainMenu called. Navigating to UserDashboard.");
        startActivity(new Intent(this, UserDashboard.class));
        finish();
    }

    private void startTimerDisplay(boolean shouldRun, final String planString) {
        timerHandler.removeCallbacksAndMessages(null);
        isTimerRunning = shouldRun;

        Log.d(TAG, "startTimerDisplay() called. shouldRun = " + shouldRun + ", isTimerRunning = " + isTimerRunning + ", rideStartTimeMillis = " + rideStartTimeMillis + ", planString: " + planString);

        if (isTimerRunning && rideStartTimeMillis > 0) {
            if (txt_Timer.getVisibility() != View.VISIBLE) {
                Log.d(TAG, "startTimerDisplay: txt_Timer was not visible, making it visible now.");
                txt_Timer.setVisibility(View.VISIBLE);
            }
            txt_ExtraTimer.setVisibility(View.GONE);

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

                    long totalElapsedSeconds = currentElapsedTimeMillis / 1000;

                    long planDurationSeconds = 0;
                    if (planString != null && !planString.isEmpty()) {
                        try {
                            String[] parts = planString.toLowerCase().split(" ");
                            if (parts.length >= 2) {
                                int value = Integer.parseInt(parts[0]);
                                if (parts[1].startsWith("hour")) {
                                    planDurationSeconds = TimeUnit.HOURS.toSeconds(value);
                                } else if (parts[1].startsWith("min")) {
                                    planDurationSeconds = TimeUnit.MINUTES.toSeconds(value);
                                }
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing planString: " + planString, e);
                        }
                    }

                    long mainRideSeconds;
                    long extraTimeSeconds = 0;

                    if (planDurationSeconds > 0 && totalElapsedSeconds > planDurationSeconds) {
                        mainRideSeconds = planDurationSeconds;
                        extraTimeSeconds = totalElapsedSeconds - planDurationSeconds;
                        txt_ExtraTimer.setText("Extra Time: " + formatSecondsToDisplay(extraTimeSeconds));
                        txt_ExtraTimer.setVisibility(View.VISIBLE);
                    } else {
                        mainRideSeconds = totalElapsedSeconds;
                        txt_ExtraTimer.setVisibility(View.GONE);
                    }

                    txt_Timer.setText("Ride Time: " + formatSecondsToDisplay(mainRideSeconds));
                    timerHandler.postDelayed(this, 1000);
                }
            };
            timerHandler.post(uiTimerRunnable);
        } else {
            Log.d(TAG, "startTimerDisplay: Timer explicitly stopped or rideStartTimeMillis invalid. isTimerRunning=" + isTimerRunning + ", rideStartTimeMillis=" + rideStartTimeMillis);
            isTimerRunning = false;

            if (shouldRun && rideStartTimeMillis == 0) {
                Log.e(TAG, "startTimerDisplay: Attempted to start timer display but rideStartTimeMillis is 0!");
                txt_Timer.setVisibility(View.GONE);
                txt_ExtraTimer.setVisibility(View.GONE);
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

        final String currentPlanString = txt_Plan.getText().toString();
        startTimerDisplay(false, currentPlanString);

        long rideEndTimeMillis = System.currentTimeMillis();
        long totalElapsedTimeSeconds = (rideEndTimeMillis - rideStartTimeMillis) / 1000;
        if (totalElapsedTimeSeconds < 0) totalElapsedTimeSeconds = 0;

        Log.d(TAG, "endRideAction: Ride ended. StartTime(ms): " + rideStartTimeMillis +
                ", EndTime(ms): " + rideEndTimeMillis + ", Total Duration (sec): " + totalElapsedTimeSeconds);

        // Calculate plan duration and extra time
        long planDurationSeconds = 0;
        if (currentPlanString != null && !currentPlanString.isEmpty()) {
            try {
                String[] parts = currentPlanString.toLowerCase().split(" ");
                if (parts.length >= 2) {
                    int value = Integer.parseInt(parts[0]);
                    if (parts[1].startsWith("hour")) {
                        planDurationSeconds = TimeUnit.HOURS.toSeconds(value);
                    } else if (parts[1].startsWith("min")) {
                        planDurationSeconds = TimeUnit.MINUTES.toSeconds(value);
                    }
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing planString for endRideAction: " + currentPlanString, e);
            }
        }

        long finalMainRideDurationSeconds;
        long finalExtraTimeSeconds = 0;

        if (planDurationSeconds > 0 && totalElapsedTimeSeconds > planDurationSeconds) {
            finalMainRideDurationSeconds = planDurationSeconds;
            finalExtraTimeSeconds = totalElapsedTimeSeconds - planDurationSeconds;
        } else {
            finalMainRideDurationSeconds = totalElapsedTimeSeconds;
        }

        final long finalDurationForFirestore = totalElapsedTimeSeconds;
        final long extraTimeForFirestore = finalExtraTimeSeconds;

        Log.d(TAG, "endRideAction: PlanDuration(s): " + planDurationSeconds +
                ", TotalElapsed(s): " + totalElapsedTimeSeconds +
                ", MainRideDuration(s): " + finalMainRideDurationSeconds +
                ", ExtraTime(s): " + extraTimeForFirestore);

        db.collection("RideHistory").document(uid)
                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot rideDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String rideDocId = rideDoc.getId();

                        Map<String, Object> rideUpdates = new HashMap<>();
                        rideUpdates.put("elapsedTime", finalDurationForFirestore);
                        rideUpdates.put("extraTime", extraTimeForFirestore);
                        rideUpdates.put("rideStatus", "Completed");
                        rideUpdates.put("rideEndTime", rideEndTimeMillis);

                        rideDoc.getReference().update(rideUpdates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.i(TAG, "endRideAction: RideHistory/" + rideDocId + " updated in Firestore. Duration: " + finalDurationForFirestore + ", ExtraTime: " + extraTimeForFirestore + ", Status: Completed");

                                    Long bookingTimestamp = rideDoc.getLong("bookingTimestamp");
                                    if (bookingTimestamp != null) {
                                        Map<String, Object> allHistoryUpdates = new HashMap<>();
                                        allHistoryUpdates.put("elapsedTime", finalDurationForFirestore);
                                        allHistoryUpdates.put("extraTime", extraTimeForFirestore);
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

                                    // Update Realtime Database
                                    Map<String, Object> timerEndUpdate = new HashMap<>();
                                    timerEndUpdate.put("status", "ended");
                                    rideTimerRef.updateChildren(timerEndUpdate)
                                            .addOnSuccessListener(unused -> Log.i(TAG, "endRideAction: rideTimer status set to 'ended' in RTDB."))
                                            .addOnFailureListener(e -> Log.e(TAG, "endRideAction: Failed to update rideTimer status in RTDB.", e));

                                    userReleaseBikeRef.child("bikeReleased").setValue(false);
                                    userReleaseBikeRef.child("rideStartRequest").setValue(false);

                                    String durationMsg = "Ride Ended. Duration: " + formatSecondsToDisplay(finalDurationForFirestore);
                                    if (extraTimeForFirestore > 0) {
                                        durationMsg += " (including " + formatSecondsToDisplay(extraTimeForFirestore) + " extra)";
                                    }
                                    Toast.makeText(MyRides.this, durationMsg, Toast.LENGTH_LONG).show();

                                    rideStartTimeMillis = 0;
                                    loadRideInfo();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "endRideAction: Failed to update RideHistory in Firestore.", e);
                                    Toast.makeText(MyRides.this, "Error finalizing ride. Please check history.", Toast.LENGTH_SHORT).show();
                                    rideTimerRef.child("status").setValue("ended_fs_error");
                                    userReleaseBikeRef.child("bikeReleased").setValue(false);
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
                    loadRideInfo();
                });
    }

    private String formatSecondsToDisplay(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (days > 0) {
            return String.format(Locale.getDefault(), "%d day%s %02d hr%s %02d min %02d sec",
                    days, days > 1 ? "s" : "",
                    hours, hours > 1 ? "s" : "",
                    minutes, seconds);
        } else if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d hr%s %02d min %02d sec",
                    hours, hours > 1 ? "s" : "",
                    minutes, seconds);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%02d min %02d sec", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d sec", seconds);
        }
    }
}
