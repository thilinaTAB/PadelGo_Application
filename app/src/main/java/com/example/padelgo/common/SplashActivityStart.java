package com.example.padelgo.common;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;  // Import the Looper class

import androidx.appcompat.app.AppCompatActivity;

import com.example.padelgo.R;

public class SplashActivityStart extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_start_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivityStart.this, Login.class);
                startActivity(intent);
                finish();
            }
        }, 2000); // 2 seconds delay
    }
}