package com.example.padelgo.admin;

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

    Button btn_cplus, btn_cminus, btn_cBplus, btn_cBminus, btn_crplus, btn_crminus, btn_fplus, btn_fminus,
            btn_hplus, btn_hminus, btn_tplus, btn_tminus, btn_cr1plus, btn_cr1minus, btn_f1plus, btn_f1minus,
            btn_bplus, btn_bminus, btn_eplus, btn_eminus, btn_mplus, btn_mminus, btn_rplus, btn_rminus;
    TextView txt_classic, txt_cityBike, txt_cruiser, txt_folding, txt_hybrid, txt_touring,
            txt_cruiser1, txt_folding1, txt_bmx, txt_electric, txt_mountain, txt_roadBike;

    private DatabaseReference mDatabase;

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

        mDatabase = FirebaseDatabase.getInstance().getReference("bicycleAvailability_Matale");

        btn_cplus = findViewById(R.id.BTN_cplus);
        btn_cminus = findViewById(R.id.BTN_cminus);
        btn_cBplus = findViewById(R.id.BTN_cBplus);
        btn_cBminus = findViewById(R.id.BTN_cBminus);
        btn_crplus = findViewById(R.id.BTN_crplus);
        btn_crminus = findViewById(R.id.BTN_crminus);
        btn_fplus = findViewById(R.id.BTN_fplus);
        btn_fminus = findViewById(R.id.BTN_fminus);
        btn_hplus = findViewById(R.id.BTN_hplus);
        btn_hminus = findViewById(R.id.BTN_hminus);
        btn_tplus = findViewById(R.id.BTN_tplus);
        btn_tminus = findViewById(R.id.BTN_tminus);
        btn_cr1plus = findViewById(R.id.BTN_cr1plus);
        btn_cr1minus = findViewById(R.id.BTN_cr1minus);
        btn_f1plus = findViewById(R.id.BTN_f1plus);
        btn_f1minus = findViewById(R.id.BTN_f1minus);
        btn_bplus = findViewById(R.id.BTN_bplus);
        btn_bminus = findViewById(R.id.BTN_bminus);
        btn_eplus = findViewById(R.id.BTN_eplus);
        btn_eminus = findViewById(R.id.BTN_eminus);
        btn_mplus = findViewById(R.id.BTN_mplus);
        btn_mminus = findViewById(R.id.BTN_mminus);
        btn_rplus = findViewById(R.id.BTN_rplus);
        btn_rminus = findViewById(R.id.BTN_rminus);

        txt_classic = findViewById(R.id.TXT_classic);
        txt_cityBike = findViewById(R.id.TXT_cityBike);
        txt_cruiser = findViewById(R.id.TXT_cruiser);
        txt_folding = findViewById(R.id.TXT_folding);
        txt_hybrid = findViewById(R.id.TXT_hybrid);
        txt_touring = findViewById(R.id.TXT_touring);
        txt_cruiser1 = findViewById(R.id.TXT_cruiser1);
        txt_folding1 = findViewById(R.id.TXT_folding1);
        txt_bmx = findViewById(R.id.TXT_bmx);
        txt_electric = findViewById(R.id.TXT_electric);
        txt_mountain = findViewById(R.id.TXT_mountain);
        txt_roadBike = findViewById(R.id.TXT_roadBike);

        btn_cplus.setOnClickListener(v -> incrementText(txt_classic, "classic"));
        btn_cminus.setOnClickListener(v -> decrementText(txt_classic, "classic"));
        btn_cBplus.setOnClickListener(v -> incrementText(txt_cityBike, "cityBike"));
        btn_cBminus.setOnClickListener(v -> decrementText(txt_cityBike, "cityBike"));
        btn_crplus.setOnClickListener(v -> incrementText(txt_cruiser, "cruiser"));
        btn_crminus.setOnClickListener(v -> decrementText(txt_cruiser, "cruiser"));
        btn_fplus.setOnClickListener(v -> incrementText(txt_folding, "folding"));
        btn_fminus.setOnClickListener(v -> decrementText(txt_folding, "folding"));
        btn_hplus.setOnClickListener(v -> incrementText(txt_hybrid, "hybrid"));
        btn_hminus.setOnClickListener(v -> decrementText(txt_hybrid, "hybrid"));
        btn_tplus.setOnClickListener(v -> incrementText(txt_touring, "touring"));
        btn_tminus.setOnClickListener(v -> decrementText(txt_touring, "touring"));
        btn_cr1plus.setOnClickListener(v -> incrementText(txt_cruiser1, "cruiser1"));
        btn_cr1minus.setOnClickListener(v -> decrementText(txt_cruiser1, "cruiser1"));
        btn_f1plus.setOnClickListener(v -> incrementText(txt_folding1, "folding1"));
        btn_f1minus.setOnClickListener(v -> decrementText(txt_folding1, "folding1"));
        btn_bplus.setOnClickListener(v -> incrementText(txt_bmx, "bmx"));
        btn_bminus.setOnClickListener(v -> decrementText(txt_bmx, "bmx"));
        btn_eplus.setOnClickListener(v -> incrementText(txt_electric, "electric"));
        btn_eminus.setOnClickListener(v -> decrementText(txt_electric, "electric"));
        btn_mplus.setOnClickListener(v -> incrementText(txt_mountain, "mountain"));
        btn_mminus.setOnClickListener(v -> decrementText(txt_mountain, "mountain"));
        btn_rplus.setOnClickListener(v -> incrementText(txt_roadBike, "roadBike"));
        btn_rminus.setOnClickListener(v -> decrementText(txt_roadBike, "roadBike"));

        ValueEventListener availabilityListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot bikeSnapshot : dataSnapshot.getChildren()) {
                    String bikeType = bikeSnapshot.getKey();
                    Integer availability = bikeSnapshot.getValue(Integer.class);

                    if (availability != null) {
                        Log.d("FirebaseRead", "Availability for " + bikeType + ": " + availability);
                        switch (bikeType) {
                            case "classic":
                                txt_classic.setText(String.valueOf(availability));
                                break;
                            case "cityBike":
                                txt_cityBike.setText(String.valueOf(availability));
                                break;
                            case "cruiser":
                                txt_cruiser.setText(String.valueOf(availability));
                                break;
                            case "folding":
                                txt_folding.setText(String.valueOf(availability));
                                break;
                            case "hybrid":
                                txt_hybrid.setText(String.valueOf(availability));
                                break;
                            case "touring":
                                txt_touring.setText(String.valueOf(availability));
                                break;
                            case "cruiser1":
                                txt_cruiser1.setText(String.valueOf(availability));
                                break;
                            case "folding1":
                                txt_folding1.setText(String.valueOf(availability));
                                break;
                            case "bmx":
                                txt_bmx.setText(String.valueOf(availability));
                                break;
                            case "electric":
                                txt_electric.setText(String.valueOf(availability));
                                break;
                            case "mountain":
                                txt_mountain.setText(String.valueOf(availability));
                                break;
                            case "roadBike":
                                txt_roadBike.setText(String.valueOf(availability));
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
        mDatabase.addValueEventListener(availabilityListener);
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
        mDatabase.child(bikeType).setValue(value)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseUpdate", "Successfully updated " + path + " to " + value);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseUpdate", "Error updating " + path + ": " + e.getMessage());
                });
    }
}