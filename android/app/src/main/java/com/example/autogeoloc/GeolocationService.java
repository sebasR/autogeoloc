package com.example.autogeoloc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeolocationService extends Service {

    private static final String CHANNEL_ID = "geoloc";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "geoloc_prefs";
    private static final String KEY_URLS = "publication_urls";
    private static final String KEY_INTERVAL = "refresh_interval_seconds";
    private static final int DEFAULT_INTERVAL_SECONDS = 60;
    private static final int HTTP_TIMEOUT_MS = 10000;

    private static volatile Location lastLocation;
    private static volatile String lastStatus;
    private static volatile int lastStatusColor = R.color.status_grey;

    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences prefs;
    private boolean running = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable publicationLoop = new Runnable() {
        @Override
        public void run() {
            publicationCycle();
            handler.postDelayed(this, refreshIntervalMillis());
        }
    };

    public static Location getLastLocation() {
        return lastLocation;
    }

    public static String getLastStatus() {
        return lastStatus;
    }

    public static int getLastStatusColor() {
        return lastStatusColor;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundNotification();
        if (!running) {
            running = true;
            handler.post(publicationLoop);
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(publicationLoop);
        executor.shutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForegroundNotification() {
        Notification notification = buildNotification(lastStatus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String status) {
        Intent openApp = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openApp, flags);
        String text = status != null ? status : getString(R.string.notification_active);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_stat_geoloc)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String status) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, buildNotification(status));
    }

    private long refreshIntervalMillis() {
        int seconds = prefs.getInt(KEY_INTERVAL, DEFAULT_INTERVAL_SECONDS);
        return Math.max(1, seconds) * 1000L;
    }

    private void publicationCycle() {
        if (!running) {
            return;
        }
        List<String> urls = getUrls();
        if (urls.isEmpty()) {
            setStatus(getString(R.string.no_publication), R.color.status_grey);
            return;
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,
                        new CancellationToken() {
                            @Override
                            public boolean isCancellationRequested() {
                                return false;
                            }

                            @NonNull
                            @Override
                            public CancellationToken onCanceledRequested(
                                    @NonNull OnTokenCanceledListener onTokenCanceledListener) {
                                return this;
                            }
                        })
                .addOnSuccessListener(this::handleLocation)
                .addOnFailureListener(e ->
                        setStatus(getString(R.string.position_error), R.color.status_red));
    }

    private void handleLocation(Location location) {
        if (location == null) {
            setStatus(getString(R.string.position_unknown), R.color.status_grey);
            return;
        }
        lastLocation = location;
        publish(location);
    }

    private void publish(Location location) {
        List<String> urls = getUrls();
        if (urls.isEmpty()) {
            setStatus(getString(R.string.no_publication), R.color.status_grey);
            return;
        }
        executor.execute(() -> {
            boolean allOk = true;
            for (String url : urls) {
                allOk &= send(buildPublicationUrl(url, location));
            }
            boolean ok = allOk;
            handler.post(() -> {
                if (ok) {
                    String time = new SimpleDateFormat("HH:mm:ss", Locale.US)
                            .format(new Date());
                    setStatus(getString(R.string.last_publication, time), R.color.status_green);
                } else {
                    setStatus(getString(R.string.publish_error), R.color.status_red);
                }
            });
        });
    }

    private void setStatus(String status, int colorRes) {
        lastStatus = status;
        lastStatusColor = colorRes;
        updateNotification(status);
    }

    private String buildPublicationUrl(String template, Location location) {
        String lat = URLEncoder.encode(String.format(Locale.US, "%.6f",
                location.getLatitude()), StandardCharsets.UTF_8);
        String lon = URLEncoder.encode(String.format(Locale.US, "%.6f",
                location.getLongitude()), StandardCharsets.UTF_8);
        return template.replace("{lat}", lat).replace("{lon}", lon);
    }

    private boolean send(String target) {
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) new URL(target).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            int code = connection.getResponseCode();
            connection.disconnect();
            return code >= 200 && code < 300;
        } catch (IOException e) {
            return false;
        }
    }

    private List<String> getUrls() {
        Set<String> stored = prefs.getStringSet(KEY_URLS, new HashSet<>());
        List<String> urls = new ArrayList<>(stored);
        Collections.sort(urls);
        return urls;
    }
}
