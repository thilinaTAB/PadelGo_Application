package com.example.padelgo.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private static final int gracePeriodSec = 900;
    private static final double chargePerMin = 5;
    private static final double minimumCharge = 150.0;

    TextView txt_ExtraTime, txt_ExtraTimeCharge, txt_TotalCharge, txt_OtherCharge;
    Button btn_ExtaPay;

    private FirebaseFirestore db;
    private FirebaseAuth fAuth;
    private static final String fStoreExtraChargeField = "calculatedExtraChargeAmountLKR";
    private static final String fStoreExtraTime = "extraTime";
    private static final String fStoreInitialTotalAmount = "amount";
    private static final String fStoreFineAmount = "fineAmount";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_extra_charges);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txt_ExtraTime = findViewById(R.id.TXT_ExtraTime);
        txt_ExtraTimeCharge = findViewById(R.id.TXT_ExtraTimeCharge);
        txt_TotalCharge = findViewById(R.id.TXT_TotalCharge);
        txt_OtherCharge = findViewById(R.id.TXT_OtherCharge);

        btn_ExtaPay = findViewById(R.id.BTN_ExtraPay);

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        btn_ExtaPay.setVisibility(View.INVISIBLE);
        txt_OtherCharge.setText(String.format(Locale.getDefault(), "LKR %.2f", 0.0));

        btn_ExtaPay.setOnClickListener(v -> {
            Log.d(TAG, "Proceed to Payment Gateway button clicked.");
            Intent paymentIntent = new Intent(ExtraCharges.this, PaymentGateway.class);
            startActivity(paymentIntent);
            finish();
        });

        loadRideDetails();
    }

    private void loadRideDetails() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser == null) {
            updateDisplay("User not logged in", "LKR 0.00", "LKR 0.00", "LKR 0.00");
            btn_ExtaPay.setVisibility(View.INVISIBLE);
            return;
        }

        String userId = currentUser.getUid();
        // Assuming you are fetching the latest ride for the user
        db.collection("RideHistory").document(userId)
                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        updateDisplay("No ride data found", "LKR 0.00", "LKR 0.00", "LKR 0.00");
                        btn_ExtaPay.setVisibility(View.INVISIBLE);
                        Toast.makeText(ExtraCharges.this, "No ride data found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                    String rideDocId = doc.getId();
                    Log.d(TAG, "Processing ride document: " + rideDocId);

                    //Extra Time Display
                    Long extraTimeSecondsRaw = doc.getLong(fStoreExtraTime);
                    String displayExtraTimeText = "No extra time recorded";
                    if (extraTimeSecondsRaw != null && extraTimeSecondsRaw > 0) {
                        long totalDisplayMinutes = extraTimeSecondsRaw / 60;
                        long remainingDisplaySeconds = extraTimeSecondsRaw % 60;
                        if (totalDisplayMinutes > 0) {
                            displayExtraTimeText = String.format(Locale.getDefault(), "%d min %d sec", totalDisplayMinutes, remainingDisplaySeconds);
                        } else {
                            displayExtraTimeText = String.format(Locale.getDefault(), "%d sec", remainingDisplaySeconds);
                        }
                    }
                    txt_ExtraTime.setText(displayExtraTimeText);


                    //Calculated Extra Time Charge
                    double calculatedAutomatedExtraCharge = 0.0;
                    if (doc.contains(fStoreExtraChargeField)) {
                        String extraChargeAmountLKRString = doc.getString(fStoreExtraChargeField);
                        if (extraChargeAmountLKRString != null && !extraChargeAmountLKRString.isEmpty()) {
                            try {
                                calculatedAutomatedExtraCharge = Double.parseDouble(extraChargeAmountLKRString);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid format for " + fStoreExtraChargeField + " in " + rideDocId + ": " + extraChargeAmountLKRString);
                            }
                        }
                    } else if (extraTimeSecondsRaw != null) {
                        calculatedAutomatedExtraCharge = calculateAutomatedExtraChargeValue(extraTimeSecondsRaw);
                    }
                    String displayExtraChargeText = String.format(Locale.getDefault(), "LKR %.2f", calculatedAutomatedExtraCharge);
                    txt_ExtraTimeCharge.setText(displayExtraChargeText);


                    //Station Officer Applied Fine
                    double AppliedFine = 0.0;
                    if (doc.contains(fStoreFineAmount)) {
                        Object fineAmountObj = doc.get(fStoreFineAmount);
                        if (fineAmountObj instanceof Number) {
                            AppliedFine = ((Number) fineAmountObj).doubleValue();
                        } else if (fineAmountObj instanceof String) {
                            try {
                                AppliedFine = Double.parseDouble((String) fineAmountObj);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Could not parse fineAmount string: " + fineAmountObj);
                            }
                        }
                        Log.d(TAG, "Retrieved fineAmount: " + AppliedFine);
                    } else {
                        Log.d(TAG, fStoreFineAmount + " field not found in document " + rideDocId);
                    }
                    String displayOtherChargeText = String.format(Locale.getDefault(), "LKR %.2f", AppliedFine);
                    txt_OtherCharge.setText(displayOtherChargeText);


                    //Total Amount
                    double initialTotalAmount = 0.0;
                    if (doc.contains(fStoreInitialTotalAmount)) {
                        Object initialAmountObj = doc.get(fStoreInitialTotalAmount);
                        if (initialAmountObj instanceof String) {
                            try {
                                initialTotalAmount = Double.parseDouble((String) initialAmountObj);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Invalid number format for '" + fStoreInitialTotalAmount + "': " + initialAmountObj);
                            }
                        } else if (initialAmountObj instanceof Number) {
                            initialTotalAmount = ((Number) initialAmountObj).doubleValue();
                        }
                    } else {
                        Log.w(TAG, "'" + fStoreInitialTotalAmount + "' field not found. Assuming 0 for initial amount.");
                    }


                    //Calculate Final Total Due
                    double finalTotalDue = initialTotalAmount + calculatedAutomatedExtraCharge + AppliedFine;

                    if (finalTotalDue > 150) {
                        finalTotalDue = calculatedAutomatedExtraCharge + AppliedFine;
                    }
                    String displayTotalDueText = String.format(Locale.getDefault(), "LKR %.2f", finalTotalDue);
                    txt_TotalCharge.setText(displayTotalDueText);


                    //Payment Button Visibility
                    if (finalTotalDue > 0) {
                        btn_ExtaPay.setVisibility(View.VISIBLE);
                    } else {
                        btn_ExtaPay.setVisibility(View.INVISIBLE);
                        Toast.makeText(ExtraCharges.this, "No amount due for payment.", Toast.LENGTH_LONG).show();
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching ride history for display", e);
                    updateDisplay("Error loading data", "LKR 0.00", "LKR 0.00", "LKR 0.00");
                    btn_ExtaPay.setVisibility(View.INVISIBLE);
                    Toast.makeText(ExtraCharges.this, "Failed to load ride details.", Toast.LENGTH_SHORT).show();
                });
    }

    private double calculateAutomatedExtraChargeValue(Long extraTimeSecondsRaw) {
        if (extraTimeSecondsRaw == null) return 0.0;
        double calculatedCharge = 0.0;
        long chargeableExtraSeconds = extraTimeSecondsRaw - gracePeriodSec;
        if (chargeableExtraSeconds > 0) {
            double chargeableExtraMinutes = chargeableExtraSeconds / 60.0;
            calculatedCharge = chargeableExtraMinutes * chargePerMin;
            if (calculatedCharge > 0 && calculatedCharge < minimumCharge) {
                calculatedCharge = minimumCharge;
            }
        }
        return Math.max(0, calculatedCharge);
    }

    private void updateDisplay(String extraTimeText, String extraChargeText, String otherChargeText, String totalDueText) {
        txt_ExtraTime.setText(extraTimeText);
        txt_ExtraTimeCharge.setText(extraChargeText);
        txt_OtherCharge.setText(otherChargeText);
        txt_TotalCharge.setText(totalDueText);
    }
}
