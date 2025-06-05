package com.example.padelgo.admin;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminProfile extends AppCompatActivity {
    TextView txt_UserName, txt_UserEmail;

    ImageView imgbtn_Back;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "UserProfile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        txt_UserName = findViewById(R.id.TXT_UserName);
        txt_UserEmail = findViewById(R.id.TXT_UserEmail);
        imgbtn_Back = findViewById(R.id.IMGBTN_Back);

        imgbtn_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminProfile.this, AdminDash.class);
                startActivity(intent);
                finish();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("Users").document(userId);
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String fullName = document.getString("FullName");
                        if (fullName == null) {
                            fullName = document.getString("Full Name");
                            Log.w(TAG, "Field 'FullName' not found, falling back to 'Full Name'");
                        }
                        String email = currentUser.getEmail();
                        txt_UserName.setText(fullName != null ? fullName : "Name not available");
                        txt_UserEmail.setText(email);
                    } else {
                        Log.d(TAG, "No such user document");
                        txt_UserName.setText("User data not found");
                        txt_UserEmail.setText(currentUser.getEmail());
                    }
                } else {
                    Log.e(TAG, "Error loading user data: ", task.getException());
                    txt_UserName.setText("Error loading user data");
                }
            });
        }
    }
}