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
    private static final int GRACE_PERIOD_SECONDS = 900;
    private static final double CHARGE_PER_MINUTE_AFTER_GRACE = 0.25;
    private static final double FIXED_CHARGE_IF_ANY_PER_MINUTE_CHARGE_IS_LESS_THAN_THIS = 150.0;

    TextView txt_DisplayExtraTime, txt_CalculatedExtraChargeDisplay, txt_TotalDueDisplay;
    Button btn_ProceedToPaymentGateway;

    private FirebaseFirestore db;
    private FirebaseAuth fAuth;
    private static final String FIRESTORE_EXTRA_CHARGE_FIELD_FOR_DISPLAY = "calculatedExtraChargeAmountLKR";
    private static final String FIRESTORE_EXTRA_TIME_SECONDS_FIELD = "extraTime";
    private static final String FIRESTORE_TOTAL_AMOUNT_FOR_PAYMENT_GATEWAY = "amount";


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

        txt_DisplayExtraTime = findViewById(R.id.TXT_ExtraTime);
        txt_CalculatedExtraChargeDisplay = findViewById(R.id.TXT_ExtraTimeCharge);
        txt_TotalDueDisplay = findViewById(R.id.TXT_TotalCharge);
        btn_ProceedToPaymentGateway = findViewById(R.id.BTN_ExtraPay);

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        btn_ProceedToPaymentGateway.setVisibility(View.INVISIBLE); // Initially hide

        btn_ProceedToPaymentGateway.setOnClickListener(v -> {
            Log.d(TAG, "Proceed to Payment Gateway button clicked.");
            Intent paymentIntent = new Intent(ExtraCharges.this, PaymentGateway.class);
            startActivity(paymentIntent);
            finish();
        });

        loadRideDetailsForDisplay();
    }

    private void loadRideDetailsForDisplay() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser == null) {
            updateDisplay("User not logged in", "LKR 0.00", "LKR 0.00");
            btn_ProceedToPaymentGateway.setVisibility(View.INVISIBLE);
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

                        // 1. Display Extra Time
                        Long extraTimeSecondsRaw = doc.getLong(FIRESTORE_EXTRA_TIME_SECONDS_FIELD);
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

                        String extraChargeAmountLKRString = doc.getString(FIRESTORE_EXTRA_CHARGE_FIELD_FOR_DISPLAY);
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
                                Log.w(TAG, "Display: Invalid number format for " + FIRESTORE_EXTRA_CHARGE_FIELD_FOR_DISPLAY + " in " + rideDocId + ": " + extraChargeAmountLKRString + ". Using client-side calc for display.");
                                displayExtraChargeText = calculateClientSideExtraChargeForDisplayOnly(extraTimeSecondsRaw);
                            }
                        } else {
                            Log.w(TAG, "Display: " + FIRESTORE_EXTRA_CHARGE_FIELD_FOR_DISPLAY + " not found in " + rideDocId + ". Using client-side calc for display.");
                            displayExtraChargeText = calculateClientSideExtraChargeForDisplayOnly(extraTimeSecondsRaw);
                        }

                        String totalAmountForPaymentGatewayStr = doc.getString(FIRESTORE_TOTAL_AMOUNT_FOR_PAYMENT_GATEWAY);
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
                                Log.e(TAG, "Invalid number format for the total amount field '" + FIRESTORE_TOTAL_AMOUNT_FOR_PAYMENT_GATEWAY + "' in Firestore for ride " + rideDocId + ": " + totalAmountForPaymentGatewayStr);
                                displayTotalDueText = "LKR ---- (Error)";
                            }
                        } else {
                            Log.w(TAG, "Total amount field '" + FIRESTORE_TOTAL_AMOUNT_FOR_PAYMENT_GATEWAY + "' is null or empty in Firestore for ride " + rideDocId);
                            displayTotalDueText = "LKR 0.00 (Total amount not found)";
                        }

                        updateDisplay(displayExtraTimeText, displayExtraChargeText, displayTotalDueText);

                        if (canPay) {
                            btn_ProceedToPaymentGateway.setVisibility(View.VISIBLE);
                        } else {
                            btn_ProceedToPaymentGateway.setVisibility(View.INVISIBLE);
                            if (totalAmountForPaymentGatewayStr == null || totalAmountForPaymentGatewayStr.isEmpty() || Double.parseDouble(totalAmountForPaymentGatewayStr) <= 0 ) {
                                Toast.makeText(ExtraCharges.this, "No amount due or amount not set for payment.", Toast.LENGTH_LONG).show();
                            }
                        }

                    } else {
                        updateDisplay("No ride data found", "LKR 0.00", "LKR 0.00");
                        btn_ProceedToPaymentGateway.setVisibility(View.INVISIBLE);
                        Toast.makeText(ExtraCharges.this, "No ride data found to display charges.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching ride history for display", e);
                    updateDisplay("Error loading data", "LKR 0.00", "LKR 0.00");
                    btn_ProceedToPaymentGateway.setVisibility(View.INVISIBLE);
                    Toast.makeText(ExtraCharges.this, "Failed to load ride details.", Toast.LENGTH_SHORT).show();
                });
    }

    private String calculateClientSideExtraChargeForDisplayOnly(Long extraTimeSecondsRaw) {
        if (extraTimeSecondsRaw == null) return "LKR 0.00 (Data N/A)";
        double calculatedCharge = 0.0;
        long chargeableExtraSeconds = extraTimeSecondsRaw - GRACE_PERIOD_SECONDS;
        if (chargeableExtraSeconds > 0) {
            double chargeableExtraMinutes = chargeableExtraSeconds / 60.0;
            calculatedCharge = chargeableExtraMinutes * CHARGE_PER_MINUTE_AFTER_GRACE;
            if (calculatedCharge > 0 && calculatedCharge < FIXED_CHARGE_IF_ANY_PER_MINUTE_CHARGE_IS_LESS_THAN_THIS) {
                calculatedCharge = FIXED_CHARGE_IF_ANY_PER_MINUTE_CHARGE_IS_LESS_THAN_THIS;
            }
        }
        return String.format(Locale.getDefault(), "LKR %.2f", calculatedCharge);
    }

    private void updateDisplay(String extraTimeText, String extraChargeText, String totalDueText) {
        txt_DisplayExtraTime.setText(extraTimeText);
        txt_CalculatedExtraChargeDisplay.setText(extraChargeText);
        txt_TotalDueDisplay.setText(totalDueText);
    }
}
