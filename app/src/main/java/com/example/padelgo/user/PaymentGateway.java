package com.example.padelgo.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.padelgo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap; //ADDED for map updates
import java.util.Map;     //ADDED for map updates

public class PaymentGateway extends AppCompatActivity {

    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;

    //TEAM:  Replace with your current ngrok url here
    private final String backendUrl = "https://edf8239eee1a.ngrok-free.app";
    private final String publishableKey = "pk_test_51RbjciR9H2dk7jjUA7WKgDP1rQe0xCffEPLBBeoS2Bna0MYPBaqfeG8m5HFVnJs2bZBM81HepLvKJQIAHEEJWOcN00FBwPERRW";

    private Button btn_Pay;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth fAuth;
    private DatabaseReference realtimeDB;

    private int rideAmountInCents = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_payment_gateway);

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        realtimeDB = FirebaseDatabase.getInstance().getReference();

        progressBar = findViewById(R.id.progressBar);
        btn_Pay = findViewById(R.id.btnPay);

        try {
            PaymentConfiguration.init(getApplicationContext(), publishableKey);
        } catch (IllegalStateException e) {
            Log.e("StripePayment", "Stripe SDK initialization failed", e);
            Toast.makeText(this, "Error initializing Stripe", Toast.LENGTH_LONG).show();
            btn_Pay.setEnabled(false);
            return;
        }

        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        btn_Pay.setOnClickListener(view -> {
            fetchLatestRideAmount();
        });
    }

    private void fetchLatestRideAmount() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        // Show progress and disable button while fetching
        progressBar.setVisibility(View.VISIBLE);
        btn_Pay.setEnabled(false);

        db.collection("RideHistory")
                .document(userId)
                .collection("rides")
                .orderBy("serverTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot latestRideDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String amountStr = latestRideDoc.getString("amount");

                        try {
                            if (amountStr != null && !amountStr.isEmpty()) {
                                double amount = Double.parseDouble(amountStr);
                                rideAmountInCents = (int) (amount * 100); // Convert to cents
                                if (rideAmountInCents > 0) {
                                    createPaymentIntent(rideAmountInCents);
                                } else {
                                    Toast.makeText(this, "Amount for payment is zero or invalid.", Toast.LENGTH_SHORT).show();
                                    Log.w("AmountFetch", "Fetched amount is zero or negative: " + amountStr);
                                    resetButtonAndProgressState();
                                }
                            } else {
                                Toast.makeText(this, "Amount not found for the latest ride.", Toast.LENGTH_SHORT).show();
                                Log.w("AmountFetch", "'amount' field is null or empty in the latest ride document.");
                                resetButtonAndProgressState();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid amount format in ride data.", Toast.LENGTH_SHORT).show();
                            Log.e("AmountFetch", "Error parsing amount: " + amountStr, e);
                            resetButtonAndProgressState();
                        }
                    } else {
                        Toast.makeText(this, "No recent ride found to pay for.", Toast.LENGTH_SHORT).show();
                        Log.w("AmountFetch", "No ride documents found for user " + userId);
                        resetButtonAndProgressState();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching ride data for payment.", Toast.LENGTH_SHORT).show();
                    Log.e("AmountFetch", "Firestore error fetching ride data", e);
                    resetButtonAndProgressState();
                });
    }

    private void resetButtonAndProgressState() {
        if(progressBar != null) progressBar.setVisibility(View.GONE);
        if(btn_Pay != null) btn_Pay.setEnabled(true);
    }

    private void createPaymentIntent(int amount) {

        String url = backendUrl + "/api/payments/create-payment-intent";

        JSONObject paymentData = new JSONObject();
        try {
            paymentData.put("amount", amount);
        } catch (JSONException e) {
            Log.e("StripePayment", "JSON Error creating payment data", e);
            resetButtonAndProgressState(); // Enable button and hide progress on error
            Toast.makeText(this, "Error preparing payment details", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                paymentData,
                response -> {
                    try {
                        paymentIntentClientSecret = response.getString("clientSecret");
                        presentPaymentSheet(paymentIntentClientSecret);
                    } catch (JSONException e) {
                        Log.e("StripePayment", "Client secret parse error", e);
                        Toast.makeText(this, "Payment failed: Server response error", Toast.LENGTH_SHORT).show();
                        resetButtonAndProgressState();
                    }
                },
                error -> {
                    resetButtonAndProgressState();
                    if (error.networkResponse != null) {
                        Log.e("StripePayment", "Server Error " + error.networkResponse.statusCode + " creating payment intent", error);
                        Toast.makeText(this, "Server error: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    } else if (error instanceof NoConnectionError) {
                        Log.e("StripePayment", "No internet connection for payment intent", error);
                        Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("StripePayment", "Volley Error creating payment intent", error);
                        Toast.makeText(this, "Network error during payment setup", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void presentPaymentSheet(String clientSecret) {
        if (clientSecret == null || clientSecret.isEmpty()) {
            Toast.makeText(this, "Missing payment secret, cannot proceed.", Toast.LENGTH_LONG).show();
            Log.e("StripePayment", "Client secret is null or empty before presenting sheet.");
            resetButtonAndProgressState();
            return;
        }

        PaymentSheet.Configuration config = new PaymentSheet.Configuration.Builder("PadelGo Rentals")
                .allowsDelayedPaymentMethods(false)
                .build();

        paymentSheet.presentWithPaymentIntent(clientSecret, config);
    }

    private void onPaymentSheetResult(final PaymentSheetResult result) {
        resetButtonAndProgressState();

        if (result instanceof PaymentSheetResult.Completed) {
            Toast.makeText(this, "Payment Successful ✅", Toast.LENGTH_LONG).show();
            Log.i("StripePayment", "Payment completed");

            FirebaseUser currentUser = fAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                db.collection("RideHistory")
                        .document(userId)
                        .collection("rides")
                        .orderBy("serverTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(rideHistorySnapshot -> {
                            if (!rideHistorySnapshot.isEmpty()) {
                                DocumentSnapshot rideHistoryDoc = rideHistorySnapshot.getDocuments().get(0);
                                String rideHistoryDocId = rideHistoryDoc.getId();
                                Long bookingTimestamp = rideHistoryDoc.getLong("bookingTimestamp");
                                boolean rideStartRequestValue = false; // Standard value after payment

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("payment", "Paid");
                                updates.put("rideStartRequest", rideStartRequestValue);

                                if (rideHistoryDoc.contains("extraPay") && "Unpaid".equals(rideHistoryDoc.getString("extraPay"))) {
                                    updates.put("extraPay", "Paid");
                                    Log.i("FirestoreUpdate", "Updating 'extraPay' to 'Paid' for doc: " + rideHistoryDocId);
                                }

                                db.collection("RideHistory")
                                        .document(userId)
                                        .collection("rides")
                                        .document(rideHistoryDocId)
                                        .update(updates) // Use the map for updates
                                        .addOnSuccessListener(unused -> {
                                            Log.i("FirestoreUpdate", "RideHistory updated successfully for doc: " + rideHistoryDocId + " with updates: " + updates.toString());

                                            if (bookingTimestamp != null) {
                                                Map<String, Object> allHistoryUpdates = new HashMap<>();
                                                if(updates.containsKey("payment")) allHistoryUpdates.put("payment", updates.get("payment"));
                                                if(updates.containsKey("extraPay")) allHistoryUpdates.put("extraPay", updates.get("extraPay")); // Propagate extraPay
                                                if(updates.containsKey("rideStartRequest")) allHistoryUpdates.put("rideStartRequest", updates.get("rideStartRequest"));


                                                if (!allHistoryUpdates.isEmpty()) {
                                                    db.collection("AllHistory")
                                                            .whereEqualTo("userId", userId)
                                                            .whereEqualTo("bookingTimestamp", bookingTimestamp)
                                                            .limit(1)
                                                            .get()
                                                            .addOnSuccessListener(allHistorySnapshot -> {
                                                                if (!allHistorySnapshot.isEmpty()) {
                                                                    String allHistoryDocId = allHistorySnapshot.getDocuments().get(0).getId();
                                                                    db.collection("AllHistory").document(allHistoryDocId)
                                                                            .update(allHistoryUpdates) // Update AllHistory with relevant fields
                                                                            .addOnSuccessListener(aVoid1 -> Log.i("FirestoreUpdate", "Payment status marked in AllHistory for doc: " + allHistoryDocId))
                                                                            .addOnFailureListener(e1 -> Log.e("FirestoreUpdate", "Failed to update payment status in AllHistory for doc: " + allHistoryDocId, e1));
                                                                } else {
                                                                    Log.w("FirestoreUpdate", "Could not find matching document in AllHistory to update. UserID: " + userId + ", BookingTimestamp: " + bookingTimestamp);
                                                                }
                                                            })
                                                            .addOnFailureListener(e1 -> Log.e("FirestoreUpdate", "Error querying AllHistory", e1));
                                                }
                                            } else {
                                                Log.w("FirestoreUpdate", "bookingTimestamp is null in RideHistory doc (" + rideHistoryDocId + "), cannot accurately update AllHistory.");
                                            }

                                            DatabaseReference releaseRef = realtimeDB.child("release_bicycle").child(userId);
                                            releaseRef.child("rideStartRequest").setValue(rideStartRequestValue)
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        Log.i("RealtimeDBUpdate", "rideStartRequest status saved to Realtime Database");
                                                    })
                                                    .addOnFailureListener(e2 -> {
                                                        Log.e("RealtimeDBUpdate", "Failed to save rideStartRequest to Realtime Database", e2);
                                                        Toast.makeText(this, "Payment & Firestore updated, but failed to update Realtime DB.", Toast.LENGTH_LONG).show();
                                                    })
                                                    .addOnCompleteListener(task -> { // Use onCompleteListener to navigate after attempt
                                                        startActivity(new Intent(PaymentGateway.this, MyRides.class));
                                                        finish();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FirestoreUpdate", "Failed to update payment status in RideHistory for doc: " + rideHistoryDocId, e);
                                            Toast.makeText(this, "Payment successful, but failed to update RideHistory.", Toast.LENGTH_LONG).show();
                                            startActivity(new Intent(PaymentGateway.this, MyRides.class));
                                            finish();
                                        });

                            } else {
                                Log.w("FirestoreUpdate", "No ride document found in RideHistory to update after payment.");
                                Toast.makeText(this, "Payment successful, but no ride record found to update.", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(PaymentGateway.this, MyRides.class));
                                finish();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreRead", "Error fetching latest ride from RideHistory for update after payment", e);
                            Toast.makeText(this, "Payment successful, but couldn't find ride to update its details.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(PaymentGateway.this, MyRides.class));
                            finish();
                        });
            } else {
                Log.w("StripePayment", "Current user is null after payment completion.");
                Toast.makeText(this, "Payment successful, but user session lost.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(PaymentGateway.this, UserDashboard.class));
                finish();
            }

        } else if (result instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Payment Canceled ❌", Toast.LENGTH_LONG).show();
            Log.i("StripePayment", "Payment canceled");
        } else if (result instanceof PaymentSheetResult.Failed) {
            Toast.makeText(this, "Payment Failed ❌", Toast.LENGTH_LONG).show();
            Log.e("StripePayment", "Payment failed", ((PaymentSheetResult.Failed) result).getError());
        }
    }
}
