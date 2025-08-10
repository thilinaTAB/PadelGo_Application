package com.example.padelgo.user;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Locale;

public class ExtraCharges extends AppCompatActivity {

    private static final String TAG = "ExtraCharges";
    // Assuming extraTime is stored in MINUTES. Adjust if it's seconds or another unit.
    private static final int EXTRA_TIME_THRESHOLD_MINUTES = 15; // Example: No charge for the first 15 extra minutes
    private static final double CHARGE_PER_MINUTE = 0.25;       // Example: LKR 1.0 per minute AFTER the threshold
    private static final double FIXED_EXTRA_CHARGE = 150.0;    // Example: A fixed charge if any per-minute charge is applied

    TextView txt_ExtraTime, txt_ExtraTimeCharge, txt_TotalCharge;
    Button btn_ExtraPay;

    private FirebaseFirestore db;
    private FirebaseAuth fAuth;

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
        fAuth = FirebaseAuth.getInstance();

        btn_ExtraPay.setOnClickListener(v -> {
            // Retrieve the total amount from txt_TotalCharge to pass to payment
            String totalChargeText = txt_TotalCharge.getText().toString();
            // Potentially parse totalChargeText to get the double value if needed
            Toast.makeText(ExtraCharges.this, "Payment for " + totalChargeText, Toast.LENGTH_LONG).show();
            // TODO: Implement actual payment processing logic
            // You might want to pass the calculated totalCharge or the ride ID to a payment activity here
        });

        calculateExtraCharges();
    }

    private void calculateExtraCharges() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "calculateExtraCharges: No current user logged in.");
            txt_ExtraTime.setText("User not logged in");
            txt_ExtraTimeCharge.setText("LKR 0.00");
            txt_TotalCharge.setText("LKR 0.00");
            // Optionally disable the pay button
            // btn_ExtraPay.setEnabled(false);
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "calculateExtraCharges: Loading ride info for user " + userId);

        db.collection("RideHistory").document(userId)
                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "calculateExtraCharges: Firestore success. Snapshot empty: " + queryDocumentSnapshots.isEmpty());
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String firestoreRideDocId = doc.getId();
                        Log.d(TAG, "calculateExtraCharges: Processing document ID: " + firestoreRideDocId);

                        // --- Get Base Amount ---


                        // --- Get Extra Time (stored as Long in SECONDS) ---
                        Long extraTimeSeconds = doc.getLong("extraTime")-900; // Assuming 'extraTime' is total seconds
                        double extraTimeCharge = 0.0;
                        String displayExtraTime;

                        if (extraTimeSeconds != null) {
                            Log.d(TAG, "Extra time (seconds from DB): " + extraTimeSeconds);

                            // Convert seconds to a displayable format (e.g., "X min Y sec" or just total minutes)
                            long totalMinutes = extraTimeSeconds / 60;
                            long remainingSeconds = extraTimeSeconds % 60;
                            if (totalMinutes > 0) {
                                displayExtraTime = String.format(Locale.getDefault(), "%d min %d sec", totalMinutes, remainingSeconds);
                            } else {
                                displayExtraTime = String.format(Locale.getDefault(), "%d sec", remainingSeconds);
                            }
                            txt_ExtraTime.setText(displayExtraTime);

                            // Convert total extra seconds to minutes for charging calculation
                            double extraTimeMinutesForCalc = extraTimeSeconds / 60.0; // Use double for precision
                            extraTimeCharge = extraTimeMinutesForCalc * CHARGE_PER_MINUTE;
                            if (extraTimeCharge < 150) {
                                extraTimeCharge = FIXED_EXTRA_CHARGE;

                                Log.d(TAG, String.format(Locale.US, "Chargeable minutes: %.2f, Calculated extra time charge: %.2f", extraTimeMinutesForCalc, extraTimeCharge));
                            } else {
                                Log.d(TAG, "Extra time (in minutes: " + String.format(Locale.US, "%.2f", extraTimeMinutesForCalc) + ") is within the threshold of " + EXTRA_TIME_THRESHOLD_MINUTES + " minutes.");
                            }
                        } else {
                            Log.w(TAG, "calculateExtraCharges: 'extraTime' field is null in document: " + firestoreRideDocId);
                            txt_ExtraTime.setText("No extra time recorded");
                        }

                        txt_ExtraTimeCharge.setText(String.format(Locale.getDefault(), "LKR %.2f", extraTimeCharge));

                        // ... rest of the code for totalCharge calculation remains the same ...
                        double totalCharge = extraTimeCharge;
                        txt_TotalCharge.setText(String.format(Locale.getDefault(), "LKR %.2f", totalCharge));
                        Log.i(TAG, "Final Calculation: Base= ExtraCharge= " + extraTimeCharge + ", Total= " + totalCharge);


                    } else {
                        Log.w(TAG, "calculateExtraCharges: No ride documents found for user " + userId);
                        txt_ExtraTime.setText("No ride data found");
                        txt_ExtraTimeCharge.setText("LKR 0.00");
                        txt_TotalCharge.setText("LKR 0.00");
                        // btn_ExtraPay.setEnabled(false); // Optionally disable pay button
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "calculateExtraCharges: Error fetching ride history", e);
                    txt_ExtraTime.setText("Error loading data");
                    txt_ExtraTimeCharge.setText("LKR 0.00");
                    txt_TotalCharge.setText("LKR 0.00");
                    Toast.makeText(ExtraCharges.this, "Failed to load ride details.", Toast.LENGTH_SHORT).show();
                    // btn_ExtraPay.setEnabled(false); // Optionally disable pay button
                });
    }
}
