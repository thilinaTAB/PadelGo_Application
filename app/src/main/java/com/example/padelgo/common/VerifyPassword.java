package com.example.padelgo.common;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class VerifyPassword extends AppCompatActivity {


    EditText etxt_email, etxt_nic, etxt_mobile;
    Button btn_action;
    TextView txt_btnlogin, txt_verify, txt_name, txt_exPassword,lbl_name,lbl_password,lbl_email;

    FirebaseFirestore fStore;

    private String verifiedUserFullName = null;
    private String verifiedUserPassword = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_common_verify_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etxt_email = findViewById(R.id.ETXT_Email);
        etxt_nic = findViewById(R.id.ETXT_NIC);
        etxt_mobile = findViewById(R.id.ETXT_Mobile);
        txt_exPassword = findViewById(R.id.TXT_exPassword);
        btn_action = findViewById(R.id.BTN_Action);
        txt_btnlogin = findViewById(R.id.TXT_btnLogin);
        txt_verify = findViewById(R.id.TXT_Verify);
        txt_name = findViewById(R.id.TXT_Name);
        txt_exPassword = findViewById(R.id.TXT_exPassword);
        lbl_name = findViewById(R.id.LBL_name);
        lbl_password = findViewById(R.id.LBL_password);
        lbl_email = findViewById(R.id.LBL_email);


        fStore = FirebaseFirestore.getInstance();

        setupInitialUIState();

        txt_btnlogin.setOnClickListener(view -> {
            startActivity(new Intent(VerifyPassword.this, Login.class));
            finish();
        });

        btn_action.setOnClickListener(view -> {

                handleVerifyUserDetails();
        });
    }

    private void setupInitialUIState() {
        btn_action.setText("Verify Details");

        etxt_email.setVisibility(View.VISIBLE);
        etxt_nic.setVisibility(View.VISIBLE);
        etxt_mobile.setVisibility(View.VISIBLE);
        etxt_email.setEnabled(true);
        etxt_nic.setEnabled(true);
        etxt_mobile.setEnabled(true);

        txt_name.setVisibility(View.GONE);
        txt_exPassword.setVisibility(View.GONE);

        verifiedUserFullName = null;
    }

    private void setupEnterNewPasswordUIState() {
        btn_action.setText("Back to Login");
        txt_verify.setText("Your Credentials");

        etxt_email.setVisibility(View.GONE);
        etxt_nic.setVisibility(View.GONE);
        etxt_mobile.setVisibility(View.GONE);
        txt_btnlogin.setVisibility(View.GONE);

        if (verifiedUserFullName != null && !verifiedUserFullName.isEmpty()) {
            txt_name.setText(verifiedUserFullName);
            txt_exPassword.setText(verifiedUserPassword);

            lbl_name.setVisibility(View.VISIBLE);
            txt_name.setVisibility(View.VISIBLE);
            lbl_email.setVisibility(View.VISIBLE);
            etxt_email.setVisibility(View.VISIBLE);
            lbl_password.setVisibility(View.VISIBLE);
            txt_exPassword.setVisibility(View.VISIBLE);

            btn_action.setOnClickListener(view -> {
                startActivity(new Intent(VerifyPassword.this, Login.class));
                finish();
            });

        } else {
            txt_name.setVisibility(View.GONE);
            txt_exPassword.setVisibility(View.GONE);
        }


    }

    private boolean validateField(EditText editText, String fieldName, boolean isEmail) {
        String value = editText.getText().toString().trim();
        if (TextUtils.isEmpty(value)) {
            editText.setError(fieldName + " is required.");
            return false;
        }
        if (isEmail && !Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            editText.setError("Invalid email format.");
            return false;
        }
        if (editText.getId() == R.id.ETXT_NIC && !value.matches("^[0-9]{9}[Vv]$|^[0-9]{12}$")) {
            editText.setError("Invalid NIC format (e.g., 123456789V or 199012345678).");
            return false;
        }
        if (editText.getId() == R.id.ETXT_Mobile && !value.matches("^[0-9]{10}$")) { // Assuming 10 digits for mobile
            editText.setError("Mobile number must be 10 digits.");
            return false;
        }
        editText.setError(null);
        return true;
    }

    private void handleVerifyUserDetails() {
        boolean emailValid = validateField(etxt_email, "Email", true);
        boolean nicValid = validateField(etxt_nic, "NIC", false);
        boolean mobileValid = validateField(etxt_mobile, "Mobile number", false);

        if (!emailValid || !nicValid || !mobileValid) {
            Toast.makeText(this, "Please correct the errors.", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = etxt_email.getText().toString().trim();
        String nic = etxt_nic.getText().toString().trim();
        String mobile = etxt_mobile.getText().toString().trim();

        btn_action.setEnabled(false);
        Toast.makeText(this, "Verifying...", Toast.LENGTH_SHORT).show();

        fStore.collection("Users")
                .whereEqualTo("Email Address", email)
                .whereEqualTo("NIC Number", nic)
                .whereEqualTo("Mobile Number", mobile)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    btn_action.setEnabled(true);
                    if (task.isSuccessful()) {
                        QuerySnapshot documents = task.getResult();
                        if (documents != null && !documents.isEmpty()) {
                            QueryDocumentSnapshot userDoc = (QueryDocumentSnapshot) documents.getDocuments().get(0);
                            verifiedUserFullName = userDoc.getString("Full Name");
                            verifiedUserPassword = userDoc.getString("Password");

                            Toast.makeText(VerifyPassword.this, "User verified.", Toast.LENGTH_SHORT).show();
                            setupEnterNewPasswordUIState();
                        } else {
                            Toast.makeText(VerifyPassword.this, "No user found with the provided details.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.w("Error getting documents: ", task.getException());
                        Toast.makeText(VerifyPassword.this, "Error verifying details: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}