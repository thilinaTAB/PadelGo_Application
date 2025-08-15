package com.example.padelgo.user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.padelgo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;


public class SelectLocation extends AppCompatActivity implements OnMapReadyCallback {

    Button btn_Kandy, btn_Katugastota, btn_Peradeniya;
    ImageButton ib_myLocation;
    private GoogleMap padelGoMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final String TAG = "SelectLocation";

    private static final LatLng KANDY_LOCATION = new LatLng(7.293115, 80.632889);
    private static final LatLng KATUGASTOTA_LOCATION = new LatLng(7.325782, 80.625922);
    private static final LatLng PERADENIYA_LOCATION = new LatLng(7.264971, 80.592928);

    private Marker kandyMarker, katugastotaMarker, peradeniyaMarker;

    private FirebaseAuth fAuth;
    private DatabaseReference userVerificationRef;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                Boolean fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    enableMyLocationOnMap();
                    getDeviceLocationAndCenterMap(false);
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    enableMyLocationOnMap();
                    getDeviceLocationAndCenterMap(false);
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                    if (padelGoMap != null) {
                        try {
                            padelGoMap.setMyLocationEnabled(false);
                            padelGoMap.getUiSettings().setMyLocationButtonEnabled(false);
                            if (ib_myLocation != null) ib_myLocation.setVisibility(View.GONE);
                        } catch (SecurityException se) {
                            Log.e(TAG, "SecurityException while disabling MyLocation: " + se.getMessage());
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_common_select_location);
        db = FirebaseFirestore.getInstance();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser != null) {
            userVerificationRef = FirebaseDatabase.getInstance().getReference("user_verifications").child(currentUser.getUid());
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapArea);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "MapFragment not found in layout! Check the ID R.id.mapArea.");
            Toast.makeText(this, "Error: Map could not be loaded.", Toast.LENGTH_LONG).show();
        }

        btn_Kandy = findViewById(R.id.BTN_Kandy);
        btn_Katugastota = findViewById(R.id.BTN_Katugastota);
        btn_Peradeniya = findViewById(R.id.BTN_Peradeniya);
        ib_myLocation = findViewById(R.id.IB_myLocation);

        float defaultZoomLevel = 15f;

        btn_Kandy.setOnClickListener(v -> moveToLocation(KANDY_LOCATION, kandyMarker, defaultZoomLevel));
        btn_Katugastota.setOnClickListener(v -> moveToLocation(KATUGASTOTA_LOCATION, katugastotaMarker, defaultZoomLevel));
        btn_Peradeniya.setOnClickListener(v -> moveToLocation(PERADENIYA_LOCATION, peradeniyaMarker, defaultZoomLevel));

        ib_myLocation.setOnClickListener(v -> {
            if (hasLocationPermission()) {
                getDeviceLocationAndCenterMap(true);
            } else {
                checkAndRequestLocationPermissions();
                Toast.makeText(this, "Location permission needed to show current location.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void moveToLocation(LatLng location, Marker marker, float zoomLevel) {
        if (padelGoMap != null) {
            padelGoMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));
            if (marker != null) marker.showInfoWindow();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        padelGoMap = googleMap;

        if (padelGoMap == null) {
            Toast.makeText(this, "Error: Map object is null.", Toast.LENGTH_LONG).show();
            return;
        }

        setupMapMarkersAndBounds();
        checkAndRequestLocationPermissions();
        padelGoMap.getUiSettings().setZoomControlsEnabled(true);

        padelGoMap.setOnMarkerClickListener(marker -> {
            FirebaseUser currentUser = fAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please log in to proceed.", Toast.LENGTH_SHORT).show();
                return true; // Consume the click
            }
            String userId = currentUser.getUid();

            checkUserVerificationStatus(userId, () -> {
                checkActiveRideStatus(userId, (canProceed, message) -> {
                    if (canProceed) {
                        // User is verified and has no active/unpaid rides, proceed to specific location
                        if (marker.equals(kandyMarker)) {
                            startActivity(new Intent(this, RentBicycle_Kandy.class));
                        } else if (marker.equals(katugastotaMarker)) {
                            startActivity(new Intent(this, RentBicycle_Katugastota.class));
                        } else if (marker.equals(peradeniyaMarker)) {
                            startActivity(new Intent(this, RentBicycle_Peradeniya.class));
                        }
                    } else {
                        // User has an active/unpaid ride or an error occurred
                        new AlertDialog.Builder(SelectLocation.this)
                                .setTitle("Cannot Start New Ride")
                                .setMessage(message)
                                .setPositiveButton("Go to My Rides", (dialog, which) -> {
                                    Intent intent = new Intent(SelectLocation.this, MyRides.class);
                                    startActivity(intent);
                                })
                                .setNegativeButton("OK", null)
                                .show();
                    }
                });
            });
            return true;
        });

    }


    private void checkUserVerificationStatus(String userId, Runnable onSuccess) {
        if (userVerificationRef == null) {
            Toast.makeText(this, "Could not check verification status. Please try again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "userVerificationRef is null in checkUserVerificationStatus. User ID: " + userId);
            if (userId != null && !userId.isEmpty()) {
                userVerificationRef = FirebaseDatabase.getInstance().getReference("user_verifications").child(userId);
            } else {
                return;
            }
        }

        userVerificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String status = dataSnapshot.child("status").getValue(String.class);
                    if ("approved".equalsIgnoreCase(status)) {
                        onSuccess.run();
                    } else {
                        Toast.makeText(SelectLocation.this, "Your account needs to be verified.", Toast.LENGTH_LONG).show();
                        new AlertDialog.Builder(SelectLocation.this)
                                .setTitle("Sorry! Can't proceed")
                                .setMessage("Your account needs to be verified.")
                                .setNegativeButton("OK", (dialog, which) -> GotoAccount())
                                .show();
                    }
                } else {
                    Toast.makeText(SelectLocation.this, "Verification status not found. Please contact support.", Toast.LENGTH_LONG).show();
                    new AlertDialog.Builder(SelectLocation.this)
                            .setTitle("Sorry! Can't proceed")
                            .setMessage("Please Verify your account first. Thank you.")
                            .setNegativeButton("OK", (dialog, which) -> GotoAccount())
                            .show();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read user verification status.", databaseError.toException());
                Toast.makeText(SelectLocation.this, "Failed to check verification status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private interface RideStatusCallback {
        void onStatusChecked(boolean canProceed, String message);
    }

    private void checkActiveRideStatus(String userId, RideStatusCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onStatusChecked(false, "User not logged in.");
            return;
        }

        db.collection("RideHistory").document(userId)
                .collection("rides")
                .orderBy("serverTimestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No ride history, user can proceed
                        callback.onStatusChecked(true, "No previous rides.");
                        return;
                    }

                    DocumentSnapshot lastRide = queryDocumentSnapshots.getDocuments().get(0);
                    String rideStatus = lastRide.getString("rideStatus");
                    String paymentStatus = lastRide.getString("payment");
                    String extraCharge = lastRide.getString("extraPay");

                    // Check if the ride is ongoing or active
                    if ("ongoing".equalsIgnoreCase(rideStatus) && !"Paid".equalsIgnoreCase(paymentStatus)) {
                        callback.onStatusChecked(false, "You have an ongoing ride. Please complete or cancel it first.");
                    }
//                     Check if the ride is active but not paid
                    else if ("Active".equalsIgnoreCase(rideStatus)) {
                        callback.onStatusChecked(false, "You have a requested ride. Please complete the ride first.");
                    }
                    // Check if the ride is booked but not yet paid and not yet started/active (this means it's pending payment or start)
                    else if ("Paid".equalsIgnoreCase(paymentStatus) && "ongoing".equalsIgnoreCase(rideStatus)){
                        callback.onStatusChecked(false, "You have a pending ride booking. Please go to 'My Rides' to manage it.");
                    }
                    // Check if the last ride extra charges remain or not
                    else if ("Unpaid".equalsIgnoreCase(extraCharge)){
                        callback.onStatusChecked(false, "You have to pay last ride Extra charges first.");
                    }
                    else {
                        // Ride is completed and paid, or cancelled, user can proceed
                        callback.onStatusChecked(true, "Ready for a new ride.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking active ride status: ", e);
                    callback.onStatusChecked(false, "Could not verify ride status. Please try again.");
                });
    }


    private void setupMapMarkersAndBounds() {
        if (padelGoMap == null) return;

        BitmapDescriptor customMarkerIcon = BitmapDescriptorFactory.fromBitmap(
                Bitmap.createScaledBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.location_pin),
                        120, 120, false));

        kandyMarker = padelGoMap.addMarker(new MarkerOptions().position(KANDY_LOCATION).title("Select Kandy Station").icon(customMarkerIcon));
        katugastotaMarker = padelGoMap.addMarker(new MarkerOptions().position(KATUGASTOTA_LOCATION).title("Select Katugastota Station").icon(customMarkerIcon));
        peradeniyaMarker = padelGoMap.addMarker(new MarkerOptions().position(PERADENIYA_LOCATION).title("Select Peradeniya Station").icon(customMarkerIcon));

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(KANDY_LOCATION)
                .include(KATUGASTOTA_LOCATION)
                .include(PERADENIYA_LOCATION)
                .build();

        View mapView = getSupportFragmentManager().findFragmentById(R.id.mapArea).getView();
        if (mapView != null) {
            mapView.post(() -> padelGoMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150)));
        } else {
            padelGoMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        }
    }

    private void checkAndRequestLocationPermissions() {
        if (hasLocationPermission()) {
            enableMyLocationOnMap();
            getDeviceLocationAndCenterMap(false);
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void enableMyLocationOnMap() {
        if (padelGoMap == null) return;

        try {
            if (hasLocationPermission()) {
                padelGoMap.setMyLocationEnabled(true);
                padelGoMap.getUiSettings().setMyLocationButtonEnabled(false);
                if (ib_myLocation != null) ib_myLocation.setVisibility(View.VISIBLE);
            } else {
                padelGoMap.setMyLocationEnabled(false);
                padelGoMap.getUiSettings().setMyLocationButtonEnabled(false);
                if (ib_myLocation != null) ib_myLocation.setVisibility(View.GONE);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in enableMyLocationOnMap: " + e.getMessage());
        }
    }

    private void getDeviceLocationAndCenterMap(boolean centerMap) {
        if (padelGoMap == null) return;

        try {
            if (hasLocationPermission()) {
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (centerMap)
                            padelGoMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                    } else if (centerMap) {
                        Toast.makeText(this, "Could not get current location.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(this, e -> {
                    if (centerMap)
                        Toast.makeText(this, "Error getting location.", Toast.LENGTH_SHORT).show();
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in getDeviceLocationAndCenterMap: " + e.getMessage());
        }
    }

    private void GotoAccount()
    {
        Intent moveToAccount = new Intent(getApplicationContext(),UserNewProfile.class);
        startActivity(moveToAccount);
    }
}