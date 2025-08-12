package com.example.padelgo.user;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.common.Login;
import com.example.padelgo.R;
import com.example.padelgo.common.Settings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class UserDashboard extends AppCompatActivity {
    ImageView img_signOut, img_bicycleCategory, img_rent, img_profile, img_RideHistory, img_settings,img_MyRides;
    TextView txtBTN_signOut, txtBTN_bicycleCategory, txtBTN_rent, txtBTN_profile, txtBTN_RideHistory, txtBTN_settings,txtBTN_MyRides, greetingText, welcomeText;
    CardView  cv_Profile, cv_History, cv_Settings, cv_MyRides, cv_SignOut, cv_Packages,cv_RentNow;
    FirebaseAuth fauth;
    FirebaseFirestore fStore;
    String userId;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fauth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        img_signOut = findViewById(R.id.IMG_signOut);
        txtBTN_signOut = findViewById(R.id.TXTBTN_signOut);
        img_bicycleCategory = findViewById(R.id.IMG_Packages);
        txtBTN_bicycleCategory = findViewById(R.id.TXTBTN_BicycleList);
        img_rent = findViewById(R.id.IMG_rent);
        txtBTN_rent = findViewById(R.id.TXTBTN_rent);
        img_profile = findViewById(R.id.IMG__UserProfile);
        txtBTN_profile = findViewById(R.id.TXTBTN_UserProfile);
        img_RideHistory = findViewById(R.id.IMGBTN_History);
        txtBTN_RideHistory = findViewById(R.id.TXTBTN_History);
        img_settings = findViewById(R.id.IMG_settings);
        txtBTN_settings = findViewById(R.id.TXTBTN_settings);
        greetingText = findViewById(R.id.greetingText);
        welcomeText = findViewById(R.id.welcomeText);
        img_MyRides = findViewById(R.id.IMG_MyRides);
        txtBTN_MyRides = findViewById(R.id.TXTBTN_MyRides);
        cv_Profile = findViewById(R.id.CV_Profile);
        cv_History = findViewById(R.id.CV_History);
        cv_Settings = findViewById(R.id.CV_Settings);
        cv_MyRides = findViewById(R.id.CV_MyRides);
        cv_SignOut = findViewById(R.id.CV_SignOut);
        cv_Packages = findViewById(R.id.CV_Packages);
        cv_RentNow = findViewById(R.id.CV_RentNow);

        setTimeBasedGreeting();

        if (fauth.getCurrentUser() != null) {
            userId = fauth.getCurrentUser().getUid();
            fetchUserName();
        } else {
            welcomeText.setText("Welcome to PadelGo!");
        }

        img_bicycleCategory.setOnClickListener(v -> {
            Intent movetoList = new Intent(getApplicationContext(), BicycleList.class);
            startActivity(movetoList);
        });
        txtBTN_bicycleCategory.setOnClickListener(v -> {
            Intent movetoList = new Intent(getApplicationContext(), BicycleList.class);
            startActivity(movetoList);
        });
        cv_Packages.setOnClickListener(v -> {
            Intent movetoList = new Intent(getApplicationContext(), BicycleList.class);
            startActivity(movetoList);
        });

        img_rent.setOnClickListener(v -> {
            Intent moveToLocation = new Intent(getApplicationContext(), SelectLocation.class);
            startActivity(moveToLocation);
        });
        txtBTN_rent.setOnClickListener(v -> {
            Intent moveToLocation = new Intent(getApplicationContext(), SelectLocation.class);
            startActivity(moveToLocation);
        });
        cv_RentNow.setOnClickListener(v -> {
            Intent moveToLocation = new Intent(getApplicationContext(), SelectLocation.class);
            startActivity(moveToLocation);
        });

        img_profile.setOnClickListener(v -> {
            Intent moveToProfile = new Intent(getApplicationContext(), UserNewProfile.class);
            startActivity(moveToProfile);
        });
        txtBTN_profile.setOnClickListener(v -> {
            Intent moveToProfile = new Intent(getApplicationContext(), UserNewProfile.class);
            startActivity(moveToProfile);
        });
        cv_Profile.setOnClickListener(v -> {
            Intent moveToProfile = new Intent(getApplicationContext(), UserNewProfile.class);
            startActivity(moveToProfile);
        });

        img_MyRides.setOnClickListener(v -> {
            Intent moveToMyRides = new Intent(getApplicationContext(), MyRides.class);
            startActivity(moveToMyRides);
        });
        txtBTN_MyRides.setOnClickListener(v -> {
            Intent moveToMyRides = new Intent(getApplicationContext(), MyRides.class);
            startActivity(moveToMyRides);
        });
        cv_MyRides.setOnClickListener(v -> {
            Intent moveToMyRides = new Intent(getApplicationContext(), MyRides.class);
            startActivity(moveToMyRides);
        });

        img_RideHistory.setOnClickListener(v -> {
            Intent moveToHistory = new Intent(getApplicationContext(), RideHistory.class);
            startActivity(moveToHistory);
        });
        txtBTN_RideHistory.setOnClickListener(v -> {
            Intent moveToHistory = new Intent(getApplicationContext(), RideHistory.class);
            startActivity(moveToHistory);
        });
        cv_History.setOnClickListener(v -> {
            Intent moveToHistory = new Intent(getApplicationContext(), RideHistory.class);
            startActivity(moveToHistory);
        });

        img_settings.setOnClickListener(v -> {
            Intent moveToSettings = new Intent(getApplicationContext(), Settings.class);
            startActivity(moveToSettings);
        });
        txtBTN_settings.setOnClickListener(v -> {
            Intent moveToSettings = new Intent(getApplicationContext(), Settings.class);
            startActivity(moveToSettings);
        });
        cv_Settings.setOnClickListener(v -> {
            Intent moveToSettings = new Intent(getApplicationContext(), Settings.class);
            startActivity(moveToSettings);
        });

        View.OnClickListener signOutClickListener = v -> showSignOutConfirmationDialog();
        img_signOut.setOnClickListener(signOutClickListener);
        txtBTN_signOut.setOnClickListener(signOutClickListener);
        cv_SignOut.setOnClickListener(signOutClickListener);
    }

    private void setTimeBasedGreeting() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            greetingText.setText("Good Morning!");
        } else if (hour >= 12 && hour < 18) {
            greetingText.setText("Good Afternoon!");
        } else if (hour >= 18 && hour < 22) {  // 6 PM - 9:59 PM
            greetingText.setText("Good Evening!");
        } else {                              // 10 PM - 4:59 AM
            greetingText.setText("Good Night!");
        }
    }

    private void fetchUserName() {
        if (fStore == null || userId == null) {
            welcomeText.setText("Welcome to PadelGo!");
            return;
        }

        // Get the user creation timestamp from FirebaseAuth
        FirebaseUser user = fauth.getCurrentUser();
        if (user == null) {
            welcomeText.setText("Welcome to PadelGo!");
            return;
        }

        // Get metadata to check if user is new
        long creationTimestamp = user.getMetadata().getCreationTimestamp();
        long currentTimestamp = System.currentTimeMillis();
        boolean isNewUser = (currentTimestamp - creationTimestamp) < TimeUnit.DAYS.toMillis(1); // Considered new if account created < 1 day ago

        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("Full Name");
                        if (fullName == null) {
                            fullName = documentSnapshot.getString("fullName");
                        }

                        if (fullName != null && !fullName.isEmpty()) {
                            if (isNewUser) {
                                welcomeText.setText("Welcome to PadelGo, " + fullName + "!");
                            } else {
                                welcomeText.setText("Welcome back, " + fullName + "!");
                            }
                        } else {
                            String email = documentSnapshot.getString("Email Address");
                            if (email != null && !email.isEmpty()) {
                                String username = email.split("@")[0];
                                if (isNewUser) {
                                    welcomeText.setText("Welcome to PadelGo, " + username + "!");
                                } else {
                                    welcomeText.setText("Welcome back, " + username + "!");
                                }
                            } else {
                                welcomeText.setText(isNewUser ? "Welcome to PadelGo!" : "Welcome back!");
                            }
                        }
                    } else {
                        // User document doesn't exist yet (brand new user)
                        welcomeText.setText("Welcome to PadelGo!");
                    }
                })
                .addOnFailureListener(e -> {
                    welcomeText.setText("Welcome to PadelGo!");
                    Log.e("UserDashboard", "Error fetching user data", e);
                });
    }
    private void showSignOutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", (dialog, which) -> signOut())
                .setNegativeButton("No", null) // Do nothing on "No"
                .show();
    }

    private void signOut() {
        fauth.signOut();
        Intent intent = new Intent(this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}