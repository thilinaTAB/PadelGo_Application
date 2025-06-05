package com.example.padelgo.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

    CheckBox cb_BasicHour, cb_BasicDay, cb_BasicMonth,
            cb_PlusH, cb_PlusD, cb_PlusM,
            cb_ProH, cb_ProD, cb_ProM;

    Switch sw_Basic, sw_Plus, sw_Pro;

    Button btn_classic, btn_cityBike, btn_cruiser, btn_folding;
    Button btn_hybrid, btn_touring, btn_cruiser1, btn_folding1;
    Button btn_bmx, btn_mountain, btn_roadBike, btn_electric;

    TextView txt_classic, txt_cityBike, txt_cruiser, txt_folding;
    TextView txt_hybrid, txt_touring, txt_cruiser1, txt_folding1;
    TextView txt_bmx, txt_mountain, txt_roadBike, txt_electric;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_rent_bicycle_kandy);

        mDatabase = FirebaseDatabase.getInstance().getReference("bicycleAvailability_Kandy");

        initializeViews();
        setSwitchListeners();  // Initialize switch listeners before button listeners
        setButtonOnClickListeners();
        loadInitialData();
    }

    private void initializeViews() {
        btn_classic = findViewById(R.id.BTN_classic);
        btn_cityBike = findViewById(R.id.BTN_cityBike);
        btn_cruiser = findViewById(R.id.BTN_cruiser);
        btn_folding = findViewById(R.id.BTN_folding);
        btn_hybrid = findViewById(R.id.BTN_hybrid);
        btn_touring = findViewById(R.id.BTN_touring);
        btn_cruiser1 = findViewById(R.id.BTN_cruiser1);
        btn_folding1 = findViewById(R.id.BTN_folding1);
        btn_bmx = findViewById(R.id.BTN_bmx);
        btn_mountain = findViewById(R.id.BTN_mountain);
        btn_roadBike = findViewById(R.id.BTN_roadBike);
        btn_electric = findViewById(R.id.BTN_electric);

        txt_classic = findViewById(R.id.TXT_classic);
        txt_cityBike = findViewById(R.id.TXT_cityBike);
        txt_cruiser = findViewById(R.id.TXT_cruiser);
        txt_folding = findViewById(R.id.TXT_folding);
        txt_hybrid = findViewById(R.id.TXT_hybrid);
        txt_touring = findViewById(R.id.TXT_touring);
        txt_cruiser1 = findViewById(R.id.TXT_cruiser1);
        txt_folding1 = findViewById(R.id.TXT_folding1);
        txt_bmx = findViewById(R.id.TXT_bmx);
        txt_mountain = findViewById(R.id.TXT_mountain);
        txt_roadBike = findViewById(R.id.TXT_roadBike);
        txt_electric = findViewById(R.id.TXT_electric);

        cb_BasicHour = findViewById(R.id.CB_BasicHour);
        cb_BasicDay = findViewById(R.id.CB_BasicDay);
        cb_BasicMonth = findViewById(R.id.CB_BasicMonth);
        cb_PlusH = findViewById(R.id.CB_PlusH);
        cb_PlusD = findViewById(R.id.CB_PlusD);
        cb_PlusM = findViewById(R.id.CB_PlusM);
        cb_ProH = findViewById(R.id.CB_ProH);
        cb_ProD = findViewById(R.id.CB_ProD);
        cb_ProM = findViewById(R.id.CB_ProM);

        sw_Basic = findViewById(R.id.SW_Basic);
        sw_Plus = findViewById(R.id.SW_Plus);
        sw_Pro = findViewById(R.id.SW_Pro);
    }


    private void setSwitchListeners() {
        sw_Basic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sw_Plus.setChecked(false);
                    sw_Pro.setChecked(false);
                    enableBasicCheckboxes();
                    disablePlusCheckboxes();
                    disableProCheckboxes();

                    if (cb_BasicHour.isChecked()) {
                        plan = "Hours";
                        price = 70;
                    } else if (cb_BasicDay.isChecked()) {
                        plan = "Days";
                        price = 1500;
                    } else if (cb_BasicMonth.isChecked()) {
                        plan = "Months";
                        price = 40000;
                    }
                } else {
                    disableBasicCheckboxes();
                }
            }
        });

        sw_Plus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sw_Basic.setChecked(false);
                    sw_Pro.setChecked(false);
                    disableBasicCheckboxes();
                    enablePlusCheckboxes();
                    disableProCheckboxes();

                    if (cb_PlusH.isChecked()) {
                        plan = "Hours";
                        price = 100;
                    } else if (cb_PlusD.isChecked()) {
                        plan = "Days";
                        price = 2200;
                    } else if (cb_PlusM.isChecked()) {
                        plan = "Months";
                        price = 60000;
                    }
                } else {
                    disablePlusCheckboxes();
                }
            }
        });

        sw_Pro.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sw_Basic.setChecked(false);
                    sw_Plus.setChecked(false);
                    disableBasicCheckboxes();
                    disablePlusCheckboxes();
                    enableProCheckboxes();

                    if (cb_ProH.isChecked()) {
                        plan = "Hours";
                        price = 120;
                    } else if (cb_ProD.isChecked()) {
                        plan = "Days";
                        price = 2700;
                    } else if (cb_ProM.isChecked()) {
                        plan = "Months";
                        price = 78000;
                    }
                } else {
                    disableProCheckboxes();
                }
            }
        });
    }

    private void enableBasicCheckboxes() {
        cb_BasicHour.setEnabled(true);
        cb_BasicDay.setEnabled(true);
        cb_BasicMonth.setEnabled(true);
    }

    private void disableBasicCheckboxes() {
        cb_BasicHour.setEnabled(false);
        cb_BasicDay.setEnabled(false);
        cb_BasicMonth.setEnabled(false);
        cb_BasicHour.setChecked(false);
        cb_BasicDay.setChecked(false);
        cb_BasicMonth.setChecked(false);
    }

    private void enablePlusCheckboxes() {
        cb_PlusH.setEnabled(true);
        cb_PlusD.setEnabled(true);
        cb_PlusM.setEnabled(true);
    }

    private void disablePlusCheckboxes() {
        cb_PlusH.setEnabled(false);
        cb_PlusD.setEnabled(false);
        cb_PlusM.setEnabled(false);
        cb_PlusH.setChecked(false);
        cb_PlusD.setChecked(false);
        cb_PlusM.setChecked(false);
    }

    private void enableProCheckboxes() {
        cb_ProH.setEnabled(true);
        cb_ProD.setEnabled(true);
        cb_ProM.setEnabled(true);
    }

    private void disableProCheckboxes() {
        cb_ProH.setEnabled(false);
        cb_ProD.setEnabled(false);
        cb_ProM.setEnabled(false);
        cb_ProH.setChecked(false);
        cb_ProD.setChecked(false);
        cb_ProM.setChecked(false);
    }


    private void setButtonOnClickListeners() {
        setButtonOnClickListener(btn_classic, txt_classic, "classic");
        setButtonOnClickListener(btn_cityBike, txt_cityBike, "cityBike");
        setButtonOnClickListener(btn_cruiser, txt_cruiser, "cruiser");
        setButtonOnClickListener(btn_folding, txt_folding, "folding");
        setButtonOnClickListener(btn_hybrid, txt_hybrid, "hybrid");
        setButtonOnClickListener(btn_touring, txt_touring, "touring");
        setButtonOnClickListener(btn_cruiser1, txt_cruiser1, "cruiser1");
        setButtonOnClickListener(btn_folding1, txt_folding1, "folding1");
        setButtonOnClickListener(btn_bmx, txt_bmx, "bmx");
        setButtonOnClickListener(btn_mountain, txt_mountain, "mountain");
        setButtonOnClickListener(btn_roadBike, txt_roadBike, "roadBike");
        setButtonOnClickListener(btn_electric, txt_electric, "electric");
    }

    private void setButtonOnClickListener(Button button, TextView textView, String bikeType) {
        button.setOnClickListener(v -> {
            if (validateCheckboxSelection()) {
                // --- Get Plan and Price based on current selections ---
                String currentPlan = null;
                int currentPrice = -1; // Or a suitable default

                if (sw_Basic.isChecked()) {
                    if (cb_BasicHour.isChecked()) {
                        currentPlan = "Hours";
                        currentPrice = 70;
                    } else if (cb_BasicDay.isChecked()) {
                        currentPlan = "Days";
                        currentPrice = 1500;
                    } else if (cb_BasicMonth.isChecked()) {
                        currentPlan = "Months";
                        currentPrice = 40000;
                    }
                } else if (sw_Plus.isChecked()) {
                    if (cb_PlusH.isChecked()) {
                        currentPlan = "Hours";
                        currentPrice = 100;
                    } else if (cb_PlusD.isChecked()) {
                        currentPlan = "Days";
                        currentPrice = 2200;
                    } else if (cb_PlusM.isChecked()) {
                        currentPlan = "Months";
                        currentPrice = 60000;
                    }
                } else if (sw_Pro.isChecked()) {
                    if (cb_ProH.isChecked()) {
                        currentPlan = "Hours";
                        currentPrice = 120;
                    } else if (cb_ProD.isChecked()) {
                        currentPlan = "Days";
                        currentPrice = 2700;
                    } else if (cb_ProM.isChecked()) {
                        currentPlan = "Months";
                        currentPrice = 78000;
                    }
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
        if (cb_BasicMonth.isChecked()) checkedCount++;
        if (cb_PlusH.isChecked()) checkedCount++;
        if (cb_PlusD.isChecked()) checkedCount++;
        if (cb_PlusM.isChecked()) checkedCount++;
        if (cb_ProH.isChecked()) checkedCount++;
        if (cb_ProD.isChecked()) checkedCount++;
        if (cb_ProM.isChecked()) checkedCount++;

        if (checkedCount == 1) {
            return true;
        } else {
            Toast.makeText(this, "Please select exactly one pricing option.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void loadInitialData() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
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

    private void updateTextView(String bikeType, int availability) {
        TextView textView = null;
        switch (bikeType) {
            case "classic":
                textView = txt_classic;
                break;
            case "cityBike":
                textView = txt_cityBike;
                break;
            case "cruiser":
                textView = txt_cruiser;
                break;
            case "folding":
                textView = txt_folding;
                break;
            case "hybrid":
                textView = txt_hybrid;
                break;
            case "touring":
                textView = txt_touring;
                break;
            case "cruiser1":
                textView = txt_cruiser1;
                break;
            case "folding1":
                textView = txt_folding1;
                break;
            case "bmx":
                textView = txt_bmx;
                break;
            case "mountain":
                textView = txt_mountain;
                break;
            case "roadBike":
                textView = txt_roadBike;
                break;
            case "electric":
                textView = txt_electric;
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

    private void initializeDefaultValues() {
        Map<String, Object> defaultValues = new HashMap<>();
        defaultValues.put("classic", 0);
        defaultValues.put("cityBike", 0);
        defaultValues.put("cruiser", 0);
        defaultValues.put("folding", 0);
        defaultValues.put("hybrid", 0);
        defaultValues.put("touring", 0);
        defaultValues.put("cruiser1", 0);
        defaultValues.put("folding1", 0);
        defaultValues.put("bmx", 0);
        defaultValues.put("mountain", 0);
        defaultValues.put("roadBike", 0);
        defaultValues.put("electric", 0);

        mDatabase.setValue(defaultValues)
                .addOnSuccessListener(aVoid -> {
                    for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
                        updateTextView(entry.getKey(), (Integer) entry.getValue());
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error initializing data: " + e.getMessage()));
    }


    private void updateAvailability(String bikeType, int newValue) {
        mDatabase.child(bikeType).setValue(newValue)
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
}