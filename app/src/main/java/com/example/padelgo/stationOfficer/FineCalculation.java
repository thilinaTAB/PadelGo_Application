package com.example.padelgo.stationOfficer;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.padelgo.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class FineCalculation extends AppCompatActivity {

    private static final String TAG = "FineCalculation";

    private ProgressBar progressBar;
    private ScrollView scrollViewDetails;
    private TextView textViewNoPendingRides;
    private TextView textViewFullName, textViewBikeType, textViewLocation, textViewHandOverLocation, textViewPlan, textViewRideEndTime, textViewHandOverTime, textViewExtraTime, textViewExtraTimeFormatted, textViewRideStatus;
    private EditText editTextFineAmount, editTextAdminNotes;
    private Button buttonMarkAsProcessed, buttonSkipRide;

    private FirebaseFirestore db;
    private DocumentSnapshot currentAllHistoryDoc;
    private String currentRideHistoryDocId;
    private String currentRideUserId;

    private final SimpleDateFormat stringToDateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_officer_fine_calculation);

        db = FirebaseFirestore.getInstance();
        stringToDateParser.setTimeZone(TimeZone.getDefault());

        progressBar = findViewById(R.id.progressBar);
        scrollViewDetails = findViewById(R.id.scrollViewDetails);
        textViewNoPendingRides = findViewById(R.id.textViewNoPendingRides);
        textViewFullName = findViewById(R.id.textViewFullName);
        textViewBikeType = findViewById(R.id.textViewBikeType);
        textViewLocation = findViewById(R.id.textViewLocation);
        textViewHandOverLocation = findViewById(R.id.textViewHandOverLocation);
        textViewPlan = findViewById(R.id.textViewPlan);
        textViewRideEndTime = findViewById(R.id.textViewRideEndTime);
        textViewHandOverTime = findViewById(R.id.textViewHandOverTime);
        textViewExtraTime = findViewById(R.id.textViewExtraTime);
        textViewExtraTimeFormatted = findViewById(R.id.textViewExtraTimeFormatted);
        textViewRideStatus = findViewById(R.id.textViewRideStatus);

        editTextFineAmount = findViewById(R.id.editTextFineAmount);
        editTextAdminNotes = findViewById(R.id.editTextAdminNotes);
        buttonMarkAsProcessed = findViewById(R.id.buttonMarkAsProcessed);
        buttonSkipRide = findViewById(R.id.buttonSkipRide);

        buttonMarkAsProcessed.setOnClickListener(v -> processRide(true));
        buttonSkipRide.setOnClickListener(v -> processRide(false));

        fetchNextPendingRide();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);

        if (!show) {
        } else {
            scrollViewDetails.setVisibility(View.GONE);
            buttonMarkAsProcessed.setVisibility(View.GONE);
            buttonSkipRide.setVisibility(View.GONE);
            textViewNoPendingRides.setVisibility(View.GONE);
        }
    }

    private void fetchNextPendingRide() {
        showLoading(true);
        clearRideDetailsUi();
        currentAllHistoryDoc = null;
        currentRideHistoryDocId = null;
        currentRideUserId = null;

        db.collection("AllHistory").whereEqualTo("finalCalculation", "Pending").orderBy("handOverTimestamp", Query.Direction.ASCENDING).limit(1).get().addOnSuccessListener(queryDocumentSnapshots -> {
            showLoading(false);
            if (!queryDocumentSnapshots.isEmpty()) {
                currentAllHistoryDoc = queryDocumentSnapshots.getDocuments().get(0);
                currentRideUserId = currentAllHistoryDoc.getString("userId");

                Long bookingTimestampLong = currentAllHistoryDoc.getLong("bookingTimestamp");

                if (currentRideUserId != null && bookingTimestampLong != null) {
                    findRideHistoryDocumentId(currentRideUserId, bookingTimestampLong, () -> populateRideDetails(currentAllHistoryDoc));
                } else {
                    Log.w(TAG, "AllHistory doc " + currentAllHistoryDoc.getId() + " is missing userId or bookingTimestamp. Populating with available data.");
                    populateRideDetails(currentAllHistoryDoc);
                }
            } else {
                textViewNoPendingRides.setVisibility(View.VISIBLE);
                scrollViewDetails.setVisibility(View.GONE);
                buttonMarkAsProcessed.setVisibility(View.GONE);
                buttonSkipRide.setVisibility(View.GONE);
                Toast.makeText(this, "No rides pending final calculation.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            showLoading(false);
            Log.e(TAG, "Error fetching pending rides: ", e);
            Toast.makeText(FineCalculation.this, "Error fetching rides: " + e.getMessage(), Toast.LENGTH_LONG).show();
            textViewNoPendingRides.setText("Error fetching rides. Please check connection and Firestore indexes.");
            textViewNoPendingRides.setVisibility(View.VISIBLE);
        });
    }

    private void findRideHistoryDocumentId(String userId, Long bookingTimestampVal, Runnable onComplete) {
        if (userId == null || bookingTimestampVal == null) {
            Log.w(TAG, "Cannot find RideHistoryDocumentId, userId or bookingTimestampVal is null.");
            if (onComplete != null) onComplete.run();
            return;
        }
        db.collection("RideHistory").document(userId).collection("rides").whereEqualTo("bookingTimestamp", bookingTimestampVal).limit(1).get().addOnSuccessListener(rideDocs -> {
            if (!rideDocs.isEmpty()) {
                currentRideHistoryDocId = rideDocs.getDocuments().get(0).getId();
                Log.d(TAG, "Found corresponding RideHistory document ID: " + currentRideHistoryDocId);
            } else {
                Log.w(TAG, "Could not find corresponding RideHistory document for userId: " + userId + " and bookingTimestamp: " + bookingTimestampVal);
            }
            if (onComplete != null) onComplete.run();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error finding RideHistory document ID: ", e);
            if (onComplete != null) onComplete.run();
        });
    }

    private void clearRideDetailsUi() {

        textViewFullName.setText("N/A");
        textViewBikeType.setText("N/A");
        textViewLocation.setText("N/A");
        textViewHandOverLocation.setText("N/A");
        textViewPlan.setText("N/A");
        textViewRideEndTime.setText("N/A");
        textViewHandOverTime.setText("N/A");
        textViewExtraTime.setText("N/A");
        textViewExtraTimeFormatted.setText("");
        textViewRideStatus.setText("N/A");
        editTextFineAmount.setText("");
        editTextAdminNotes.setText("");
    }

    private void populateRideDetails(DocumentSnapshot doc) {
        if (doc == null) {
            Log.e(TAG, "populateRideDetails: DocumentSnapshot is null!");
            showLoading(false);
            textViewNoPendingRides.setText("Error displaying ride details.");
            textViewNoPendingRides.setVisibility(View.VISIBLE);
            scrollViewDetails.setVisibility(View.GONE);
            buttonMarkAsProcessed.setVisibility(View.GONE);
            buttonSkipRide.setVisibility(View.GONE);
            return;
        }

        currentRideUserId = doc.getString("userId");

        if (currentRideUserId != null) {
            db.collection("Users").document(currentRideUserId).get().addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    textViewFullName.setText(userDoc.getString("Full Name"));
                } else {
                    textViewFullName.setText("User details not found");
                }
            }).addOnFailureListener(e -> textViewFullName.setText("Error fetching name"));
        } else {
            textViewFullName.setText("User ID missing in ride doc");
        }

        textViewBikeType.setText(doc.getString("bikeType"));
        textViewLocation.setText(doc.getString("location"));
        textViewHandOverLocation.setText(doc.getString("handOverLocation"));
        textViewPlan.setText(doc.getString("plan"));
        textViewRideStatus.setText(doc.getString("rideStatus"));
        textViewRideEndTime.setText(getFormattedDateFromField(doc, "rideEndTime", "Ride End Time N/A"));
        textViewHandOverTime.setText(getFormattedDateFromField(doc, "handOverTimestamp", "Handover Time N/A"));


        Long extraTimeSeconds = doc.getLong("extraTime");
        if (extraTimeSeconds != null) {
            textViewExtraTime.setText(String.valueOf(extraTimeSeconds));
            textViewExtraTimeFormatted.setText(String.format(Locale.getDefault(), "(%s)", formatSecondsToDisplay(extraTimeSeconds)));
            textViewExtraTimeFormatted.setVisibility(View.VISIBLE);
        } else {
            textViewExtraTime.setText("0");
            textViewExtraTimeFormatted.setVisibility(View.GONE);
        }

        scrollViewDetails.setVisibility(View.VISIBLE);
        buttonMarkAsProcessed.setVisibility(View.VISIBLE);
        buttonSkipRide.setVisibility(View.VISIBLE);
        textViewNoPendingRides.setVisibility(View.GONE);
    }

    private String getFormattedDateFromField(DocumentSnapshot doc, String fieldName, String defaultText) {
        if (!doc.contains(fieldName)) {
            return defaultText;
        }
        Object dateObj = doc.get(fieldName);
        if (dateObj == null) {
            return defaultText;
        }

        try {
            if (dateObj instanceof Timestamp) {
                return formatFirebaseTimestamp((Timestamp) dateObj);
            } else if (dateObj instanceof String) {
                Date date = stringToDateParser.parse((String) dateObj);
                if (date != null) {
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date);
                }
            } else if (dateObj instanceof Long) {
                Date date = new Date((Long) dateObj);
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date);
            } else {
                Log.w(TAG, fieldName + " is of unexpected type: " + dateObj.getClass().getName());
                return "Unknown date format";
            }
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date string for field " + fieldName + ": " + dateObj, e);
            return "Invalid date string";
        }
        return defaultText; // Fallback
    }


    private String formatFirebaseTimestamp(Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());
            return sdf.format(timestamp.toDate());
        }
        return "N/A";
    }

    private String formatSecondsToDisplay(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void processRide(boolean applyFine) {
        if (currentAllHistoryDoc == null || currentRideUserId == null) {
            Toast.makeText(this, "No ride loaded to process.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fineAmountStr = editTextFineAmount.getText().toString().trim();
        String adminNotes = editTextAdminNotes.getText().toString().trim();
        double fineAmount = 0;

        if (applyFine) {
            if (TextUtils.isEmpty(fineAmountStr)) {
                editTextFineAmount.setError("Fine amount required or skip.");
                editTextFineAmount.requestFocus();
                return;
            }
            try {
                fineAmount = Double.parseDouble(fineAmountStr);
                if (fineAmount < 0) {
                    editTextFineAmount.setError("Fine cannot be negative.");
                    editTextFineAmount.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                editTextFineAmount.setError("Invalid fine amount.");
                editTextFineAmount.requestFocus();
                return;
            }
        }

        showLoading(true);

        WriteBatch batch = db.batch();

        Map<String, Object> allHistoryUpdates = new HashMap<>();
        allHistoryUpdates.put("finalCalculation", applyFine ? "FineApplied" : "NoFine");
        allHistoryUpdates.put("fineAmount", fineAmount);
        allHistoryUpdates.put("adminNotes", adminNotes);


        batch.update(currentAllHistoryDoc.getReference(), allHistoryUpdates);

        if (currentRideHistoryDocId != null) {
            Map<String, Object> rideHistoryUpdates = new HashMap<>();
            rideHistoryUpdates.put("finalCalculation", applyFine ? "FineApplied" : "ProcessedNoFine");
            rideHistoryUpdates.put("fineAmount", fineAmount);

            batch.update(db.collection("RideHistory").document(currentRideUserId).collection("rides").document(currentRideHistoryDocId), rideHistoryUpdates);
        } else {
            Log.w(TAG, "Skipping update to RideHistory as currentRideHistoryDocId is null. UserID: " + currentRideUserId + ", AllHistoryDocID: " + currentAllHistoryDoc.getId());
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(FineCalculation.this, "Ride processed successfully.", Toast.LENGTH_SHORT).show();
            fetchNextPendingRide();
        }).addOnFailureListener(e -> {
            showLoading(false);
            Log.e(TAG, "Error processing ride: ", e);
            Toast.makeText(FineCalculation.this, "Error processing ride: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentAllHistoryDoc == null && progressBar.getVisibility() == View.GONE && (textViewNoPendingRides.getVisibility() == View.VISIBLE || scrollViewDetails.getVisibility() == View.GONE)) {
            Log.d(TAG, "onResume: No ride data, attempting to fetch next pending ride.");
            fetchNextPendingRide();
        }
    }
}
