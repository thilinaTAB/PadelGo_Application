package com.example.padelgo.user;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.common.Login;
import com.example.padelgo.R;
import com.example.padelgo.common.SelectLocation;
import com.example.padelgo.common.Settings;
import com.example.padelgo.common.UserProfile;
import com.google.firebase.auth.FirebaseAuth;

public class UserDashboard extends AppCompatActivity {
    ImageView img_signOut, img_bicycleCategory, img_rent, img_profile, img_history, img_settings;
    TextView txtBTN_signOut, txtBTN_bicycleCategory, txtBTN_rent, txtBTN_profile, txtBTN_history, txtBTN_settings;
    FirebaseAuth fauth;

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

        // Initialize FirebaseAuth
        fauth = FirebaseAuth.getInstance();

        img_signOut = findViewById(R.id.IMG_signOut);
        txtBTN_signOut = findViewById(R.id.TXTBTN_signOut);
        img_bicycleCategory = findViewById(R.id.IMG_bicycleCategory);
        txtBTN_bicycleCategory = findViewById(R.id.TXTBTN_bicycleCategory);
        img_rent = findViewById(R.id.IMG_rent);
        txtBTN_rent = findViewById(R.id.TXTBTN_rent);
        img_profile = findViewById(R.id.IMG_profile);
        txtBTN_profile = findViewById(R.id.TXTBTN_profile);
        img_history = findViewById(R.id.IMGBTN_History);
        txtBTN_history = findViewById(R.id.TXTBTN_History);
        img_settings = findViewById(R.id.IMG_settings);
        txtBTN_settings = findViewById(R.id.TXTBTN_settings);

        img_bicycleCategory.setOnClickListener(v -> {
            Intent movetoList = new Intent(getApplicationContext(), BicycleList.class);
            startActivity(movetoList);
        });
        txtBTN_bicycleCategory.setOnClickListener(v -> {
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

        img_profile.setOnClickListener(v -> {
            Intent moveToProfile = new Intent(getApplicationContext(), UserProfile.class);
            startActivity(moveToProfile);
        });
        txtBTN_profile.setOnClickListener(v -> {
            Intent moveToProfile = new Intent(getApplicationContext(), UserProfile.class);
            startActivity(moveToProfile);
        });
        img_history.setOnClickListener(v -> {
            Intent moveToHistory = new Intent(getApplicationContext(), RideHistory.class);
            startActivity(moveToHistory);
        });
        txtBTN_history.setOnClickListener(v -> {
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

        // Sign Out Click Listeners
        View.OnClickListener signOutClickListener = v -> showSignOutConfirmationDialog();
        img_signOut.setOnClickListener(signOutClickListener);
        txtBTN_signOut.setOnClickListener(signOutClickListener);

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
        startActivity(intent);
        finish();
    }
}