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
import com.google.firebase.firestore.FirebaseFirestore;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

public class PaymentGateway extends AppCompatActivity {

    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;

    //TEAM:  Replace with your current ngrok url here
    private final String backendUrl = "https://8b43-2402-d000-8120-b43f-99db-3ca5-b5b8-d021.ngrok-free.app";
    private final String publishableKey = "pk_test_51RbjciR9H2dk7jjUA7WKgDP1rQe0xCffEPLBBeoS2Bna0MYPBaqfeG8m5HFVnJs2bZBM81HepLvKJQIAHEEJWOcN00FBwPERRW";

    private Button btn_Pay;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth fAuth;

    private int rideAmountInCents = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_payment_gateway);

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

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

        db.collection("RideHistory")
                .document(userId)
                .collection("rides")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String amountStr = queryDocumentSnapshots.getDocuments().get(0).getString("amount");

                        try {
                            double amount = Double.parseDouble(amountStr);
                            rideAmountInCents = (int) (amount * 100); // Convert to cents
                            createPaymentIntent(rideAmountInCents);
                        } catch (Exception e) {
                            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                            Log.e("AmountFetch", "Error parsing amount", e);
                        }
                    } else {
                        Toast.makeText(this, "No recent ride found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching ride data", Toast.LENGTH_SHORT).show();
                    Log.e("AmountFetch", "Firestore error", e);
                });
    }

    private void createPaymentIntent(int amount) {
        progressBar.setVisibility(View.VISIBLE);
        btn_Pay.setEnabled(false);

        String url = backendUrl + "/api/payments/create-payment-intent";

        JSONObject paymentData = new JSONObject();
        try {
            paymentData.put("amount", amount);
        } catch (JSONException e) {
            Log.e("StripePayment", "JSON Error", e);
            progressBar.setVisibility(View.GONE);
            btn_Pay.setEnabled(true);
            Toast.makeText(this, "Error preparing payment", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                paymentData,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    btn_Pay.setEnabled(true);
                    try {
                        paymentIntentClientSecret = response.getString("clientSecret");
                        presentPaymentSheet(paymentIntentClientSecret);
                    } catch (JSONException e) {
                        Log.e("StripePayment", "Client secret parse error", e);
                        Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btn_Pay.setEnabled(true);
                    if (error.networkResponse != null) {
                        Log.e("StripePayment", "Server Error " + error.networkResponse.statusCode, error);
                        Toast.makeText(this, "Server error: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    } else if (error instanceof NoConnectionError) {
                        Log.e("StripePayment", "No internet", error);
                        Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("StripePayment", "Volley Error", error);
                        Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Missing payment secret", Toast.LENGTH_LONG).show();
            Log.e("StripePayment", "Client secret is null or empty");
            return;
        }

        PaymentSheet.Configuration config = new PaymentSheet.Configuration.Builder("PadelGo Rentals")
                .allowsDelayedPaymentMethods(false)
                .build();

        paymentSheet.presentWithPaymentIntent(clientSecret, config);
    }

    private void onPaymentSheetResult(final PaymentSheetResult result) {
        btn_Pay.setEnabled(true);
        progressBar.setVisibility(View.GONE);

        if (result instanceof PaymentSheetResult.Completed) {
            Toast.makeText(this, "Payment Successful ✅", Toast.LENGTH_LONG).show();
            Log.i("StripePayment", "Payment completed");

            // ✅ Update Firestore payment status
            FirebaseUser currentUser = fAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                db.collection("RideHistory")
                        .document(userId)
                        .collection("rides")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                String docId = querySnapshot.getDocuments().get(0).getId();

                                db.collection("RideHistory")
                                        .document(userId)
                                        .collection("rides")
                                        .document(docId)
                                        .update("payment", "Paid")
                                        .addOnSuccessListener(unused -> {
                                            Log.i("FirestoreUpdate", "Payment marked as Paid");
                                            // ✅ Redirect to dashboard
                                            startActivity(new Intent(PaymentGateway.this, UserProfile.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FirestoreUpdate", "Failed to update payment status", e);
                                            Toast.makeText(this, "Payment done, but failed to update Firestore", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreRead", "Error fetching latest ride for update", e);
                            Toast.makeText(this, "Payment done, but couldn't find ride to update", Toast.LENGTH_SHORT).show();
                        });
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
