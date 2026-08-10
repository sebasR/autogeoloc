package com.example.autogeoloc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 2;

    private TextView positionText;
    private TextView statusText;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable uiRefresh = new Runnable() {
        @Override
        public void run() {
            refreshFromService();
            uiHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        positionText = findViewById(R.id.text_position);
        statusText = findViewById(R.id.text_status);
        findViewById(R.id.button_settings).setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class)));

        if (hasLocationPermission()) {
            startGeolocationService();
        } else {
            requestNeededPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasLocationPermission()) {
            requestNotificationPermissionIfNeeded();
            startGeolocationService();
        }
        uiHandler.post(uiRefresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(uiRefresh);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGeolocationService();
                requestNotificationPermissionIfNeeded();
            } else {
                positionText.setText(R.string.permission_denied);
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            startGeolocationService();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNeededPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        ActivityCompat.requestPermissions(this,
                permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
    }

    private void startGeolocationService() {
        Intent intent = new Intent(this, GeolocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void refreshFromService() {
        Location location = GeolocationService.getLastLocation();
        if (location != null) {
            positionText.setText(String.format(Locale.US, "%.6f, %.6f",
                    location.getLatitude(), location.getLongitude()));
        } else {
            positionText.setText(R.string.gps_position);
        }
        String status = GeolocationService.getLastStatus();
        if (status != null) {
            statusText.setText(status);
            statusText.setTextColor(ContextCompat.getColor(this,
                    GeolocationService.getLastStatusColor()));
        } else {
            statusText.setText(R.string.no_publication);
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_grey));
        }
    }
}
