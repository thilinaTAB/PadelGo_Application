package com.example.padelgo.admin;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminAccountCreate extends AppCompatActivity {
    TextView txt_btnBack;

    EditText etxt_Name, etxt_Email, etxt_Password, etxt_RePassword;

    boolean valid = false;

    Button btn_Register;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_account_create);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txt_btnBack = findViewById(R.id.TXT_btnBack);
        btn_Register = findViewById(R.id.BTN_Register);

        etxt_Name = findViewById(R.id.ETXT_Name);
        etxt_Email = findViewById(R.id.ETXT_Email);
        etxt_Password = findViewById(R.id.ETXT_Password);
        etxt_RePassword = findViewById(R.id.ETXT_RePassword);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();


        txt_btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent moveBack = new Intent(getApplicationContext(), AdminDash.class);
                startActivity(moveBack);
            }
        });

        btn_Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkField(etxt_Name);
                checkField(etxt_Email);
                checkField(etxt_Password);
                checkField(etxt_RePassword);

                boolean nameValid = checkField(etxt_Name);
                boolean emailValid = checkField(etxt_Email);
                boolean passwordValid = checkField(etxt_Password);
                boolean rePasswordValid = checkField(etxt_RePassword);
                boolean allValid = false;

                String name = etxt_Name.getText().toString();
                String emailAddress = etxt_Email.getText().toString();
                String password = etxt_Password.getText().toString();
                String confirmPassword = etxt_RePassword.getText().toString();

                if (allValid = nameValid && emailValid && passwordValid && rePasswordValid) {
                    allValid = true;
                } else {
                    Toast.makeText(AdminAccountCreate.this, "Failed to create an account", Toast.LENGTH_SHORT).show();
                }

                if (!password.equals(confirmPassword)) {
                    etxt_RePassword.setError("Passwords do not match");
                    allValid = false;
                }

                if (allValid) {
                    fAuth.createUserWithEmailAndPassword(emailAddress, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            FirebaseFirestore user = FirebaseFirestore.getInstance();

                            Toast.makeText(AdminAccountCreate.this, "Account Created", Toast.LENGTH_SHORT).show();
                            DocumentReference df = fStore.collection("Users").document(fAuth.getCurrentUser().getUid());
                            Map<String, Object> userInfo = new HashMap<>();
                            userInfo.put("Full Name", name);
                            userInfo.put("Email Address", emailAddress);
                            userInfo.put("Password", password);

                            userInfo.put("isAdmin", "1");

                            df.set(userInfo);

                            startActivity(new Intent(getApplicationContext(), AdminDash.class));
                            finish();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AdminAccountCreate.this, "Failed to create an account", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    public boolean checkField(EditText ex) {
        if (ex.getText().toString().isEmpty()) {
            ex.setError("Required this field");
            valid = false;
        } else {
            valid = true;
        }
        return valid;
    }
}