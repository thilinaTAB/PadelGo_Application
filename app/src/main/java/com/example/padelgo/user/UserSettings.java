package com.example.padelgo.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.example.padelgo.common.AboutUs;
import com.example.padelgo.common.PasswordChange;

public class UserSettings extends AppCompatActivity {

    Button btnChangePassword, btnAboutUs, btnTnC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnChangePassword = findViewById(R.id.BTN_ChangePassword);
        btnAboutUs = findViewById(R.id.BTN_AboutUs);
        btnTnC = findViewById(R.id.BTN_TnC);

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), PasswordChange.class);
            startActivity(intent);
        });
        btnAboutUs.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), AboutUs.class);
            startActivity(intent);
        });
        btnTnC.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), TermsConditions.class);
            startActivity(intent);
        });


    }
}