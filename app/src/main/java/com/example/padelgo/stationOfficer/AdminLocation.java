package com.example.padelgo.stationOfficer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

public class AdminLocation extends AppCompatActivity implements OnMapReadyCallback {

    Button btn_Kandy, btn_Katugastota, btn_Peradeniya;
    ImageButton ib_myLocation;
    private GoogleMap padelGoMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final String TAG = "SelectLocation";

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                Boolean fineLocationGranted = permissions.getOrDefault(
                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = permissions.getOrDefault(
                        Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    // Precise location access granted.
                    enableMyLocationOnMap();
                    getDeviceLocationAndCenterMap(false);
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    // Only approximate location access granted.
                    enableMyLocationOnMap();
                    getDeviceLocationAndCenterMap(false);
                } else {
                    // No location access granted.
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();

                    if (padelGoMap != null) {
                        try {
                            padelGoMap.setMyLocationEnabled(false);
                            padelGoMap.getUiSettings().setMyLocationButtonEnabled(false);
                            if (ib_myLocation != null) {
                                ib_myLocation.setVisibility(View.GONE);
                            }
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // The Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapArea);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // OnMapReadyCallback
        } else {
            Log.e(TAG, "MapFragment not found in layout! Check the ID R.id.mapArea.");
            Toast.makeText(this, "Error: Map could not be loaded.", Toast.LENGTH_LONG).show();
        }


        btn_Kandy = findViewById(R.id.BTN_Kandy);
        btn_Katugastota = findViewById(R.id.BTN_Katugastota);
        btn_Peradeniya = findViewById(R.id.BTN_Peradeniya);
        ib_myLocation = findViewById(R.id.IB_myLocation);

        btn_Kandy.setOnClickListener(v -> {
            Intent moveToRent_Kandy = new Intent(getApplicationContext(), AdminBicycleAvailabitily_Kandy.class);
            startActivity(moveToRent_Kandy);
        });
        btn_Katugastota.setOnClickListener(v -> {
            Intent moveToRent_Katugastota = new Intent(getApplicationContext(), AdminBicycleAvailabitily_Katugastota.class);
            startActivity(moveToRent_Katugastota);
        });
        btn_Peradeniya.setOnClickListener(v -> {
            Intent moveToRent_Peradeniya = new Intent(getApplicationContext(), AdminBicycleAvailabitily_Peradeniya.class);
            startActivity(moveToRent_Peradeniya);
        });

        ib_myLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Permissions are granted, get location and center map
                getDeviceLocationAndCenterMap(true); // Pass true to indicate centering is desired by button click
            } else {
                checkAndRequestLocationPermissions(); // trigger the permission request flow
                Toast.makeText(this, "Location permission needed to show current location.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        padelGoMap = googleMap;

        // Apply map customizations (markers, bounds)
        setupMapMarkersAndBounds();

        // Check for location permissions and enable MyLocation layer
        checkAndRequestLocationPermissions();

        // Enable standard UI settings
        padelGoMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private void setupMapMarkersAndBounds() {
        if (padelGoMap == null) return;

        // Locations
        LatLng kandyLocation = new LatLng(7.293115, 80.632889); // Kandy Station
        LatLng katugastotaLocation = new LatLng(7.325782, 80.625922); // Katugastota Station
        LatLng peradeniyaLocation = new LatLng(7.264971, 80.592928); // Peradeniya Station

        // Markers on map
        padelGoMap.addMarker(new MarkerOptions().position(kandyLocation).title("Kandy Station"));
        padelGoMap.addMarker(new MarkerOptions().position(katugastotaLocation).title("Katugastota Station"));
        padelGoMap.addMarker(new MarkerOptions().position(peradeniyaLocation).title("Peradeniya Station"));

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(kandyLocation);
        builder.include(katugastotaLocation);
        builder.include(peradeniyaLocation);
        LatLngBounds bounds = builder.build();

        int padding = 150; // TEAM: if map view is not enough or too large, you can edit this.
        padelGoMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }


    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Fine location permission is already granted
            enableMyLocationOnMap();
            getDeviceLocationAndCenterMap(false); // Pass false: don't auto-center
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Only coarse location permission is granted
            enableMyLocationOnMap();
            getDeviceLocationAndCenterMap(false); // Pass false: don't auto-center
            Toast.makeText(this, "Precise location not available, using approximate location.", Toast.LENGTH_SHORT).show();
        } else {
            // Permissions are not granted, request them.
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void enableMyLocationOnMap() {
        if (padelGoMap == null) {
            return;
        }
        try {
            // Double check permission before enabling
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                padelGoMap.setMyLocationEnabled(true);
                padelGoMap.getUiSettings().setMyLocationButtonEnabled(false);

                if (ib_myLocation != null) { // my location custom image button
                    ib_myLocation.setVisibility(View.VISIBLE);
                }

            } else {
                padelGoMap.setMyLocationEnabled(false);
                padelGoMap.getUiSettings().setMyLocationButtonEnabled(false);
                if (ib_myLocation != null) {
                    ib_myLocation.setVisibility(View.GONE);
                }
                Log.d(TAG, "Location permission not granted, MyLocation layer disabled.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in enableMyLocationOnMap: " + e.getMessage());
        }
    }

    private void getDeviceLocationAndCenterMap(boolean centerMap) {
        if (padelGoMap == null) return;
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "Current location: " + currentLatLng);
                        if (centerMap) { // Only center if requested (e.g., by custom button click)
                            padelGoMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)); // Team: you can ajust this too if zoom want to change
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Location services might be off or no last known location.");
                        if (centerMap) {
                            Toast.makeText(this, "Could not get current location.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                locationResult.addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error getting current location: " + e.getMessage());
                    if (centerMap) {
                        Toast.makeText(this, "Error getting location.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in getDeviceLocationAndCenterMap: " + e.getMessage());
        }
    }
}