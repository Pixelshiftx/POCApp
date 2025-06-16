package com.example.poc_concept;

import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.poc_concept.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.StandardIntegrityManager;
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PlayIntegrity";

    private static final String PROJECT_NUMBER = "263046549381";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(view ->
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show()
        );

        // Request Play Integrity token using Standard API
        callStandardIntegrityApi(this);
    }

    public static void callStandardIntegrityApi(Context context) {
        String originalPayload = "action=purchase&userId=123&timestamp=1723468993";
        String requestHash = sha256Base64Url(originalPayload);

        StandardIntegrityManager integrityManager =
                IntegrityManagerFactory.createStandard(context);
        integrityManager.prepareIntegrityToken(
                        StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                                .setCloudProjectNumber(Long.parseLong(PROJECT_NUMBER))
                                .build())
                .addOnSuccessListener(tokenProvider -> {
                    tokenProvider
                            .request(StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                                    .setRequestHash(requestHash)
                                    .build())
                            .addOnSuccessListener(response -> {
                                String integrityToken = response.token();
                                Log.d(TAG, "Integrity Token: " + integrityToken);
                                Log.d(TAG, "Hash : " + requestHash);
                                sendToBackend(integrityToken, requestHash);
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Token request failed", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Prepare token failed", e));
    }

    private static String sha256Base64Url(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hashBytes, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 hashing failed", e);
            return null;
        }
    }

    private static void sendToBackend(String integrityToken, String requestHash) {
        new Thread(() -> {
            try {
                // TODO: Replace with your actual backend IP address or domain
                URL url = new URL("http://172.20.10.9:3000/verify-integrity");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                String json = String.format(
                        "{\"integrityToken\":\"%s\",\"requestHash\":\"%s\"}",
                        integrityToken, requestHash
                );

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                try (InputStream is = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    String responseBody = response.toString();
                    Log.d(TAG, "Backend response: " + responseBody);

                    // OPTIONAL: Parse message field if response is JSON like { "message": "something" }
                    try {
                        JSONObject json_ = new JSONObject(responseBody);
                        if (json_.has("message")) {
                            String message = json_.getString("message");
                            Log.d(TAG, "Backend message: " + message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse JSON response", e);
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Error reading backend response", e);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending token to backend", e);
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item) ||
                NavigationUI.onNavDestinationSelected(item, Navigation.findNavController(this, R.id.nav_host_fragment_content_main));
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
