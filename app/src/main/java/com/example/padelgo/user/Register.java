package com.example.padelgo.user;

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

import com.example.padelgo.common.Login;
import com.example.padelgo.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    EditText etxt_Name, etxt_NIC, etxt_Email, etxt_Mobile, etxt_Password, etxt_RePassword;
    Button btn_Register;
    TextView txt_btnLogin;

    Boolean valid = false;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etxt_Name = findViewById(R.id.ETXT_Name);
        etxt_NIC = findViewById(R.id.ETXT_NIC);
        etxt_Email = findViewById(R.id.ETXT_Email);
        etxt_Mobile = findViewById(R.id.ETXT_Mobile);
        etxt_Password = findViewById(R.id.ETXT_Password);
        etxt_RePassword = findViewById(R.id.ETXT_RePassword);
        btn_Register = findViewById(R.id.BTN_Register);
        txt_btnLogin = findViewById(R.id.TXT_btnLogin);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        txt_btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent moveLogin = new Intent(getApplicationContext(), Login.class);
                startActivity(moveLogin);
            }
        });

        btn_Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkField(etxt_Name);
                checkField(etxt_NIC);
                checkField(etxt_Email);
                checkField(etxt_Mobile);
                checkField(etxt_Password);
                checkField(etxt_RePassword);

                boolean nameValid = checkField(etxt_Name);
                boolean emailValid = checkField(etxt_Email);
                boolean passwordValid = checkField(etxt_Password);
                boolean rePasswordValid = checkField(etxt_RePassword);
                boolean nicValid = checkField(etxt_NIC);
                boolean mobileValid = checkField(etxt_Mobile);
                boolean allValid;

                String name = etxt_Name.getText().toString();
                String emailAddress = etxt_Email.getText().toString();
                String password = etxt_Password.getText().toString();
                String confirmPassword = etxt_RePassword.getText().toString();
                String nic = etxt_NIC.getText().toString();
                String mobile = etxt_Mobile.getText().toString();

                if (allValid = nameValid && emailValid && passwordValid && rePasswordValid && nicValid && mobileValid) {
                    allValid = true;
                } else {
                    Toast.makeText(Register.this, "Failed to create an account", Toast.LENGTH_SHORT).show();
                }

                if (!nic.matches("^[0-9]{9}[Vv]$|^[0-9]{12}$")) {
                    etxt_NIC.setError("Invalid NIC Number");
                    Toast.makeText(Register.this, "Invalid NIC Number", Toast.LENGTH_SHORT).show();
                    allValid = false;
                }

                if (!mobile.matches("[0-9]{10}")) {
                    etxt_Mobile.setError("Invalid Mobile Number");
                    Toast.makeText(Register.this, "Mobile number must have 10 digits", Toast.LENGTH_SHORT).show();
                    allValid = false;
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

                            Toast.makeText(Register.this, "Account Created", Toast.LENGTH_SHORT).show();
                            DocumentReference df = fStore.collection("Users").document(fAuth.getCurrentUser().getUid());
                            Map<String, Object> userInfo = new HashMap<>();
                            userInfo.put("Full Name", name);
                            userInfo.put("Email Address", emailAddress);
                            userInfo.put("NIC Number", nic);
                            userInfo.put("Mobile Number", mobile);
                            userInfo.put("Password", password);
//                            userInfo.put("isAdmin", "1");

                            df.set(userInfo);

                            startActivity(new Intent(getApplicationContext(), Login.class));
                            finish();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(Register.this, "Failed to create an account", Toast.LENGTH_SHORT).show();
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