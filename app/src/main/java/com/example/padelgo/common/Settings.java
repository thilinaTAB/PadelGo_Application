package com.example.padelgo.common;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.padelgo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.AuthCredential;

public class Settings extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private Button changePasswordButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_settings);

        auth = FirebaseAuth.getInstance();

        currentPasswordEditText = findViewById(R.id.editTextCurrentPassword);
        newPasswordEditText = findViewById(R.id.editTextNewPassword);
        changePasswordButton = findViewById(R.id.buttonChangePassword);

        changePasswordButton.setOnClickListener(v -> handleChangePassword());


    }

    private void handleChangePassword() {
        FirebaseUser user = auth.getCurrentUser();
        String currentPassword = currentPasswordEditText.getText().toString();
        String newPassword = newPasswordEditText.getText().toString();

        if (user != null && !currentPassword.isEmpty() && !newPassword.isEmpty()) {
            String email = user.getEmail();
            if (email != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

                user.reauthenticate(credential)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                user.updatePassword(newPassword)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            } else {
                                                Toast.makeText(this, "Failed to change password: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Could not retrieve email. Re-login required.", Toast.LENGTH_LONG).show();
                auth.signOut();
                finish();
            }

        } else {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
        }
    }
}