package com.example.padelgo.stationOfficer;

import android.os.Bundle;
import android.util.Log;  // Import Log
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminBicycleAvailabitily_Matale extends AppCompatActivity {

    Button btn_classicPlus, btn_classicMinus, btn_cityBikePlus, btn_cityBikeMinus, btn_cruiserPlus, btn_cruiserMinus, btn_foldingPlus, btn_foldingMinus;
    TextView txt_classic, txt_cityBike, txt_cruiser, txt_folding;

    private DatabaseReference liveDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_bicycle_availabitily_matale);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        liveDatabase = FirebaseDatabase.getInstance().getReference("bicycleAvailability_Matale");

        btn_classicPlus = findViewById(R.id.BTN_classicPlus);
        btn_classicMinus = findViewById(R.id.BTN_classicMinus);
        btn_cityBikePlus = findViewById(R.id.BTN_cityBikePlus);
        btn_cityBikeMinus = findViewById(R.id.BTN_cityBikeMinus);
        btn_cruiserPlus = findViewById(R.id.BTN_cruiserPlus);
        btn_cruiserMinus = findViewById(R.id.BTN_cruiserMinus);
        btn_foldingPlus = findViewById(R.id.BTN_foldingPlus);
        btn_foldingMinus = findViewById(R.id.BTN_foldingMinus);

        txt_classic = findViewById(R.id.TXT_classic);
        txt_cityBike = findViewById(R.id.TXT_cityBike);
        txt_cruiser = findViewById(R.id.TXT_cruiser);
        txt_folding = findViewById(R.id.TXT_folding);

        btn_classicPlus.setOnClickListener(v -> incrementText(txt_classic, "Classic"));
        btn_classicMinus.setOnClickListener(v -> decrementText(txt_classic, "Classic"));
        btn_cityBikePlus.setOnClickListener(v -> incrementText(txt_cityBike, "CityBike"));
        btn_cityBikeMinus.setOnClickListener(v -> decrementText(txt_cityBike, "CityBike"));
        btn_cruiserPlus.setOnClickListener(v -> incrementText(txt_cruiser, "Cruiser"));
        btn_cruiserMinus.setOnClickListener(v -> decrementText(txt_cruiser, "Cruiser"));
        btn_foldingPlus.setOnClickListener(v -> incrementText(txt_folding, "Folding"));
        btn_foldingMinus.setOnClickListener(v -> decrementText(txt_folding, "Folding"));

        ValueEventListener availabilityListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot bikeSnapshot : dataSnapshot.getChildren()) {
                    String bikeType = bikeSnapshot.getKey();
                    Integer availability = bikeSnapshot.getValue(Integer.class);

                    if (availability != null) {
                        Log.d("FirebaseRead", "Availability for " + bikeType + ": " + availability);
                        switch (bikeType) {
                            case "Classic":
                                txt_classic.setText(String.valueOf(availability));
                                break;
                            case "CityBike":
                                txt_cityBike.setText(String.valueOf(availability));
                                break;
                            case "Cruiser":
                                txt_cruiser.setText(String.valueOf(availability));
                                break;
                            case "Folding":
                                txt_folding.setText(String.valueOf(availability));
                                break;
                            default:
                                Log.w("FirebaseRead", "Unknown bike type: " + bikeType);
                        }
                    } else {
                        Log.w("FirebaseRead", "No availability data found for " + bikeType);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w("FirebaseRead", "Failed to read availability.", error.toException());
            }
        };
        liveDatabase.addValueEventListener(availabilityListener);
    }

    private void incrementText(TextView textView, String bikeType) {
        try {
            int currentValue = Integer.parseInt(textView.getText().toString());
            int newValue = currentValue + 1;
            textView.setText(String.valueOf(newValue));
            updateFirebase(bikeType, newValue);
        } catch (NumberFormatException e) {
            textView.setText("0");
            updateFirebase(bikeType, 0);
        }
    }

    private void decrementText(TextView textView, String bikeType) {
        try {
            int currentValue = Integer.parseInt(textView.getText().toString());
            if (currentValue > 0) {
                int newValue = currentValue - 1;
                textView.setText(String.valueOf(newValue));
                updateFirebase(bikeType, newValue);
            }
        } catch (NumberFormatException e) {
            textView.setText("0");
            updateFirebase(bikeType, 0);
        }
    }

    private void updateFirebase(String bikeType, int value) {
        String path = "bicycleAvailability_Matale/" + bikeType;
        liveDatabase.child(bikeType).setValue(value)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseUpdate", "Successfully updated " + path + " to " + value);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseUpdate", "Error updating " + path + ": " + e.getMessage());
                });
    }
}