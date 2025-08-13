package com.example.padelgo.stationOfficer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

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

public class StationOfficerDashboard extends AppCompatActivity {
    FirebaseAuth fauth;

    CardView btn_rents, btn_addStationOfficer, btn_availability, btn_verifyUsers, btn_settings, btn_release, btn_signOut;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_station_officer_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fauth = FirebaseAuth.getInstance();

        btn_rents = findViewById(R.id.BTN_Rents);
        btn_addStationOfficer = findViewById(R.id.BTN_AddStationOfficer);
        btn_availability = findViewById(R.id.BTN_Availability);
        btn_verifyUsers = findViewById(R.id.BTN_VerifyUsers);
        btn_settings = findViewById(R.id.BTN_Settings);
        btn_release = findViewById(R.id.BTN_Release);
        btn_signOut = findViewById(R.id.BTN_SignOut);

        View.OnClickListener signOutClickListener = v -> showSignOutConfirmationDialog();

        btn_rents.setOnClickListener(v -> {
            Intent moveToRents = new Intent(getApplicationContext(), StationOfficerHistory.class);
            startActivity(moveToRents);
        });
        btn_addStationOfficer.setOnClickListener(v -> {
            Intent moveAdminRegister = new Intent(getApplicationContext(), StationOfficerAccountCreate.class);
            startActivity(moveAdminRegister);
        });
        btn_availability.setOnClickListener(v -> {
            Intent movetoAvailability = new Intent(getApplicationContext(), StationOfficerLocation.class);
            startActivity(movetoAvailability);
        });
        btn_verifyUsers.setOnClickListener(v -> {
            Intent moveToProfile = new Intent(getApplicationContext(), StationOfficerVerifyNIC.class);
            startActivity(moveToProfile);
        });
        btn_settings.setOnClickListener(v -> {
            Intent moveToSettings = new Intent(getApplicationContext(), Settings.class);
            startActivity(moveToSettings);
        });
        btn_release.setOnClickListener(v -> {
            Intent moveToRelease = new Intent(getApplicationContext(), ReleaseBicycleActivity.class);
            startActivity(moveToRelease);
        });
        btn_signOut.setOnClickListener(signOutClickListener);

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