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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import android.os.Handler;
import java.util.Locale;

public class MyRides extends AppCompatActivity {
    TextView txt_Bicycle, txt_Location, txt_Plan, txt_Amount, txt_Date, txt_Paid, txt_Timer, txt_wait;

    Button btn_Cancel, btn_Pay, btn_Start;
    ImageView imgbtn_Back;
    CardView view_MyRide, view_NoRideData;
    private FirebaseAuth fAuth;
    private FirebaseFirestore db;
    private static final String TAG = "UserProfile";
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

        txt_Bicycle = findViewById(R.id.TXT_Bicycle);
        txt_Location = findViewById(R.id.TXT_Location);
        txt_Plan = findViewById(R.id.TXT_Plan);
        txt_Amount = findViewById(R.id.TXT_Amount);
        txt_Date = findViewById(R.id.TXT_Date);
        view_MyRide = findViewById(R.id.View_MyRide);
        view_NoRideData = findViewById(R.id.View_NoRideData);
        imgbtn_Back = findViewById(R.id.IMGBTN_Back);
        btn_Cancel = findViewById(R.id.BTN_Cancel);
        btn_Pay = findViewById(R.id.BTN_Pay);
        txt_Paid = findViewById(R.id.TXT_Paid);
        btn_Start = findViewById(R.id.BTN_Start);
        txt_Timer = findViewById(R.id.TXT_Timer);
        txt_wait = findViewById(R.id.TXT_Wait);

        imgbtn_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MyRides.this, UserDashboard.class);
                startActivity(intent);
                finish();
            }
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mainMenu();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        btn_Cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });

        btn_Pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MyRides.this, PaymentGateway.class);
                startActivity(intent);
            }
        });

        btn_Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isTimerRunning) {
                    txt_Timer.setVisibility(View.VISIBLE);
                    elapsedTimeInSeconds = 0;
                    isTimerRunning = true;
                    startTimer();
                    Toast.makeText(MyRides.this, "Ride Started", Toast.LENGTH_SHORT).show();
                }
            }
        });

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadRideInfo();
    }

    private void loadRideInfo() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("RideHistory")
                    .document(userId)
                    .collection("rides")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                if (document != null) {
                                    String bicycleType = document.getString("bicycleType");
                                    String location = document.getString("location");
                                    String plan = document.getString("plan");
                                    String amount = document.getString("amount");
                                    String date = document.getString("date");
                                    String paymentStatus = document.getString("payment");

                                    txt_Bicycle.setText(bicycleType);
                                    txt_Location.setText(location);
                                    txt_Plan.setText(plan);
                                    txt_Amount.setText("LKR "+amount+".00");
                                    txt_Date.setText(date);

                                    // 🔒 If already paid, hide Pay and Cancel buttons
                                    if (paymentStatus != null && paymentStatus.equalsIgnoreCase("Paid")) {
                                        btn_Pay.setVisibility(View.GONE);
                                        btn_Cancel.setVisibility(View.GONE);
                                        txt_Paid.setVisibility(View.VISIBLE);
                                        checkBicycleRelease();
                                    } else {
                                        btn_Pay.setVisibility(View.VISIBLE);
                                        btn_Cancel.setVisibility(View.VISIBLE);
                                    }

                                    if (bicycleType != null && !bicycleType.isEmpty()) {
                                        view_MyRide.setVisibility(View.VISIBLE);
                                        view_NoRideData.setVisibility(View.GONE);
                                    } else {
                                        view_MyRide.setVisibility(View.GONE);
                                        view_NoRideData.setVisibility(View.VISIBLE);
                                    }
                                } else {
                                    Log.w(TAG, "Most recent ride document is null.");
                                    view_MyRide.setVisibility(View.GONE);
                                    view_NoRideData.setVisibility(View.VISIBLE);
                                }

                            } else {
                                Log.d(TAG, "No ride history found for user");
                                view_MyRide.setVisibility(View.GONE);
                                view_NoRideData.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Log.e(TAG, "Error loading ride info: ", task.getException());
                            view_MyRide.setVisibility(View.GONE);
                            view_NoRideData.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ride")
                .setMessage("Are you sure you want to cancel your last ride?")
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteLastRide()) // Use your string
                .setNegativeButton(R.string.no, null) // Use your string
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private void deleteLastRide() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("RideHistory")
                    .document(userId)
                    .collection("rides")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                DocumentSnapshot lastRide = queryDocumentSnapshots.getDocuments().get(0);
                                if (lastRide != null) {
                                    lastRide.getReference().delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d(TAG, "Last ride deleted successfully");
                                                    Toast.makeText(MyRides.this, "Last ride cancelled.", Toast.LENGTH_SHORT).show();
                                                    loadRideInfo(); // Refresh the displayed ride info
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e(TAG, "Error deleting last ride: ", e);
                                                    Toast.makeText(MyRides.this, "Error cancelling last ride.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            } else {
                                Log.d(TAG, "No last ride history found for user");
                                Toast.makeText(MyRides.this, "No ride history to cancel.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error fetching ride history: ", e);
                            Toast.makeText(MyRides.this, "Error accessing ride history.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void checkBicycleRelease() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        // txt_wait and btn_Start are initialized in onCreate

        if (currentUser != null) {
            String userId = currentUser.getUid();

            DatabaseReference statusRef = FirebaseDatabase.getInstance()
                    .getReference("release_bicycle")
                    .child(userId)
                    .child("bikeReleased");

            // Defensive check for UI elements
            if (txt_wait == null || btn_Start == null) {
                Log.e(TAG, "checkBicycleRelease: txt_wait or btn_Start is null. Aborting.");
                return;
            }

            statusRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean isBikeReleased = snapshot.getValue(Boolean.class);
                    Log.d(TAG, "Bike release status for user " + userId + ": " + isBikeReleased);

                    txt_wait.setVisibility(View.GONE);
                    btn_Start.setVisibility(View.GONE);

                    if (isBikeReleased == null) {
                        Log.d(TAG, "bikeReleased status is null. Bike not yet processed for release.");

                    } else if (Boolean.FALSE.equals(isBikeReleased)) {
                        txt_wait.setText("Bike release pending. Please wait.");
                        txt_wait.setVisibility(View.VISIBLE);
                    } else if (Boolean.TRUE.equals(isBikeReleased)) {
                        Log.d(TAG, "bike Released. You can start the ride.");
                        btn_Start.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read bike release status: " + error.getMessage(), error.toException());

                    if (txt_wait != null) {
                        txt_wait.setText("Error checking bike status.");
                        txt_wait.setVisibility(View.VISIBLE);
                    }
                    if (btn_Start != null) {
                        btn_Start.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            Log.w(TAG, "checkBicycleRelease: Current user is null.");

            if (txt_wait != null) {
                txt_wait.setText("Please log in.");
                txt_wait.setVisibility(View.GONE);
            }
            if (btn_Start != null) {
                btn_Start.setVisibility(View.GONE);
            }
        }
    }

    private void mainMenu() {
        Intent goDash = new Intent(this, UserDashboard.class);
        startActivity(goDash);
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

                    String timeFormatted = String.format(Locale.getDefault(),
                            "%02d day %02d hrs %02d min %02d sec", days, hours, minutes, seconds);
                    txt_Timer.setText("Ride Time: " + timeFormatted);

                    elapsedTimeInSeconds++;
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }


}