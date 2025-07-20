package com.example.padelgo.user;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
// import androidx.core.content.ContextCompat; // Not strictly needed for getString with formatting
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser;   // Import FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;       // Import Query

public class ExtraCharges extends AppCompatActivity {

    private static final String TAG = "ExtraCharges";
    private static final int EXTRA_TIME_THRESHOLD_MINUTES = 15;
    private static final double CHARGE_PER_MINUTE = 1.0;
    private static final double FIXED_EXTRA_CHARGE = 150.0;

    TextView txt_ExtraTime, txt_ExtraTimeCharge, txt_TotalCharge;
    Button btn_ExtraPay;

    private FirebaseFirestore db;
    private FirebaseAuth fAuth; // Add FirebaseAuth instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_extra_charges);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txt_ExtraTime = findViewById(R.id.TXT_ExtraTime);
        txt_ExtraTimeCharge = findViewById(R.id.TXT_ExtraTimeCharge);
        txt_TotalCharge = findViewById(R.id.TXT_TotalCharge);
        btn_ExtraPay = findViewById(R.id.BTN_ExtraPay);

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth

        // Fetch ride history for the current user directly
        fetchLatestRideAndCalculateChargesForCurrentUser();

        btn_ExtraPay.setOnClickListener(v -> {
            Toast.makeText(ExtraCharges.this, "Payment button clicked!", Toast.LENGTH_SHORT).show();
            // You might want to pass the calculated totalCharge or the ride ID to a payment activity here
        });
    }

    @SuppressLint("StringFormatInvalid")
    private void fetchLatestRideAndCalculateChargesForCurrentUser() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found. Cannot calculate extra charges.");
            Toast.makeText(this, "Error: You must be logged in.", Toast.LENGTH_LONG).show();
            // Optionally, disable UI elements or finish the activity
            txt_ExtraTime.setText(getString(R.string.extra_time_format, 0));
            txt_ExtraTimeCharge.setText(getString(R.string.currency_format, 0.0));
            txt_TotalCharge.setText(getString(R.string.currency_format, 0.0));
            btn_ExtraPay.setEnabled(false);
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "Fetching latest ride for user: " + uid);

        db.collection("RideHistory").document(uid)
                .collection("rides")
                .limit(1) // Just get any one document from the subcollection
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            Log.d(TAG, "TEST FETCH: Single ride document fetched: " + document.getId() + " => " + document.getData());
                            // Now check if this document has 'extraTime'
                            if (document.contains("extraTime")) {
                                Number extraTimeNumber = document.getLong("extraTime");
                                Log.d(TAG, "TEST FETCH: extraTime field exists, value: " + extraTimeNumber);
                                if (extraTimeNumber != null) {
                                    calculateAndDisplayCharges(extraTimeNumber.intValue());
                                } else {
                                    calculateAndDisplayCharges(0); // extraTime was null
                                }
                            } else {
                                Log.e(TAG, "TEST FETCH: 'extraTime' field is MISSING in the fetched document.");
                                calculateAndDisplayCharges(0); // extraTime field missing
                            }
                        } else {
                            Log.w(TAG, "TEST FETCH: No ride documents found in 'rides' subcollection for user " + uid);
                            calculateAndDisplayCharges(0);
                        }
                    } else {
                        Log.e(TAG, "TEST FETCH: Error fetching any ride: ", task.getException());
                        calculateAndDisplayCharges(0);
                    }
                });
    }

    @SuppressLint("StringFormatInvalid") // Keep if your IDE still warns, but should be fine with direct getString
    private void calculateAndDisplayCharges(int extraTimeMinutes) {
        double extraCharge = 0.0;
        double totalCharge; // This will be the extra charge itself

        if (extraTimeMinutes > EXTRA_TIME_THRESHOLD_MINUTES) {
            extraCharge = (extraTimeMinutes * CHARGE_PER_MINUTE) + FIXED_EXTRA_CHARGE;
        }

        totalCharge = extraCharge;

        Log.d(TAG, "Calculated Extra Time: " + extraTimeMinutes + " mins, Extra Charge: Rs " + extraCharge + ", Total Charge: Rs " + totalCharge);

        txt_ExtraTime.setText(getString(R.string.extra_time_format, extraTimeMinutes));
        txt_ExtraTimeCharge.setText(getString(R.string.currency_format, extraCharge));
        txt_TotalCharge.setText(getString(R.string.currency_format, totalCharge));

        btn_ExtraPay.setEnabled(totalCharge > 0);
    }
}
