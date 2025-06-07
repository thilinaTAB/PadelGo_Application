package com.example.padelgo.common;

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
import com.example.padelgo.stationOfficer.AdminDash;
import com.example.padelgo.user.Register;
import com.example.padelgo.user.UserDashboard;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    EditText etxt_Email, etxt_Password;
    Button btn_Login;
    TextView txt_btnForgot, txt_btnRegister;

    Boolean valid = false;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_common_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etxt_Email = findViewById(R.id.ETXT_Email);
        etxt_Password = findViewById(R.id.ETXT_Password);
        btn_Login = findViewById(R.id.BTN_Login);
        txt_btnForgot = findViewById(R.id.TXT_btnForgot);
        txt_btnRegister = findViewById(R.id.TXT_btnRegister);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        txt_btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent moveReg = new Intent(Login.this, Register.class);
                startActivity(moveReg);
            }
        });
        txt_btnForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent moveReset = new Intent(Login.this, VerifyPassword.class);
                startActivity(moveReset);
            }
        });

        btn_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkField(etxt_Email);
                checkField(etxt_Password);

                String emailVal = etxt_Email.getText().toString().trim();
                String passVal = etxt_Password.getText().toString().trim();

                if (valid) {
                    fAuth.signInWithEmailAndPassword(emailVal, passVal).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Toast.makeText(Login.this, "Successfull Sign in", Toast.LENGTH_SHORT).show();
                            checkUserAccess(authResult.getUser().getUid());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            txt_btnForgot.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
    }

    public boolean checkField(EditText ex) {
        if (ex.getText().toString().trim().isEmpty()) {
            ex.setError("Fill This");
            valid = false;
        } else {
            valid = true;
        }
        return valid;
    }

    public void checkUserAccess(String uid) {
        DocumentReference df = fStore.collection("Users").document(uid);
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                if (documentSnapshot.getString("isAdmin") != null) {
                    startActivity(new Intent(getApplicationContext(), AdminDash.class));
                    finish();
                } else {
                    startActivity(new Intent(getApplicationContext(), UserDashboard.class));
                    finish();
                }
            }
        });
    }
}