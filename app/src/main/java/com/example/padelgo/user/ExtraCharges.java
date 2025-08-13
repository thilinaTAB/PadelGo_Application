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

    TextView txt_ExtraTime, txt_ExtraTimeCharge, txt_TotalCharge;
    Button btn_ExtaPay;

    private FirebaseFirestore db;
    private FirebaseAuth fAuth;
    private static final String fStoreExtraChargeField = "calculatedExtraChargeAmountLKR";
    private static final String fStoreExtraTime = "extraTime";
    private static final String fStoreTotalPayment = "amount";


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
        btn_ExtaPay = findViewById(R.id.BTN_ExtraPay);

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        btn_ExtaPay.setVisibility(View.INVISIBLE);

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
            updateDisplay("User not logged in", "LKR 0.00", "LKR 0.00");
            btn_ExtaPay.setVisibility(View.INVISIBLE);
            return;
        }

        String userId = currentUser.getUid();
        db.collection("RideHistory").document(userId)
                .collection("rides").orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String rideDocId = doc.getId();

                        Long extraTimeSecondsRaw = doc.getLong(fStoreExtraTime);
                        String displayExtraTimeText = "No extra time recorded";
                        if (extraTimeSecondsRaw != null) {
                            long totalDisplayMinutes = extraTimeSecondsRaw / 60;
                            long remainingDisplaySeconds = extraTimeSecondsRaw % 60;
                            if (totalDisplayMinutes > 0) {
                                displayExtraTimeText = String.format(Locale.getDefault(), "%d min %d sec", totalDisplayMinutes, remainingDisplaySeconds);
                            } else if (extraTimeSecondsRaw > 0) {
                                displayExtraTimeText = String.format(Locale.getDefault(), "%d sec", extraTimeSecondsRaw);
                            } else {
                                displayExtraTimeText = "No recorded extra time";
                            }
                        }

                        String extraChargeAmountLKRString = doc.getString(fStoreExtraChargeField);
                        String displayExtraChargeText = "LKR 0.00";

                        if (extraChargeAmountLKRString != null && !extraChargeAmountLKRString.isEmpty()) {
                            try {
                                double amountDouble = Double.parseDouble(extraChargeAmountLKRString);
                                if (amountDouble > 0) {
                                    displayExtraChargeText = String.format(Locale.getDefault(), "LKR %.2f", amountDouble);
                                } else {
                                    displayExtraChargeText = "LKR 0.00 (No extra charge)";
                                }
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Display: Invalid number format for " + fStoreExtraChargeField + " in " + rideDocId + ": " + extraChargeAmountLKRString + ". Using client-side calc for display.");
                                displayExtraChargeText = calculateExtraChargeDisplay(extraTimeSecondsRaw);
                            }
                        } else {
                            Log.w(TAG, "Display: " + fStoreExtraChargeField + " not found in " + rideDocId + ". Using client-side calc for display.");
                            displayExtraChargeText = calculateExtraChargeDisplay(extraTimeSecondsRaw);
                        }

                        String totalAmountForPaymentGatewayStr = doc.getString(fStoreTotalPayment);
                        String displayTotalDueText = "LKR 0.00";
                        boolean canPay = false;

                        if (totalAmountForPaymentGatewayStr != null && !totalAmountForPaymentGatewayStr.isEmpty()) {
                            try {
                                double totalAmountDouble = Double.parseDouble(totalAmountForPaymentGatewayStr);
                                if (totalAmountDouble > 0) {
                                    displayTotalDueText = String.format(Locale.getDefault(), "LKR %.2f", totalAmountDouble);
                                    canPay = true;
                                } else {
                                    displayTotalDueText = "LKR 0.00 (Total amount is zero or less)";
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Invalid number format for the total amount field '" + fStoreTotalPayment + "' in Firestore for ride " + rideDocId + ": " + totalAmountForPaymentGatewayStr);
                                displayTotalDueText = "LKR ---- (Error)";
                            }
                        } else {
                            Log.w(TAG, "Total amount field '" + fStoreTotalPayment + "' is null or empty in Firestore for ride " + rideDocId);
                            displayTotalDueText = "LKR 0.00 (Total amount not found)";
                        }

                        updateDisplay(displayExtraTimeText, displayExtraChargeText, displayTotalDueText);

                        if (canPay) {
                            btn_ExtaPay.setVisibility(View.VISIBLE);
                        } else {
                            btn_ExtaPay.setVisibility(View.INVISIBLE);
                            if (totalAmountForPaymentGatewayStr == null || totalAmountForPaymentGatewayStr.isEmpty() || Double.parseDouble(totalAmountForPaymentGatewayStr) <= 0 ) {
                                Toast.makeText(ExtraCharges.this, "No amount due or amount not set for payment.", Toast.LENGTH_LONG).show();
                            }
                        }

                    } else {
                        updateDisplay("No ride data found", "LKR 0.00", "LKR 0.00");
                        btn_ExtaPay.setVisibility(View.INVISIBLE);
                        Toast.makeText(ExtraCharges.this, "No ride data found to display charges.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching ride history for display", e);
                    updateDisplay("Error loading data", "LKR 0.00", "LKR 0.00");
                    btn_ExtaPay.setVisibility(View.INVISIBLE);
                    Toast.makeText(ExtraCharges.this, "Failed to load ride details.", Toast.LENGTH_SHORT).show();
                });
    }

    private String calculateExtraChargeDisplay(Long extraTimeSecondsRaw) {
        if (extraTimeSecondsRaw == null) return "LKR 0.00 (Data N/A)";
        double calculatedCharge = 0.0;
        long chargeableExtraSeconds = extraTimeSecondsRaw - gracePeriodSec;
        if (chargeableExtraSeconds > 0) {
            double chargeableExtraMinutes = chargeableExtraSeconds / 60.0;
            calculatedCharge = chargeableExtraMinutes * chargePerMin;
            if (calculatedCharge > 0 && calculatedCharge < minimumCharge) {
                calculatedCharge = minimumCharge;
            }
        }
        return String.format(Locale.getDefault(), "LKR %.2f", calculatedCharge);
    }

    private void updateDisplay(String extraTimeText, String extraChargeText, String totalDueText) {
        txt_ExtraTime.setText(extraTimeText);
        txt_ExtraTimeCharge.setText(extraChargeText);
        txt_TotalCharge.setText(totalDueText);
    }
}
