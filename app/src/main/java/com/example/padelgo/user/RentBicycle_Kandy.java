package com.example.padelgo.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.padelgo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class RentBicycle_Kandy extends AppCompatActivity {

    String location = "Kandy";
    String plan;
    int price;

    CheckBox cb_BasicHour, cb_BasicDay;
    Button btn_classic, btn_cityBike, btn_cruiser, btn_folding;
    TextView txt_classic, txt_cityBike, txt_cruiser, txt_folding;

    private DatabaseReference liveDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_rent_bicycle_kandy);
        liveDatabase = FirebaseDatabase.getInstance().getReference("bicycleAvailability_Kandy");

        initializeViews();
        checkPlan();
        loadInitialData();
        setRideValues();
    }


    private void initializeViews() {
        cb_BasicHour = findViewById(R.id.CB_BasicHour);
        cb_BasicDay = findViewById(R.id.CB_BasicDay);

        btn_classic = findViewById(R.id.BTN_classic);
        btn_cityBike = findViewById(R.id.BTN_cityBike);
        btn_cruiser = findViewById(R.id.BTN_cruiser);
        btn_folding = findViewById(R.id.BTN_folding);

        txt_classic = findViewById(R.id.TXT_classic);
        txt_cityBike = findViewById(R.id.TXT_cityBike);
        txt_cruiser = findViewById(R.id.TXT_cruiser);
        txt_folding = findViewById(R.id.TXT_folding);

        txt_classic.setVisibility(View.GONE);
        txt_cityBike.setVisibility(View.GONE);
        txt_cruiser.setVisibility(View.GONE);
        txt_folding.setVisibility(View.GONE);

        btn_classic.setEnabled(false);
        btn_cityBike.setEnabled(false);
        btn_cruiser.setEnabled(false);
        btn_folding.setEnabled(false);

        cb_BasicHour.setOnClickListener(v -> checkPlan());
        cb_BasicDay.setOnClickListener(v -> checkPlan());


    }

    private void checkPlan() {
        if (cb_BasicHour.isChecked()) {
            plan = "Hourly";
            price = 70;

            cb_BasicDay.setEnabled(false);


            btn_classic.setEnabled(true);
            btn_classic.setBackgroundColor(Color.parseColor("#7E206E"));
            btn_cityBike.setEnabled(true);
            btn_cityBike.setBackgroundColor(Color.parseColor("#7E206E"));
            btn_cruiser.setEnabled(true);
            btn_cruiser.setBackgroundColor(Color.parseColor("#7E206E"));
            btn_folding.setEnabled(true);
            btn_folding.setBackgroundColor(Color.parseColor("#7E206E"));

            txt_classic.setVisibility(View.VISIBLE);
            txt_cityBike.setVisibility(View.VISIBLE);
            txt_cruiser.setVisibility(View.VISIBLE);
            txt_folding.setVisibility(View.VISIBLE);

        } else if (cb_BasicDay.isChecked()) {
            plan = "Days";
            price = 500;

            cb_BasicHour.setEnabled(false);

            btn_classic.setEnabled(true);
            btn_classic.setBackgroundColor(Color.parseColor("#7E206E"));
            btn_cityBike.setEnabled(true);
            btn_cityBike.setBackgroundColor(Color.parseColor("#7E206E"));
            btn_cruiser.setEnabled(true);
            btn_cruiser.setBackgroundColor(Color.parseColor("#7E206E"));
            btn_folding.setEnabled(true);
            btn_folding.setBackgroundColor(Color.parseColor("#7E206E"));

            txt_classic.setVisibility(View.VISIBLE);
            txt_cityBike.setVisibility(View.VISIBLE);
            txt_cruiser.setVisibility(View.VISIBLE);
            txt_folding.setVisibility(View.VISIBLE);

        } else {
            plan = "";
            price = 0;
            btn_classic.setEnabled(false);
            btn_classic.setBackgroundColor(Color.parseColor("#273F51B5"));
            btn_cityBike.setEnabled(false);
            btn_cityBike.setBackgroundColor(Color.parseColor("#273F51B5"));
            btn_cruiser.setEnabled(false);
            btn_cruiser.setBackgroundColor(Color.parseColor("#273F51B5"));
            btn_folding.setEnabled(false);
            btn_folding.setBackgroundColor(Color.parseColor("#273F51B5"));

            cb_BasicDay.setEnabled(true);
            cb_BasicHour.setEnabled(true);

            txt_classic.setVisibility(View.GONE);
            txt_cityBike.setVisibility(View.GONE);
            txt_cruiser.setVisibility(View.GONE);
            txt_folding.setVisibility(View.GONE);
        }
    }

    private void setRideValues() {
        rideConfirming(btn_classic, txt_classic, "Classic");
        rideConfirming(btn_cityBike, txt_cityBike, "CityBike");
        rideConfirming(btn_cruiser, txt_cruiser, "Cruiser");
        rideConfirming(btn_folding, txt_folding, "Folding");
    }

    private void rideConfirming(Button button, TextView textView, String bikeType) {
        button.setOnClickListener(v -> {
            if (validateCheckboxSelection()) {

                String currentPlan = null;
                int currentPrice = -1;

                if (cb_BasicHour.isChecked()) {
                    currentPlan = "Hours";
                    currentPrice = 70;
                } else if (cb_BasicDay.isChecked()) {
                    currentPlan = "Days";
                    currentPrice = 500;
                }


                if (currentPlan != null && currentPrice != -1) {  // Only proceed if a plan is selected.
                    try {
                        int currentValue = Integer.parseInt(textView.getText().toString());
                        if (currentValue > 0) {
                            int newValue = currentValue - 1;
                            textView.setText(String.valueOf(newValue));
                            updateAvailability(bikeType, newValue); // Update Firebase
                            Intent gotoConfirm = new Intent(getApplicationContext(), RideConfirmation.class);
                            gotoConfirm.putExtra("bikeType", bikeType);
                            gotoConfirm.putExtra("Location", location);
                            gotoConfirm.putExtra("Plan", currentPlan);
                            gotoConfirm.putExtra("Price", currentPrice);
                            startActivity(gotoConfirm);
                        } else if (currentValue == 0) {
                            textView.setText("-");
                            showNotAvailableDialog();
                        }
                    } catch (NumberFormatException e) {
                        textView.setText("-");
                        showNotAvailableDialog();
                    }
                } else {
                    Toast.makeText(this, "Please select a pricing plan.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean validateCheckboxSelection() {
        int checkedCount = 0;
        if (cb_BasicHour.isChecked()) checkedCount++;
        if (cb_BasicDay.isChecked()) checkedCount++;


        if (checkedCount == 1) {
            return true;
        } else {
            Toast.makeText(this, "Please select exactly one pricing option.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void updateAvailability(String bikeType, int newValue) {
        liveDatabase.child(bikeType).setValue(newValue)
                .addOnFailureListener(e ->
                        Log.e("Firebase", "Error updating availability for " + bikeType + ": " + e.getMessage()));
    }

    private void showNotAvailableDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Not Available")
                .setMessage("This bicycle is currently not available.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void loadInitialData() {
        liveDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot bikeSnapshot : dataSnapshot.getChildren()) {
                        String bikeType = bikeSnapshot.getKey();
                        Integer availability = bikeSnapshot.getValue(Integer.class);
                        if (availability != null) {
                            updateTextView(bikeType, availability);
                        }
                    }
                } else {
                    initializeDefaultValues();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error loading data: " + databaseError.getMessage());
            }
        });
    }

    private void initializeDefaultValues() {
        Map<String, Object> defaultValues = new HashMap<>();
        defaultValues.put("Classic", 0);
        defaultValues.put("CityBike", 0);
        defaultValues.put("Cruiser", 0);
        defaultValues.put("Folding", 0);

        liveDatabase.setValue(defaultValues)
                .addOnSuccessListener(aVoid -> {
                    for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
                        updateTextView(entry.getKey(), (Integer) entry.getValue());
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error initializing data: " + e.getMessage()));
    }

    private void updateTextView(String bikeType, int availability) {
        TextView textView = null;
        switch (bikeType) {
            case "Classic":
                textView = txt_classic;
                break;
            case "CityBike":
                textView = txt_cityBike;
                break;
            case "Cruiser":
                textView = txt_cruiser;
                break;
            case "Folding":
                textView = txt_folding;
                break;
        }
        if (textView != null) {
            if (availability > 0) {
                textView.setText(String.valueOf(availability));
            } else {
                textView.setText("-");
            }
        }
    }
}