package com.example.padelgo.user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.padelgo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VerifyNIC extends AppCompatActivity {

    private ImageView img_ViewFront, img_ViewBack;
    private Button btn_Upload;
    private ProgressBar progressBar;

    private Uri frontUri, backUri;
    private boolean selectingFront = true;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private static final int REQUEST_PERMISSIONS = 101;
    private static final String TAG = "VerifyNIC";
    private String userFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_verify_nic);

        img_ViewFront = findViewById(R.id.imageViewFrontNIC);
        img_ViewBack = findViewById(R.id.imageViewBackNIC);
        btn_Upload = findViewById(R.id.buttonUploadNIC);
        progressBar = findViewById(R.id.progressBarUpload);

        requestPermissionsIfNeeded();
        initCloudinary();
        setupLaunchers();
        fetchUserFullName();

        img_ViewFront.setOnClickListener(v -> {
            selectingFront = true;
            showImageOptions();
        });

        img_ViewBack.setOnClickListener(v -> {
            selectingFront = false;
            showImageOptions();
        });

        btn_Upload.setOnClickListener(v -> {
            if (frontUri == null || backUri == null) {
                Toast.makeText(this, "Upload both front and back images", Toast.LENGTH_SHORT).show();
                return;
            }
            if (userFullName == null || userFullName.isEmpty()) {
                Toast.makeText(this, "Could not retrieve user name. Please try again.", Toast.LENGTH_SHORT).show();
                fetchUserFullName();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            uploadToCloudinary(frontUri, true);
        });
    }

    private void fetchUserFullName() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            userFullName = documentSnapshot.getString("Full Name");
                            if (userFullName != null) {
                                Log.d(TAG, "User full name fetched: " + userFullName);
                            } else {
                                Log.w(TAG, "User full name field is null in Firestore.");
                                Toast.makeText(VerifyNIC.this, "Full name not found in profile.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.w(TAG, "User document does not exist in Firestore.");
                            Toast.makeText(VerifyNIC.this, "User profile not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user full name from Firestore", e);
                        Toast.makeText(VerifyNIC.this, "Error fetching user details.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.w(TAG, "Current user is null, cannot fetch full name.");
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPermissionsIfNeeded() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
                break;
            }
        }
    }

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "djzhajqfj");
        config.put("api_key", "112449326487919");
        config.put("api_secret", "MX9DmDtLXHeEkwDbWDIJ5ar8oF8");
        MediaManager.init(this, config);
    }

    private void showImageOptions() {
        String[] options = {"Select from Gallery", "Capture from Camera"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                        pick.setType("image/*");
                        imagePickerLauncher.launch(pick);
                    } else {
                        try {
                            File photo = createImageFile();
                            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
                            if (selectingFront) frontUri = uri;
                            else backUri = uri;
                            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            camera.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                            cameraLauncher.launch(camera);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).show();
    }

    private void setupLaunchers() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (selectingFront) {
                            frontUri = uri;
                            Picasso.get().load(uri).into(img_ViewFront);
                        } else {
                            backUri = uri;
                            Picasso.get().load(uri).into(img_ViewBack);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (selectingFront) {
                            Picasso.get().load(frontUri).into(img_ViewFront);
                        } else {
                            Picasso.get().load(backUri).into(img_ViewBack);
                        }
                    }
                });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "NIC_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private void uploadToCloudinary(Uri uri, boolean isFront) {
        MediaManager.get().upload(uri).callback(new UploadCallback() {
            @Override public void onStart(String requestId) {}

            @Override public void onProgress(String requestId, long bytes, long totalBytes) {
                progressBar.setProgress((int) ((bytes * 100) / totalBytes));
            }

            @Override public void onSuccess(String requestId, Map resultData) {
                String url = resultData.get("secure_url").toString();
                Log.d("Upload", (isFront ? "Front" : "Back") + " uploaded: " + url);

                if (isFront) {
                    saveToFirebase(url, null); // Save front URL first
                    uploadToCloudinary(backUri, false); // Then upload back
                } else {
                    saveToFirebase(null, url); // Save back URL and other details
                }
            }

            @Override public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(VerifyNIC.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }

            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveToFirebase(String frontUrl, String backUrl) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not logged in. Cannot save verification data.");
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user_verifications").child(uid);

        if (frontUrl != null) {
            ref.child("nic_front_url").setValue(frontUrl);
        }

        if (backUrl != null) {
            Map<String, Object> verificationDetails = new HashMap<>();
            verificationDetails.put("nic_back_url", backUrl);
            verificationDetails.put("status", "pending");
            if (userFullName != null && !userFullName.isEmpty()) {
                verificationDetails.put("fullName", userFullName);
            }

            ref.updateChildren(verificationDetails).addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Realtime DB updated with back URL, status, and full name.");
                FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();
                Map<String, Object> firestoreUpdate = new HashMap<>();
                firestoreUpdate.put("verificationStatus", "pending");
                firestoreUpdate.put("fullName", userFullName);


                dbFirestore.collection("Users").document(uid)
                        .update(firestoreUpdate)
                        .addOnSuccessListener(aVoid2 -> Log.d(TAG, "Firestore status and FullName set to pending"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update Firestore status and FullName", e));

                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "NIC Images Uploaded Successfully!", Toast.LENGTH_SHORT).show();
                finish();
                startActivity(new Intent(this, UserDashboard.class));

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to update Realtime DB for back image details.", e);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error saving details.", Toast.LENGTH_SHORT).show();
            });
        }
    }
}