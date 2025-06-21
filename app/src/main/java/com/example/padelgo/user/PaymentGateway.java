package com.example.padelgo.user;

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
import com.stripe.android.PaymentConfiguration;

import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

public class PaymentGateway extends AppCompatActivity {

    private PaymentSheet paymentSheet;

    private String paymentIntentClientSecret;
    // It's good practice to ensure this URL is correct, especially if your tunnel restarts
    private final String backendUrl = "https://1d1bb460571c8f.lhr.life"; // Your public tunnel URL
    private final String publishableKey = "pk_test_51RbjciR9H2dk7jjUA7WKgDP1rQe0xCffEPLBBeoS2Bna0MYPBaqfeG8m5HFVnJs2bZBM81HepLvKJQIAHEEJWOcN00FBwPERRW"; // Replace with your real key

    private Button btnPay;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_payment_gateway); // Ensure this layout has a ProgressBar with id progressBar

        progressBar = findViewById(R.id.progressBar); // Initialize ProgressBar
        btnPay = findViewById(R.id.btnPay);

        // Initialize Stripe SDK
        try {
            PaymentConfiguration.init(getApplicationContext(), publishableKey);
        } catch (IllegalStateException e) {
            Log.e("StripePayment", "Stripe SDK already initialized or invalid publishable key.", e);
            // Handle error, maybe show a message to the user or disable payment functionality
            Toast.makeText(this, "Error initializing payment SDK.", Toast.LENGTH_LONG).show();
            btnPay.setEnabled(false); // Disable pay button if SDK init fails
            return;
        }

        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        btnPay.setOnClickListener(view -> {
            // You might want to get the amount dynamically or validate it here
            createPaymentIntent(1500); // Example: ₹15.00 (amount in smallest currency unit)
        });
    }

    private void createPaymentIntent(int amount) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnPay.setEnabled(false);

        String url = backendUrl + "/api/payments/create-payment-intent";

        JSONObject paymentData = new JSONObject();
        try {
            paymentData.put("amount", amount);
            // You might need to send currency from the client if your backend supports multiple
            // paymentData.put("currency", "inr"); // Example: if backend expects it
        } catch (JSONException e) {
            Log.e("StripePayment", "Error creating paymentData JSON", e);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            btnPay.setEnabled(true);
            Toast.makeText(this, "Local error preparing payment.", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                paymentData,
                response -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    btnPay.setEnabled(true);
                    try {
                        paymentIntentClientSecret = response.getString("clientSecret");
                        Log.i("StripePayment", "Successfully received client secret.");
                        presentPaymentSheet(paymentIntentClientSecret);
                    } catch (JSONException e) {
                        Log.e("StripePayment", "Failed to parse client secret from response", e);
                        Toast.makeText(this, "Error processing payment response.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    btnPay.setEnabled(true);
                    String errorMessage = "Network error. Please try again.";
                    if (error.networkResponse != null) {
                        errorMessage = "Server error " + error.networkResponse.statusCode + ". Please try again later.";
                        Log.e("StripePayment", "Server Error: " + error.networkResponse.statusCode + " for URL: " + url, error);
                    } else if (error instanceof NoConnectionError) {
                        errorMessage = "No internet connection. Please check your network.";
                        Log.e("StripePayment", "NoConnectionError for URL: " + url, error);
                    } else {
                        Log.e("StripePayment", "Volley Error creating PaymentIntent for URL: " + url, error);
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
        );

        // Consider making timeout and retries configurable or constants
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, // Increased timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // Default is 1
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void presentPaymentSheet(String clientSecret) {
        if (clientSecret == null || clientSecret.isEmpty()) {
            Toast.makeText(this, "Cannot proceed with payment: missing client secret.", Toast.LENGTH_LONG).show();
            Log.e("StripePayment", "Client secret is null or empty before presenting PaymentSheet.");
            return;
        }

        PaymentSheet.Configuration.Builder configBuilder =
                new PaymentSheet.Configuration.Builder("PadelGo Rentals") // Your merchant name
                        .allowsDelayedPaymentMethods(false);

        // Example: If you were using customer sessions
        // if (customerConfig != null) {
        // configBuilder.customer(customerConfig);
        // }

        // Example: Setting default billing details (optional)
        // PaymentSheet.BillingDetails defaultBillingDetails = new PaymentSheet.BillingDetails.Builder()
        // .name("John Doe") // Prefill name
        // .email("john.doe@example.com") // Prefill email
        // // Address, phone can also be prefilled
        // .build();
        // configBuilder.defaultBillingDetails(defaultBillingDetails);

        // Example: Setting appearance (optional)
        // PaymentSheet.Appearance appearance = new PaymentSheet.Appearance.Builder()
        // .colorsLight(new PaymentSheet.Colors(
        // Color.BLUE, // primary
        // Color.WHITE, // componentBackground
        // Color.LTGRAY // componentBorder
        // ))
        // .build();
        // configBuilder.appearance(appearance);

        paymentSheet.presentWithPaymentIntent(clientSecret, configBuilder.build());
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        // Re-enable pay button regardless of result, so user can retry if necessary (e.g. after cancellation)
        btnPay.setEnabled(true);
        if (progressBar != null) progressBar.setVisibility(View.GONE);


        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(this, "Payment Successful ✅", Toast.LENGTH_LONG).show();
            Log.i("StripePayment", "PaymentSheetResult: Completed");
            // TODO: Navigate to a success screen or update UI
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Payment Canceled ❌", Toast.LENGTH_LONG).show();
            Log.i("StripePayment", "PaymentSheetResult: Canceled");
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            PaymentSheetResult.Failed failedResult = (PaymentSheetResult.Failed) paymentSheetResult;
            Toast.makeText(this, "Payment Failed: " + failedResult.getError().getLocalizedMessage(), Toast.LENGTH_LONG).show();
            Log.e("StripePayment", "PaymentSheetResult: Failed", failedResult.getError());
        }
    }
}