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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MyRides extends AppCompatActivity {
    TextView txt_Bicycle, txt_Location, txt_Plan, txt_Amount, txt_Date, txt_Paid, txt_Timer, txt_wait;
    Button btn_Cancel, btn_Pay, btn_Start, btn_End;
    ImageView imgbtn_Back;
    CardView view_MyRide, view_NoRideData;
    private FirebaseAuth fAuth;
    private FirebaseFirestore db;
    private DatabaseReference realtimeDB;
    private static final String TAG = "MyRides";
    private Handler timerHandler = new Handler(Looper.getMainLooper());

    private int elapsedTimeInSeconds = 0;
    private boolean isTimerRunning = false;
    private ValueEventListener bikeReleaseListener;
    private DatabaseReference statusRef;


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

        loadRideInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. isTimerRunning: " + isTimerRunning);
        loadRideInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called.");
        if (statusRef != null && bikeReleaseListener != null) {
            Log.d(TAG, "onPause: Removing bikeReleaseListener.");
            statusRef.removeEventListener(bikeReleaseListener);
            bikeReleaseListener = null;
        }
        Log.d(TAG, "onPause: Removing timer callbacks and setting isTimerRunning to false.");
        timerHandler.removeCallbacksAndMessages(null);
        isTimerRunning = false;
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
            return;
        }

        String uid = user.getUid();
        Log.d(TAG, "loadRideInfo: Loading ride info for user " + uid + ". Current isTimerRunning: " + isTimerRunning);

        db.collection("RideHistory").document(uid)
                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "loadRideInfo: Firestore success. Snapshot empty: " + snapshot.isEmpty());
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        Log.d(TAG, "loadRideInfo: Document ID: " + doc.getId());
                        txt_Bicycle.setText(doc.getString("bikeType"));
                        txt_Location.setText(doc.getString("location"));
                        txt_Plan.setText(doc.getString("plan"));
                        txt_Amount.setText("LKR " + doc.getString("amount") + ".00");
                        txt_Date.setText(doc.getString("dateAndTime"));

                        String payStatus = doc.getString("payment");
                        Boolean rideStartRequest = doc.getBoolean("rideStartRequest");
                        Boolean bikeReleased = doc.getBoolean("bikeReleased");
                        Long elapsedTimeFromDB = doc.getLong("elapsedTime");

                        Log.d(TAG, "loadRideInfo: payStatus=" + payStatus + ", rideStartRequest=" + rideStartRequest + ", bikeReleased=" + bikeReleased + ", elapsedTimeFromDB=" + elapsedTimeFromDB);

                        txt_Paid.setVisibility(View.GONE);
                        btn_Pay.setVisibility(View.GONE);
                        btn_Cancel.setVisibility(View.GONE);
                        btn_Start.setVisibility(View.GONE);
                        btn_End.setVisibility(View.GONE);
                        txt_Timer.setVisibility(View.GONE);
                        txt_wait.setVisibility(View.GONE);


                        if ("Paid".equalsIgnoreCase(payStatus)) {
                            Log.d(TAG, "loadRideInfo: Ride is Paid.");
                            txt_Paid.setVisibility(View.VISIBLE);
                            if (Boolean.TRUE.equals(bikeReleased)) {
                                Log.d(TAG, "loadRideInfo: Bike is RELEASED. isTimerRunning: " + isTimerRunning);
                                btn_End.setVisibility(View.VISIBLE);
                                txt_Timer.setVisibility(View.VISIBLE);
                                if (!isTimerRunning) {
                                    Log.d(TAG, "loadRideInfo: Timer was not running. Starting timer now.");
                                    elapsedTimeInSeconds = elapsedTimeFromDB != null ? elapsedTimeFromDB.intValue() : 0;
                                    startTimer(true);
                                } else {
                                    Log.d(TAG, "loadRideInfo: Timer already considered running. Updating text if needed by startTimer itself.");
                                    startTimer(true);
                                }
                            } else if (Boolean.TRUE.equals(rideStartRequest)) {
                                Log.d(TAG, "loadRideInfo: Ride start REQUESTED, bike not released.");
                                txt_wait.setText("Bike release pending. Please wait.");
                                txt_wait.setVisibility(View.VISIBLE);
                                checkBicycleRelease();
                            } else {
                                Log.d(TAG, "loadRideInfo: Ride Paid, but not started/released. Show START button.");
                                btn_Start.setVisibility(View.VISIBLE);
                            }
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
                        btn_Pay.setVisibility(View.GONE);
                        btn_Cancel.setVisibility(View.GONE);
                        btn_Start.setVisibility(View.GONE);
                        btn_End.setVisibility(View.GONE);
                        txt_Paid.setVisibility(View.GONE);
                        txt_Timer.setVisibility(View.GONE);
                        txt_wait.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "loadRideInfo: Error loading ride info from Firestore", e);
                    view_MyRide.setVisibility(View.GONE);
                    view_NoRideData.setVisibility(View.VISIBLE);
                    btn_Pay.setVisibility(View.GONE);
                    btn_Cancel.setVisibility(View.GONE);
                    btn_Start.setVisibility(View.GONE);
                    btn_End.setVisibility(View.GONE);
                    txt_Paid.setVisibility(View.GONE);
                    txt_Timer.setVisibility(View.GONE);
                    txt_wait.setVisibility(View.GONE);
                });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ride")
                .setMessage("Are you sure you want to cancel your last ride?")
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
                        String docId = snap.getDocuments().get(0).getId();
                        Log.d(TAG, "deleteLastRide: Found ride " + docId + " to delete.");
                        snap.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(a -> {
                                    Log.d(TAG, "deleteLastRide: Ride " + docId + " deleted successfully.");
                                    Toast.makeText(this, "Last ride cancelled.", Toast.LENGTH_SHORT).show();
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
                .setMessage("Do you need to start your ride?")
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
        Log.d(TAG, "startRideAction: Attempting to start ride for user " + uid);
        DatabaseReference rtRef = realtimeDB.child("release_bicycle").child(uid);

        //Fetch user's full name first
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String userFullName = userDoc.getString("Full Name");
                        if (userFullName == null) {
                            userFullName = "";
                            Log.w(TAG, "User fullName not found in Firestore. Using empty string.");
                        }
                        final String finalUserFullName = userFullName;

                        //Fetch RideHistory
                        db.collection("RideHistory").document(uid)
                                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                                .limit(1).get()
                                .addOnSuccessListener(rideHistorySnapshot -> {
                                    if (!rideHistorySnapshot.isEmpty()) {
                                        DocumentSnapshot rideHistoryDoc = rideHistorySnapshot.getDocuments().get(0);
                                        String rideHistoryDocId = rideHistoryDoc.getId();
                                        Long bookingTimestamp = rideHistoryDoc.getLong("bookingTimestamp");

                                        Log.d(TAG, "startRideAction: Found RideHistory doc " + rideHistoryDocId + " to update.");

                                        // 1. Update RideHistory
                                        db.collection("RideHistory").document(uid)
                                                .collection("rides").document(rideHistoryDocId)
                                                .update("rideStartRequest", true, "bikeReleased", false, "elapsedTime", 0)
                                                .addOnSuccessListener(a -> {
                                                    Log.i(TAG, "startRideAction: RideHistory updated for doc " + rideHistoryDocId + ". rideStartRequest=true");

                                                    // 2. Update AllHistory
                                                    if (bookingTimestamp != null) {
                                                        db.collection("AllHistory")
                                                                .whereEqualTo("userId", uid) // or rideHistoryDoc.getString("userId")
                                                                .whereEqualTo("bookingTimestamp", bookingTimestamp)
                                                                .limit(1)
                                                                .get()
                                                                .addOnSuccessListener(allHistorySnapshot -> {
                                                                    if (!allHistorySnapshot.isEmpty()) {
                                                                        String allHistoryDocId = allHistorySnapshot.getDocuments().get(0).getId();
                                                                        db.collection("AllHistory").document(allHistoryDocId)
                                                                                .update("rideStartRequest", true)
                                                                                .addOnSuccessListener(aVoid -> Log.i(TAG, "startRideAction: AllHistory updated for doc " + allHistoryDocId + ". rideStartRequest=true"))
                                                                                .addOnFailureListener(e -> Log.e(TAG, "startRideAction: Failed to update rideStartRequest in AllHistory for doc " + allHistoryDocId, e));
                                                                    } else {
                                                                        Log.w(TAG, "startRideAction: Could not find matching document in AllHistory to update. UserID: " + uid + ", BookingTimestamp: " + bookingTimestamp);
                                                                    }
                                                                })
                                                                .addOnFailureListener(e -> Log.e(TAG, "startRideAction: Error querying AllHistory", e));
                                                    } else {
                                                        Log.w(TAG, "startRideAction: bookingTimestamp is null in RideHistory doc (" + rideHistoryDocId + "), cannot accurately update AllHistory.");
                                                    }

                                                    // 3. Update Realtime Database
                                                    Map<String, Object> releaseData = new HashMap<>();
                                                    releaseData.put("rideStartRequest", true);
                                                    releaseData.put("bikeReleased", false);
                                                    releaseData.put("fullName", finalUserFullName);

                                                    rtRef.setValue(releaseData)
                                                            .addOnSuccessListener(unused -> Log.i(TAG, "startRideAction: Realtime DB updated for user " + uid + ". rideStartRequest=true, fullName=" + finalUserFullName))
                                                            .addOnFailureListener(e -> Log.e(TAG, "startRideAction: Failed to update Realtime DB for user " + uid, e));

                                                    //4. Update UI
                                                    btn_Start.setVisibility(View.GONE);
                                                    txt_wait.setText("Bike release pending. Please wait.");
                                                    txt_wait.setVisibility(View.VISIBLE);
                                                    checkBicycleRelease();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "startRideAction: Failed to update RideStartRequest in RideHistory for doc " + rideHistoryDocId, e);
                                                    Toast.makeText(MyRides.this, "Failed to start ride. Please try again.", Toast.LENGTH_SHORT).show();
                                                });
                                    } else {
                                        Log.w(TAG, "startRideAction: No ride found in RideHistory to start for user " + uid);
                                        Toast.makeText(MyRides.this, "No ride found to start.", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "startRideAction: Failed to get RideHistory document for ride start action for user " + uid, e);
                                    Toast.makeText(MyRides.this, "Error starting ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.w(TAG, "startRideAction: User document not found in Firestore. Cannot get fullName for Realtime DB.");
                        Toast.makeText(MyRides.this, "User profile not found, cannot start ride.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "startRideAction: Failed to fetch user fullName from Firestore for Realtime DB update.", e);
                    Toast.makeText(MyRides.this, "Error fetching user details for ride start.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkBicycleRelease() {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "checkBicycleRelease: User is null. Cannot check release status.");
            return;
        }
        String uid = user.getUid();
        Log.d(TAG, "checkBicycleRelease: Setting up listener for bikeReleased for user " + uid);

        if (statusRef != null && bikeReleaseListener != null) {
            Log.d(TAG, "checkBicycleRelease: Removing existing bikeReleaseListener before adding a new one.");
            statusRef.removeEventListener(bikeReleaseListener);
        }
        statusRef = realtimeDB.child("release_bicycle").child(uid).child("bikeReleased");

        bikeReleaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean released = snapshot.getValue(Boolean.class);
                Log.d(TAG, "checkBicycleRelease - onDataChange: bikeReleased from RTDB = " + released + ". Current isTimerRunning: " + isTimerRunning);

                txt_wait.setVisibility(View.GONE);

                if (Boolean.TRUE.equals(released)) {
                    Log.d(TAG, "checkBicycleRelease - onDataChange: Bike IS RELEASED.");
                    txt_Paid.setVisibility(View.VISIBLE);
                    btn_End.setVisibility(View.VISIBLE);
                    txt_Timer.setVisibility(View.VISIBLE);

                    if (!isTimerRunning) {
                        Log.d(TAG, "checkBicycleRelease - onDataChange: Timer was not running. Fetching elapsedTime and starting timer.");
                        db.collection("RideHistory").document(uid)
                                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                                .limit(1).get().addOnSuccessListener(rideSnap -> {
                                    if (!rideSnap.isEmpty()) {
                                        DocumentSnapshot currentRideDoc = rideSnap.getDocuments().get(0);
                                        Long elapsedTimeFromDB = currentRideDoc.getLong("elapsedTime");
                                        Log.d(TAG, "checkBicycleRelease - onDataChange: Fetched elapsedTime from Firestore: " + elapsedTimeFromDB);
                                        elapsedTimeInSeconds = elapsedTimeFromDB != null ? elapsedTimeFromDB.intValue() : 0;
                                        startTimer(true);
                                        Toast.makeText(MyRides.this, "Ride Started", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.w(TAG, "checkBicycleRelease - onDataChange: Ride document not found after release signal. Cannot start timer.");
                                    }
                                }).addOnFailureListener(e -> Log.e(TAG, "checkBicycleRelease - onDataChange: Error fetching ride doc for timer start.", e));
                    } else {
                        Log.d(TAG, "checkBicycleRelease - onDataChange: Timer already considered running.");
                        startTimer(true);
                    }
                } else {
                    Log.d(TAG, "checkBicycleRelease - onDataChange: Bike IS NOT RELEASED (or value is null).");
                    db.collection("RideHistory").document(uid)
                            .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                            .limit(1).get().addOnSuccessListener(rideSnap -> {
                                if (!rideSnap.isEmpty()) {
                                    Boolean rideStartRequested = rideSnap.getDocuments().get(0).getBoolean("rideStartRequest");
                                    Log.d(TAG, "checkBicycleRelease - onDataChange (bike not released): rideStartRequested from Firestore = " + rideStartRequested);
                                    if (Boolean.TRUE.equals(rideStartRequested)) {
                                        txt_wait.setText("Bike release pending. Please wait.");
                                        txt_wait.setVisibility(View.VISIBLE);
                                        txt_Paid.setVisibility(View.VISIBLE);
                                        btn_End.setVisibility(View.GONE);
                                        txt_Timer.setVisibility(View.GONE);
                                        if(isTimerRunning) startTimer(false);
                                    } else {
                                        Log.d(TAG, "checkBicycleRelease - onDataChange (bike not released): No start request. Calling loadRideInfo().");
                                        if(isTimerRunning) startTimer(false);
                                        loadRideInfo();
                                    }
                                } else {
                                    if(isTimerRunning) startTimer(false);
                                    loadRideInfo();
                                }
                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "checkBicycleRelease - onDataChange (bike not released): Error fetching ride doc.", e);
                                if(isTimerRunning) startTimer(false);
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
                if(isTimerRunning) startTimer(false);
            }
        };
        statusRef.addValueEventListener(bikeReleaseListener);
    }

    private void mainMenu() {
        Log.d(TAG, "mainMenu called. Navigating to UserDashboard.");
        startActivity(new Intent(this, UserDashboard.class));
        finish();
    }

    private void startTimer(boolean shouldRun) {
        timerHandler.removeCallbacksAndMessages(null);
        isTimerRunning = shouldRun;

        Log.d(TAG, "startTimer() called. shouldRun = " + shouldRun + ", isTimerRunning = " + isTimerRunning + ", current elapsedTimeInSeconds = " + elapsedTimeInSeconds);

        if (isTimerRunning) {
            if (txt_Timer.getVisibility() != View.VISIBLE) {
                Log.w(TAG, "startTimer: txt_Timer was not visible, making it visible now.");
                txt_Timer.setVisibility(View.VISIBLE);
            }

            Runnable timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isTimerRunning) {
                        Log.d(TAG, "Timer runnable: isTimerRunning is false, stopping.");
                        timerHandler.removeCallbacksAndMessages(null);
                        return;
                    }

                    int days = elapsedTimeInSeconds / 86400;
                    int hours = (elapsedTimeInSeconds % 86400) / 3600;
                    int minutes = (elapsedTimeInSeconds % 3600) / 60;
                    int seconds = elapsedTimeInSeconds % 60;
                    String formattedTime = String.format(Locale.getDefault(), "%02d day %02d hrs %02d min %02d sec", days, hours, minutes, seconds);
                    txt_Timer.setText("Ride Time: " + formattedTime);

                    elapsedTimeInSeconds++;

                    if (elapsedTimeInSeconds % 10 == 0) {
                        FirebaseUser user = fAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            db.collection("RideHistory").document(uid)
                                    .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                                    .limit(1).get().addOnSuccessListener(snap -> {
                                        if (!snap.isEmpty()) {
                                            String docId = snap.getDocuments().get(0).getId();
                                            snap.getDocuments().get(0).getReference().update("elapsedTime", elapsedTimeInSeconds)
                                                    .addOnSuccessListener(voidOk -> Log.v(TAG, "Timer: Updated elapsedTime in Firestore to " + elapsedTimeInSeconds + " for doc " + docId))
                                                    .addOnFailureListener(e -> Log.e(TAG, "Timer: Failed to update elapsedTime in Firestore for doc " + docId, e));
                                        }
                                    }).addOnFailureListener(e -> Log.e(TAG, "Timer: Failed to get ride doc for updating elapsedTime.", e));
                        }
                    }
                    timerHandler.postDelayed(this, 1000);
                }
            };
            timerHandler.post(timerRunnable);
        } else {
            Log.d(TAG, "startTimer: Timer explicitly stopped or not started (shouldRun=false). Clearing text and hiding.");
            txt_Timer.setText("Ride Time: 00 day 00 hrs 00 min 00 sec");
            txt_Timer.setVisibility(View.GONE);
        }
    }
}