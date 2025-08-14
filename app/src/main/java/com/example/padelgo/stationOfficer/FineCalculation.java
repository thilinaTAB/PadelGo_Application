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
    private TextView txt_NoPendingRides;
    private TextView txt_userFullName, txt_RideBikeType, txt_StationLocation, txt_HandOverLocation, txt_RidePlan, txt_RideEndTime, txt_HandOverTime, txt_ExtraTime, txt_ExtraTimeFormatted, txt_RideStatus;
    private EditText etxt_FineAmount;
    private Button btn_MarkAsProcessed, btn_SkipRide;

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
        txt_NoPendingRides = findViewById(R.id.TXT_NoPendingRides);
        txt_userFullName = findViewById(R.id.TXT_UserFullName);
        txt_RideBikeType = findViewById(R.id.TXT_RideBikeType);
        txt_StationLocation = findViewById(R.id.TXT_StationLocation);
        txt_HandOverLocation = findViewById(R.id.TXT_HandOverLocation);
        txt_RidePlan = findViewById(R.id.TXT_RidePlan);
        txt_RideEndTime = findViewById(R.id.TXT_RideEndTime);
        txt_HandOverTime = findViewById(R.id.TXT_HandOverTime);
        txt_ExtraTime = findViewById(R.id.TXT_ExtraTimeUsed);
        txt_ExtraTimeFormatted = findViewById(R.id.TXT_ExtraTimeFormatted);
        txt_RideStatus = findViewById(R.id.TXT_RideStatus);

        etxt_FineAmount = findViewById(R.id.ETXT_FineAmount);
        btn_MarkAsProcessed = findViewById(R.id.BTN_MarkAsProcessed);
        btn_SkipRide = findViewById(R.id.BTN_SkipRide);

        btn_MarkAsProcessed.setOnClickListener(v -> processRide(true));
        btn_SkipRide.setOnClickListener(v -> processRide(false));

        fetchNextPendingRide();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);

        if (!show) {
        } else {
            scrollViewDetails.setVisibility(View.GONE);
            btn_MarkAsProcessed.setVisibility(View.GONE);
            btn_SkipRide.setVisibility(View.GONE);
            txt_NoPendingRides.setVisibility(View.GONE);
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
                txt_NoPendingRides.setVisibility(View.VISIBLE);
                scrollViewDetails.setVisibility(View.GONE);
                btn_MarkAsProcessed.setVisibility(View.GONE);
                btn_SkipRide.setVisibility(View.GONE);
                Toast.makeText(this, "No rides pending final calculation.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            showLoading(false);
            Log.e(TAG, "Error fetching pending rides: ", e);
            Toast.makeText(FineCalculation.this, "Error fetching rides: " + e.getMessage(), Toast.LENGTH_LONG).show();
            txt_NoPendingRides.setText("Error fetching rides. Please check connection and Firestore indexes.");
            txt_NoPendingRides.setVisibility(View.VISIBLE);
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

        txt_userFullName.setText("N/A");
        txt_RideBikeType.setText("N/A");
        txt_StationLocation.setText("N/A");
        txt_HandOverLocation.setText("N/A");
        txt_RidePlan.setText("N/A");
        txt_RideEndTime.setText("N/A");
        txt_HandOverTime.setText("N/A");
        txt_ExtraTime.setText("N/A");
        txt_ExtraTimeFormatted.setText("");
        txt_RideStatus.setText("N/A");
        etxt_FineAmount.setText("");
    }

    private void populateRideDetails(DocumentSnapshot doc) {
        if (doc == null) {
            Log.e(TAG, "populateRideDetails: DocumentSnapshot is null!");
            showLoading(false);
            txt_NoPendingRides.setText("Error displaying ride details.");
            txt_NoPendingRides.setVisibility(View.VISIBLE);
            scrollViewDetails.setVisibility(View.GONE);
            btn_MarkAsProcessed.setVisibility(View.GONE);
            btn_SkipRide.setVisibility(View.GONE);
            return;
        }

        currentRideUserId = doc.getString("userId");

        if (currentRideUserId != null) {
            db.collection("Users").document(currentRideUserId).get().addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    txt_userFullName.setText(userDoc.getString("Full Name"));
                } else {
                    txt_userFullName.setText("User details not found");
                }
            }).addOnFailureListener(e -> txt_userFullName.setText("Error fetching name"));
        } else {
            txt_userFullName.setText("User ID missing in ride doc");
        }

        txt_RideBikeType.setText(doc.getString("bikeType"));
        txt_StationLocation.setText(doc.getString("location"));
        txt_HandOverLocation.setText(doc.getString("handOverLocation"));
        txt_RidePlan.setText(doc.getString("plan"));
        txt_RideStatus.setText(doc.getString("rideStatus"));
        txt_RideEndTime.setText(getFormattedDateFromField(doc, "rideEndTime", "Ride End Time N/A"));
        txt_HandOverTime.setText(getFormattedDateFromField(doc, "handOverTimestamp", "Handover Time N/A"));


        Long extraTimeSeconds = doc.getLong("extraTime");
        if (extraTimeSeconds != null) {
            txt_ExtraTime.setText(String.valueOf(extraTimeSeconds));
            txt_ExtraTimeFormatted.setText(String.format(Locale.getDefault(), "(%s)", formatSecondsToDisplay(extraTimeSeconds)));
            txt_ExtraTimeFormatted.setVisibility(View.VISIBLE);
        } else {
            txt_ExtraTime.setText("0");
            txt_ExtraTimeFormatted.setVisibility(View.GONE);
        }

        scrollViewDetails.setVisibility(View.VISIBLE);
        btn_MarkAsProcessed.setVisibility(View.VISIBLE);
        btn_SkipRide.setVisibility(View.VISIBLE);
        txt_NoPendingRides.setVisibility(View.GONE);
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

        String fineAmountStr = etxt_FineAmount.getText().toString().trim();
        double fineAmount = 0;

        if (applyFine) {
            if (TextUtils.isEmpty(fineAmountStr)) {
                etxt_FineAmount.setError("Fine amount required or skip.");
                etxt_FineAmount.requestFocus();
                return;
            }
            try {
                fineAmount = Double.parseDouble(fineAmountStr);
                if (fineAmount < 0) {
                    etxt_FineAmount.setError("Fine cannot be negative.");
                    etxt_FineAmount.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etxt_FineAmount.setError("Invalid fine amount.");
                etxt_FineAmount.requestFocus();
                return;
            }
        }

        showLoading(true);

        WriteBatch batch = db.batch();

        Map<String, Object> allHistoryUpdates = new HashMap<>();
        allHistoryUpdates.put("finalCalculation", applyFine ? "FineApplied" : "NoFine");
        allHistoryUpdates.put("fineAmount", fineAmount);


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
        if (currentAllHistoryDoc == null && progressBar.getVisibility() == View.GONE && (txt_NoPendingRides.getVisibility() == View.VISIBLE || scrollViewDetails.getVisibility() == View.GONE)) {
            Log.d(TAG, "onResume: No ride data, attempting to fetch next pending ride.");
            fetchNextPendingRide();
        }
    }
}
