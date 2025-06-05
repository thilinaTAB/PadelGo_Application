package com.example.padelgo.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class RideHistory extends AppCompatActivity {

    TextView txt_History1;

    ImageView imgbtn_Back;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final String TAG = "RideHistory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_ride_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txt_History1 = findViewById(R.id.TXT_History1);
        imgbtn_Back = findViewById(R.id.IMGBTN_Back);

        imgbtn_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RideHistory.this, UserDashboard.class);
                startActivity(intent);
                finish();
            }
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadRideHistory();
    }

    private void loadRideHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("RideHistory")
                    .document(userId)
                    .collection("rides")
                    .orderBy("timestamp", Query.Direction.DESCENDING) // Order by most recent first (optional)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            StringBuilder historyText = new StringBuilder();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Extract the ride details you want to display
                                String bicycleType = document.getString("bicycleType");
                                String location = document.getString("location");
                                String plan = document.getString("plan");
                                String amount = document.getString("amount");
                                String date = document.getString("date");

                                // Format the ride information into a string
                                String rideString = String.format(
                                        "• %s at %s on %s (%s, %s)",
                                        bicycleType, location, date, plan, amount
                                );

                                // Append to the text with a bullet point and newline
                                historyText.append("").append(rideString).append("\n \n");
                            }

                            txt_History1.setText(historyText.toString());

                        } else {
                            Log.e(TAG, "Error getting ride history: ", task.getException());
                            txt_History1.setText("Error loading ride history.");
                        }
                    });
        } else {
            txt_History1.setText("User not logged in.");
        }
    }
}