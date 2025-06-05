package com.example.padelgo.admin;

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
import com.example.padelgo.common.Settings;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDash extends AppCompatActivity {
    ImageView img_signOut, img_addAdmin, img_bicycleAvailability, img_settings, img_rents, img_profile;
    TextView txtBTN_signOut, txtBTN_addAdmin, txtBTN_bicycleAvailability, txtBTN_settings, txtBTN_rents, txtBTN_profile;
    FirebaseAuth fauth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fauth = FirebaseAuth.getInstance();

        img_signOut = findViewById(R.id.IMG_signOut);
        txtBTN_signOut = findViewById(R.id.TXTBTN_signOut);
        img_addAdmin = findViewById(R.id.IMG_addAdmin);
        txtBTN_addAdmin = findViewById(R.id.TXTBTN_addAdmin);
        img_bicycleAvailability = findViewById(R.id.IMG_bicycleAvailability);
        txtBTN_bicycleAvailability = findViewById(R.id.TXTBTN_bicycleAvailability);
        img_settings = findViewById(R.id.IMG_settings);
        txtBTN_settings = findViewById(R.id.TXTBTN_settings);
        img_rents = findViewById(R.id.IMG_rents);
        txtBTN_rents = findViewById(R.id.TXTBTN_rents);
        img_profile = findViewById(R.id.IMG_profile);
        txtBTN_profile = findViewById(R.id.TXTBTN_profile);

        View.OnClickListener signOutClickListener = v -> showSignOutConfirmationDialog();
        img_signOut.setOnClickListener(signOutClickListener);
        txtBTN_signOut.setOnClickListener(signOutClickListener);

        img_addAdmin.setOnClickListener(v -> {
            Intent moveAdminRegister = new Intent(getApplicationContext(), AdminAccountCreate.class);
            startActivity(moveAdminRegister);
        });

        txtBTN_addAdmin.setOnClickListener(v -> {
            Intent moveAdminRegister = new Intent(getApplicationContext(), AdminAccountCreate.class);
            startActivity(moveAdminRegister);
        });
        img_bicycleAvailability.setOnClickListener(v -> {
            Intent movetoAvailability = new Intent(getApplicationContext(), AdminLocation.class);
            startActivity(movetoAvailability);
        });
        txtBTN_bicycleAvailability.setOnClickListener(v -> {
            Intent movetoAvailability = new Intent(getApplicationContext(), AdminLocation.class);
            startActivity(movetoAvailability);
        });
        txtBTN_settings.setOnClickListener(v -> {
            Intent moveToSettings = new Intent(getApplicationContext(), Settings.class);
            startActivity(moveToSettings);
        });
        img_settings.setOnClickListener(v -> {
            Intent moveToSettings = new Intent(getApplicationContext(), Settings.class);
            startActivity(moveToSettings);
        });
        txtBTN_rents.setOnClickListener(v -> {
            Intent moveToRents = new Intent(getApplicationContext(), AdminRents.class);
            startActivity(moveToRents);
        });
        img_rents.setOnClickListener(v -> {
            Intent moveToRents = new Intent(getApplicationContext(), AdminRents.class);
            startActivity(moveToRents);
        });
        txtBTN_profile.setOnClickListener(v -> {
            Intent moveToProfile = new Intent(getApplicationContext(), AdminProfile.class);
            startActivity(moveToProfile);
        });
        img_profile.setOnClickListener(v -> {
            Intent moveToProfile = new Intent(getApplicationContext(), AdminProfile.class);
            startActivity(moveToProfile);
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
        startActivity(intent);
        finish();
    }
}