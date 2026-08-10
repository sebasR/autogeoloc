package com.example.autogeoloc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "geoloc_prefs";
    private static final String KEY_URLS = "publication_urls";
    private static final String KEY_INTERVAL = "refresh_interval_seconds";
    private static final int DEFAULT_INTERVAL_SECONDS = 60;

    private SharedPreferences prefs;
    private EditText urlInput;
    private LinearLayout urlList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        urlInput = findViewById(R.id.input_url);
        urlList = findViewById(R.id.list_urls);

        findViewById(R.id.button_add_url).setOnClickListener(v -> addUrl());
        findViewById(R.id.button_save).setOnClickListener(v -> saveRefreshInterval());
        findViewById(R.id.website_link).setOnClickListener(v -> openWebsite());

        EditText refreshInput = findViewById(R.id.input_refresh);
        refreshInput.setText(String.valueOf(prefs.getInt(KEY_INTERVAL, DEFAULT_INTERVAL_SECONDS)));

        renderList();
    }

    private void addUrl() {
        String url = urlInput.getText().toString().trim();
        if (!isValidUrl(url)) {
            urlInput.setError(getString(R.string.invalid_url));
            return;
        }
        Set<String> urls = new HashSet<>(prefs.getStringSet(KEY_URLS, new HashSet<>()));
        urls.add(url);
        prefs.edit().putStringSet(KEY_URLS, urls).apply();
        urlInput.getText().clear();
        renderList();
    }

    private void removeUrl(String url) {
        Set<String> urls = new HashSet<>(prefs.getStringSet(KEY_URLS, new HashSet<>()));
        urls.remove(url);
        prefs.edit().putStringSet(KEY_URLS, urls).apply();
        renderList();
    }

    private void editUrl(String url) {
        removeUrl(url);
        urlInput.setText(url);
        urlInput.requestFocus();
    }

    private void saveRefreshInterval() {
        EditText refreshInput = findViewById(R.id.input_refresh);
        String text = refreshInput.getText().toString().trim();
        int seconds;
        try {
            seconds = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            seconds = -1;
        }
        if (seconds < 1) {
            refreshInput.setError(getString(R.string.invalid_interval));
            return;
        }
        prefs.edit().putInt(KEY_INTERVAL, seconds).apply();
        Toast.makeText(this, getString(R.string.last_publication,
                String.valueOf(seconds) + "s"), Toast.LENGTH_SHORT).show();
        finish();
    }

    private static boolean isValidUrl(String url) {
        return (url.startsWith("http://") || url.startsWith("https://"))
                && url.contains("{lat}")
                && url.contains("{lon}");
    }

    private void openWebsite() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.website_url))));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open browser", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderList() {
        urlList.removeAllViews();
        for (String url : getUrls()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView urlView = new TextView(this);
            urlView.setText(url);
            urlView.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button removeButton = new Button(this);
            removeButton.setText(R.string.url_remove);
            removeButton.setOnClickListener(v -> removeUrl(url));

            Button editButton = new Button(this);
            editButton.setText(R.string.url_edit);
            editButton.setOnClickListener(v -> editUrl(url));

            row.addView(urlView);
            row.addView(editButton);
            row.addView(removeButton);
            urlList.addView(row);
        }
    }

    private List<String> getUrls() {
        Set<String> stored = prefs.getStringSet(KEY_URLS, new HashSet<>());
        List<String> urls = new ArrayList<>(stored);
        Collections.sort(urls);
        return urls;
    }
}
