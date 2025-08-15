package com.example.padelgo.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import android.os.Handler;
import java.util.Locale;

public class UserNewProfile extends AppCompatActivity {
    TextView txt_UserName, txt_UserEmail;

    Button btn_VerifyAccount;
    ImageView imgbtn_Back;
    private FirebaseAuth fAuth;
    private FirebaseFirestore db;
    private static final String TAG = "UserProfile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_new_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        txt_UserName = findViewById(R.id.TXT_UserName);
        txt_UserEmail = findViewById(R.id.TXT_UserEmail);
        imgbtn_Back = findViewById(R.id.IMGBTN_Back);
        btn_VerifyAccount = findViewById(R.id.BTN_VerifyAccount);

        imgbtn_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserNewProfile.this, UserDashboard.class);
                startActivity(intent);
                finish();
            }
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mainMenu();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        btn_VerifyAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserNewProfile.this, VerifyNIC.class);
                startActivity(intent);
                finish();
            }
        });

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();
        checkVerificationStatus();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
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

    private void checkVerificationStatus() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        TextView txt_AccountStatus = findViewById(R.id.TXT_AccountStatus);

        if (currentUser != null) {
            String userId = currentUser.getUid();

            DatabaseReference statusRef = FirebaseDatabase.getInstance()
                    .getReference("user_verifications")
                    .child(userId)
                    .child("status");

            statusRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String status = snapshot.getValue(String.class);

                    if (status == null) {
                        txt_AccountStatus.setText("Account Status: Not Verified");
                        btn_VerifyAccount.setVisibility(View.VISIBLE);
                    } else if (status.equals("pending")) {
                        txt_AccountStatus.setText("Account Status: Pending");
                        btn_VerifyAccount.setVisibility(View.GONE);
                    } else if (status.equals("approved")) {
                        txt_AccountStatus.setText("Account Status: Active");
                        btn_VerifyAccount.setVisibility(View.GONE);
                    } else if (status.equals("rejected")) {
                        txt_AccountStatus.setText("Account Status: Rejected");
                        btn_VerifyAccount.setVisibility(View.VISIBLE);
                    } else {
                        txt_AccountStatus.setText("Account Status: Not Verified");
                        btn_VerifyAccount.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read NIC status: " + error.getMessage());
                    txt_AccountStatus.setText("Account Status: Unknown");
                    btn_VerifyAccount.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void mainMenu() {
        Intent goDash = new Intent(this, UserDashboard.class);
        startActivity(goDash);
        finish();
    }
}