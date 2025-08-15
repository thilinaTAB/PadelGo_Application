package com.example.padelgo.common;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.example.padelgo.user.UserDashboard;

public class AboutUs extends AppCompatActivity {
    Button bn_Ok;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_common_about_us);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bn_Ok = findViewById(R.id.BTN_Ok);
        bn_Ok.setOnClickListener(v -> {
            Intent moveToDashboard = new Intent(getApplicationContext(), UserDashboard.class);
            startActivity(moveToDashboard);
        });
    }
}
