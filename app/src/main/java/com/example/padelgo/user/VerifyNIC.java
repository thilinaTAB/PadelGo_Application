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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VerifyNIC extends AppCompatActivity {

    private ImageView imageViewFront, imageViewBack;
    private Button buttonUpload;
    private ProgressBar progressBar;

    private Uri frontUri, backUri;
    private boolean selectingFront = true;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private static final int REQUEST_PERMISSIONS = 101;
    private static final String TAG = "VerifyNIC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_verify_nic);

        imageViewFront = findViewById(R.id.imageViewFrontNIC);
        imageViewBack = findViewById(R.id.imageViewBackNIC);
        buttonUpload = findViewById(R.id.buttonUploadNIC);
        progressBar = findViewById(R.id.progressBarUpload);

        requestPermissionsIfNeeded();
        initCloudinary();
        setupLaunchers();

        imageViewFront.setOnClickListener(v -> {
            selectingFront = true;
            showImageOptions();
        });

        imageViewBack.setOnClickListener(v -> {
            selectingFront = false;
            showImageOptions();
        });

        buttonUpload.setOnClickListener(v -> {
            if (frontUri == null || backUri == null) {
                Toast.makeText(this, "Upload both front and back images", Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            uploadToCloudinary(frontUri, true);
        });
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
                            Picasso.get().load(uri).into(imageViewFront);
                        } else {
                            backUri = uri;
                            Picasso.get().load(uri).into(imageViewBack);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (selectingFront) {
                            Picasso.get().load(frontUri).into(imageViewFront);
                        } else {
                            Picasso.get().load(backUri).into(imageViewBack);
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
                    saveToFirebase(url, null);
                    uploadToCloudinary(backUri, false);
                } else {
                    saveToFirebase(null, url);
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
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user_verifications").child(uid);

        if (frontUrl != null) ref.child("nic_front_url").setValue(frontUrl);
        if (backUrl != null) {
            ref.child("nic_back_url").setValue(backUrl);
            ref.child("status").setValue("pending");
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "NIC Images Uploaded Successfully!", Toast.LENGTH_SHORT).show();
        }
    }
}
