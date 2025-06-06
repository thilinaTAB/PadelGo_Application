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
import com.example.padelgo.user.RentBicycle_Kandy;
import com.example.padelgo.user.RentBicycle_Katugastota;
import com.example.padelgo.user.RentBicycle_Matale;

public class SelectLocation extends AppCompatActivity {

    Button BTN_Kandy, BTN_Katugastota, BTN_Matale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_common_select_location);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BTN_Kandy = findViewById(R.id.BTN_Kandy);
        BTN_Katugastota = findViewById(R.id.BTN_Katugastota);
        BTN_Matale = findViewById(R.id.BTN_Matale);

        BTN_Kandy.setOnClickListener(v -> {
            Intent moveToRent_Kandy = new Intent(getApplicationContext(), RentBicycle_Kandy.class);
            startActivity(moveToRent_Kandy);
        });
        BTN_Katugastota.setOnClickListener(v -> {
            Intent moveToRent_Katugastota = new Intent(getApplicationContext(), RentBicycle_Katugastota.class);
            startActivity(moveToRent_Katugastota);
        });
        BTN_Matale.setOnClickListener(v -> {
            Intent moveToRent_Matale = new Intent(getApplicationContext(), RentBicycle_Matale.class);
            startActivity(moveToRent_Matale);
        });

    }
}