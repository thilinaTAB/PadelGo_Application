package com.example.padelgo.user;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.firestore.QuerySnapshot;

import android.os.Handler;
import java.util.Locale;

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

        // Initialize UI
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

        // Firebase init
        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        realtimeDB = FirebaseDatabase.getInstance().getReference();

        // UI listeners
        imgbtn_Back.setOnClickListener(v -> {
            startActivity(new Intent(MyRides.this, UserDashboard.class));
            finish();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mainMenu();
            }
        });
        btn_Cancel.setOnClickListener(v -> showDeleteConfirmationDialog());
        btn_Pay.setOnClickListener(v -> startActivity(new Intent(MyRides.this, PaymentGateway.class)));
        btn_Start.setOnClickListener(v -> startRideAction());

        // Load ride
        loadRideInfo();
    }

    private void loadRideInfo() {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        db.collection("RideHistory").document(uid)
                .collection("rides").orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        // Populate UI
                        txt_Bicycle.setText(doc.getString("bicycleType"));
                        txt_Location.setText(doc.getString("location"));
                        txt_Plan.setText(doc.getString("plan"));
                        txt_Amount.setText("LKR " + doc.getString("amount") + ".00");
                        txt_Date.setText(doc.getString("date"));
                        String payStatus = doc.getString("payment");

                        if ("Paid".equalsIgnoreCase(payStatus)) {
                            btn_Pay.setVisibility(View.GONE);
                            btn_Cancel.setVisibility(View.GONE);
                            txt_Paid.setVisibility(View.VISIBLE);
                            btn_Start.setVisibility(View.VISIBLE);
                            checkBicycleRelease();
                        } else {
                            btn_Pay.setVisibility(View.VISIBLE);
                            btn_Cancel.setVisibility(View.VISIBLE);
                        }

                        view_MyRide.setVisibility(View.VISIBLE);
                        view_NoRideData.setVisibility(View.GONE);
                    } else {
                        view_MyRide.setVisibility(View.GONE);
                        view_NoRideData.setVisibility(View.VISIBLE);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading ride", e);
                    view_MyRide.setVisibility(View.GONE);
                    view_NoRideData.setVisibility(View.VISIBLE);
                });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ride")
                .setMessage("Are you sure you want to cancel your last ride?")
                .setPositiveButton(R.string.yes, (d, w) -> deleteLastRide())
                .setNegativeButton(R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteLastRide() {
        FirebaseUser user = fAuth.getCurrentUser(); if (user == null) return;
        String uid = user.getUid();
        db.collection("RideHistory").document(uid)
                .collection("rides").orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        snap.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(this, "Last ride cancelled.", Toast.LENGTH_SHORT).show();
                                    loadRideInfo();
                                });
                    } else Toast.makeText(this, "No ride history to cancel.", Toast.LENGTH_SHORT).show();
                });
    }

    private void startRideAction() {
        FirebaseUser user = fAuth.getCurrentUser(); if (user == null) return;
        String uid = user.getUid();
        DatabaseReference ref = realtimeDB.child("release_bicycle").child(uid);
        ref.child("rideStartRequest").setValue(true);
        ref.child("bikeReleased").setValue(false);

        // Update Firestore
        db.collection("RideHistory").document(uid)
                .collection("rides").orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1).get().addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        String docId = snap.getDocuments().get(0).getId();
                        db.collection("RideHistory").document(uid)
                                .collection("rides").document(docId)
                                .update("payment", "Paid", "rideStartRequest", true, "bikeReleased", false)
                                .addOnSuccessListener(a -> Log.i(TAG, "RideStartRequest set"));
                    }
                });

        // UI: hide start immediately
        btn_Start.setVisibility(View.GONE);
        txt_wait.setText("Bike release pending. Please wait.");
        txt_wait.setVisibility(View.VISIBLE);
        checkBicycleRelease();
    }

    private void checkBicycleRelease() {
        FirebaseUser user = fAuth.getCurrentUser(); if (user == null) return;
        String uid = user.getUid();
        DatabaseReference statusRef = realtimeDB.child("release_bicycle").child(uid).child("bikeReleased");

        statusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean released = snapshot.getValue(Boolean.class);
                // Always hide start if already requested
                btn_Start.setVisibility(View.GONE);
                txt_wait.setVisibility(View.GONE);
                btn_End.setVisibility(View.GONE);

                if (Boolean.TRUE.equals(released)) {
                    btn_End.setVisibility(View.VISIBLE);
                    if (!isTimerRunning) {
                        txt_Timer.setVisibility(View.VISIBLE);
                        elapsedTimeInSeconds = 0;
                        isTimerRunning = true;
                        startTimer();
                        Toast.makeText(MyRides.this, "Ride Started", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    txt_wait.setText("Bike release pending. Please wait.");
                    txt_wait.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txt_wait.setText("Error checking bike status.");
                txt_wait.setVisibility(View.VISIBLE);
                btn_End.setVisibility(View.GONE);
            }
        });
    }

    private void mainMenu() {
        startActivity(new Intent(this, UserDashboard.class));
        finish();
    }

    private void startTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isTimerRunning) {
                    int days = elapsedTimeInSeconds / 86400;
                    int hours = (elapsedTimeInSeconds % 86400) / 3600;
                    int minutes = (elapsedTimeInSeconds % 3600) / 60;
                    int seconds = elapsedTimeInSeconds % 60;
                    String formatted = String.format(Locale.getDefault(), "%02d day %02d hrs %02d min %02d sec", days, hours, minutes, seconds);
                    txt_Timer.setText("Ride Time: " + formatted);
                    elapsedTimeInSeconds++;
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }
}
