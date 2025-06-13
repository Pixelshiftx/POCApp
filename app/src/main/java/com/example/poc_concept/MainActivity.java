package com.example.poc_concept;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.poc_concept.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PlayIntegrity";
    private static final String PROJECT_NUMBER = "263046549381"; // your project number

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // Navigation setup
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Floating Action Button
        binding.fab.setOnClickListener(view ->
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show()
        );

        // Request Play Integrity token
        requestPlayIntegrityToken();
    }

    private void requestPlayIntegrityToken() {
        IntegrityManager integrityManager = IntegrityManagerFactory.create(this);

        String secureNonce = generateSecureNonce();


        IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(Long.parseLong(PROJECT_NUMBER))
                .setNonce(secureNonce)
                .build();

        integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener(response -> {
                    String token = response.token();
                    Log.d(TAG, "Integrity Token: " + token);
                    Log.d(TAG, "Nonce: " + secureNonce);
                    Toast.makeText(this, "Integrity Token received!", Toast.LENGTH_SHORT).show();

                    // Send to local server for validation
                    new Thread(() -> sendTokenToServer(token, secureNonce)).start();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Integrity Token request failed", e);
                    Toast.makeText(this, "Failed to retrieve Integrity Token.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    private String generateSecureNonce() {
        byte[] nonceBytes = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(nonceBytes);
        // Use web-safe Base64 (NO_WRAP and URL_SAFE flags), and strip "=" padding
        return Base64.encodeToString(nonceBytes, Base64.NO_WRAP | Base64.URL_SAFE);
    }

    private void sendTokenToServer(String token, String nonce) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://172.20.10.9:3000/verify-integrity");

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Build the JSON payload
            String jsonInputString = String.format(
                    "{\"integrityToken\":\"%s\",\"expectedNonce\":\"%s\"}",
                    token, nonce
            );

            // Write JSON to request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("UTF-8");
                os.write(input, 0, input.length);
                os.flush();
            }

            int code = conn.getResponseCode();
            Log.d(TAG, "Server Response Code: " + code);

            BufferedReader reader;
            if (code >= 200 && code < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }

            Log.d(TAG, "Server Response: " + response.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error sending token to server", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

}