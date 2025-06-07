package com.example.padelgo.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.padelgo.R;
import com.example.padelgo.common.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SplashActivityConfirm extends AppCompatActivity {

    String BicycleType, Location, Plan, Amount, Date;
    private FirebaseFirestore db;
    private FirebaseAuth fAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_confirm_splash);

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        BicycleType = getIntent().getStringExtra("BicycleType");
        Location = getIntent().getStringExtra("Location");
        Plan = getIntent().getStringExtra("Plan");
        Amount = getIntent().getStringExtra("Amount");
        Date = getIntent().getStringExtra("Date");

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                saveRideInfo();
            }
        }, 3000); // 4 seconds = 4000 milliseconds
    }

    private void saveRideInfo() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            Map<String, Object> rideInfo = new HashMap<>();
            rideInfo.put("bicycleType", BicycleType);
            rideInfo.put("location", Location);
            rideInfo.put("plan", Plan);
            rideInfo.put("amount", Amount);
            rideInfo.put("date", Date);
            rideInfo.put("timestamp", System.currentTimeMillis());

            db.collection("RideHistory")
                    .document(userId)
                    .collection("rides")
                    .add(rideInfo)
                    .addOnSuccessListener(documentReference -> {
                        Intent splash = new Intent(SplashActivityConfirm.this, UserProfile.class);
                        startActivity(splash);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "ERROR. Try later", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}