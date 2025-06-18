package com.example.padelgo.user;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.padelgo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RideConfirmation extends AppCompatActivity {

    TextView txt_bikeType, txt_location, txt_plan, txt_amount;
    EditText etxt_date, etxt_time, etxt_numPlan;
    Button btn_confirm, btn_cancel;
    Calendar calendar = Calendar.getInstance();
    private FirebaseFirestore db;
    private FirebaseAuth fAuth;
    private DatabaseReference liveDatabase;
    private String bikeTypeToUpdate;
    private static final String TAG = "RideConfirmation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_ride_confirmation);
        String location = getIntent().getStringExtra("Location");
        if (location != null && !location.isEmpty()) {
            liveDatabase = FirebaseDatabase.getInstance().getReference("bicycleAvailability_" + location);
        } else {
            Log.e(TAG, "Location not provided");
        }


        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showBackConfirmationDialog();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        txt_bikeType = findViewById(R.id.TXT_Bicycle);
        txt_location = findViewById(R.id.TXT_Location);
        etxt_date = findViewById(R.id.ETXT_Date);
        etxt_time = findViewById(R.id.ETXT_Time);
        btn_confirm = findViewById(R.id.BTN_Confirm);
        btn_cancel = findViewById(R.id.BTN_Cancel);
        etxt_numPlan = findViewById(R.id.ETXT_numPlan);
        txt_plan = findViewById(R.id.TXT_Plan);
        txt_amount = findViewById(R.id.TXT_Amount);

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        etxt_date.setFocusable(false);
        etxt_date.setClickable(true);
        etxt_date.setOnClickListener(v -> showDatePickerDialog());

        etxt_time.setFocusable(false);
        etxt_time.setClickable(true);
        etxt_time.setOnClickListener(v -> showTimePickerDialog());

        String bikeTypeFromIntent = getIntent().getStringExtra("bikeType");
        bikeTypeToUpdate = bikeTypeFromIntent;
        txt_bikeType.setText(bikeTypeFromIntent + " bicycle");

        txt_location.setText(location);

        String Plan = getIntent().getStringExtra("Plan");
        txt_plan.setText(Plan);

        int basePrice = getIntent().getIntExtra("Price", -1);
        calculateAndUpdateTotalPrice(basePrice);

        etxt_numPlan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                calculateAndUpdateTotalPrice(basePrice);
            }
        });

        btn_confirm.setOnClickListener(v -> {
            if (validateFields() && isSelectedDateTimeValid()) {
                saveRideDetailsToFirestore();
            } else if (!validateFields()) {
                Toast.makeText(RideConfirmation.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            }
        });

        btn_cancel.setOnClickListener(v -> showCancelConfirmation());
    }

    private boolean validateFields() {
        return !etxt_date.getText().toString().trim().isEmpty() &&
                !etxt_time.getText().toString().trim().isEmpty() &&
                !etxt_numPlan.getText().toString().trim().isEmpty();
    }

    private void showDatePickerDialog() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, monthOfYear, dayOfMonth);
                    selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                    selectedDate.set(Calendar.MINUTE, 0);
                    selectedDate.set(Calendar.SECOND, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);

                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    if (selectedDate.before(today)) {
                        Toast.makeText(this, "Cannot select a past date.", Toast.LENGTH_SHORT).show();
                        etxt_date.setText("");
                        etxt_time.setText("");
                        return;
                    }
                    calendar.set(year, monthOfYear, dayOfMonth);
                    updateDateInView();
                    etxt_time.setText("");
                    Toast.makeText(this, "Please select a time for the chosen date.", Toast.LENGTH_SHORT).show();

                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        if (etxt_date.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please select a date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        boolean isToday = calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    if (isToday) {
                        if (hourOfDay < currentHour || (hourOfDay == currentHour && minute < currentMinute)) {
                            Toast.makeText(this, "Cannot select a past time for today.", Toast.LENGTH_SHORT).show();
                            etxt_time.setText("");
                            return;
                        }
                    }
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    updateTimeInView();
                },
                isToday ? currentHour : 0,
                isToday ? currentMinute : 0,
                true // is24HourView
        );
        timePickerDialog.show();
    }


    private void updateTimeInView() {
        String myFormat = "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        etxt_time.setText(sdf.format(calendar.getTime()));
    }

    private void updateDateInView() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        etxt_date.setText(sdf.format(calendar.getTime()));
    }

    private boolean isSelectedDateTimeValid() {
        if (etxt_date.getText().toString().isEmpty() || etxt_time.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select both date and time.", Toast.LENGTH_SHORT).show();
            return false;
        }

        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);


        if (calendar.before(now)) {
            Toast.makeText(this, "Selected date and time cannot be in the past.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    private void calculateAndUpdateTotalPrice(int basePrice) {
        int multiplier = 0;
        try {
            String numPlanText = etxt_numPlan.getText().toString();
            if (!numPlanText.isEmpty()) {
                multiplier = Integer.parseInt(numPlanText);
            }
        } catch (NumberFormatException e) {
        }

        int totalPrice = basePrice * multiplier;

        if (basePrice != -1) {
            txt_amount.setText("LKR " + totalPrice + ".00");
        } else {
            txt_amount.setText("-");
        }
    }

    private void mainMenu() {
        Intent goDash = new Intent(this, UserDashboard.class);
        startActivity(goDash);
        finish();
    }

    private void incrementBikeCountAndGoToMain() {
        if (bikeTypeToUpdate != null && !bikeTypeToUpdate.isEmpty() && liveDatabase != null) {
            liveDatabase.child(bikeTypeToUpdate).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Integer currentValue = dataSnapshot.getValue(Integer.class);
                    if (currentValue != null) {
                        int newValue = currentValue + 1;
                        liveDatabase.child(bikeTypeToUpdate).setValue(newValue)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Bike count incremented for " + bikeTypeToUpdate);
                                    mainMenu();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error incrementing bike count: " + e.getMessage());
                                    Toast.makeText(RideConfirmation.this, "Error updating bike count. Please try again.", Toast.LENGTH_SHORT).show();
                                    mainMenu();
                                });
                    } else {
                        Log.e(TAG, "Could not read current bike count for " + bikeTypeToUpdate + ". Assuming it's 0 and trying to set to 1 or handle appropriately.");
                        mainMenu();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error fetching bike count for increment: " + databaseError.getMessage());
                    Toast.makeText(RideConfirmation.this, "Error fetching bike data. Please try again.", Toast.LENGTH_SHORT).show();
                    mainMenu();
                }
            });
        } else {
            Log.e(TAG, "Cannot increment bike count: bikeType (" + bikeTypeToUpdate + ") or database reference is null (" + (liveDatabase == null) + ").");
            mainMenu();
        }
    }

    private void showBackConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Go Back?")
                .setMessage("Are you sure you want to go back to the main menu? This will cancel your current selection.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    incrementBikeCountAndGoToMain();
                })
                .setNegativeButton("No", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void saveRideDetailsToFirestore() {
        if (fAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isSelectedDateTimeValid()) {
            return;
        }

        String userId = fAuth.getCurrentUser().getUid();
        String userName = fAuth.getCurrentUser().getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = "N/A";
        }
        String bikeType = txt_bikeType.getText().toString();
        String location = txt_location.getText().toString();
        String plan = etxt_numPlan.getText().toString() + " " + txt_plan.getText().toString();
        String amount = txt_amount.getText().toString();
        String dateAndTime = etxt_date.getText().toString() + " " + etxt_time.getText().toString();

        Map<String, Object> rideDetails = new HashMap<>();
        rideDetails.put("userId", userId);
        rideDetails.put("Full Name", userName);
        rideDetails.put("bikeType", bikeType);
        rideDetails.put("location", location);
        rideDetails.put("plan", plan);
        rideDetails.put("amount", amount);
        rideDetails.put("dateAndTime", dateAndTime);
        // Store the selected calendar timestamp for easier querying/sorting if needed
        rideDetails.put("bookingTimestamp", calendar.getTimeInMillis());
        rideDetails.put("serverTimestamp", FieldValue.serverTimestamp());


        CollectionReference allHistoryRef = db.collection("AllHistory");

        allHistoryRef.add(rideDetails)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Ride details added to AllHistory with ID: " + documentReference.getId());
                    Toast.makeText(RideConfirmation.this, "Ride confirmed and details saved!", Toast.LENGTH_SHORT).show();
                    Intent goConfirm = new Intent(RideConfirmation.this, SplashActivityConfirm.class);
                    goConfirm.putExtra("BicycleType", bikeType);
                    goConfirm.putExtra("Location", location);
                    goConfirm.putExtra("Plan", plan);
                    goConfirm.putExtra("Amount", amount);
                    goConfirm.putExtra("Date", dateAndTime);
                    startActivity(goConfirm);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding ride details to AllHistory: ", e);
                    Toast.makeText(RideConfirmation.this, "Error confirming ride. Please try again.", Toast.LENGTH_SHORT).show();
                    incrementBikeCountAndGoToMain();
                });
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ride?")
                .setMessage("Are you sure you want to cancel?")
                .setPositiveButton("Yes", (dialog, which) -> incrementBikeCountAndGoToMain())
                .setNegativeButton("No", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}