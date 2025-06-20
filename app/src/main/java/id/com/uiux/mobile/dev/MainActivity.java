package id.com.uiux.mobile.dev;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import id.com.uiux.mobile.dev.R;
import id.com.uiux.mobile.dev.databinding.ActivityMainBinding;
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

    private static final String PROJECT_NUMBER = "321385566680";
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



        // Request Play Integrity token using Standard API
        callStandardIntegrityApi();
    }

    public void callStandardIntegrityApi() {
        String originalPayload = "action=purchase&userId=123&timestamp=1723468993";
        String requestHash = sha256Base64Url(originalPayload);

        StandardIntegrityManager integrityManager = IntegrityManagerFactory.createStandard(this);

        integrityManager.prepareIntegrityToken(
                        StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                                .setCloudProjectNumber(Long.parseLong(PROJECT_NUMBER))
                                .build())
                .addOnSuccessListener(tokenProvider -> {
                    tokenProvider.request(
                                    StandardIntegrityTokenRequest.builder()
                                            .setRequestHash(requestHash)
                                            .build())
                            .addOnSuccessListener(response -> {
                                String integrityToken = response.token();
                                Log.d(TAG, "Integrity Token: " + integrityToken);
                                sendToBackend(integrityToken, requestHash); // now uses instance method
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

    private void sendToBackend(String integrityToken, String requestHash) {
        new Thread(() -> {
            boolean isVerified = false;

            try {
                URL url = new URL("http://192.168.60.26:3000/verify-integrity");
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

                    JSONObject jsons = new JSONObject(responseBody);
                    isVerified = jsons.optBoolean("verified", false);

                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error parsing backend response", e);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending token to backend", e);
            }

            boolean finalIsVerified = isVerified;
            runOnUiThread(() -> {
                Bundle bundle = new Bundle();
                bundle.putBoolean("verified_result", finalIsVerified);

                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                if (finalIsVerified) {
                    navController.navigate(R.id.SecondFragment, bundle);
                } else {
                    navController.navigate(R.id.FirstFragment, bundle);
                }
            });

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
//
//    private String generateSecureNonce() {
//        byte[] nonceBytes = new byte[32]; // 256 bits
//        new SecureRandom().nextBytes(nonceBytes);
//        // Use web-safe Base64 (NO_WRAP and URL_SAFE flags), and strip "=" padding
//        return Base64.encodeToString(nonceBytes, Base64.NO_WRAP | Base64.URL_SAFE);
//    }
//
//    private void sendTokenToServer(String token, String nonce) {
//        HttpURLConnection conn = null;
//        boolean isVerified = false; // default to false on failure
//
//        try {
//            URL url = new URL("http://192.168.208.26:3000/verify-integrity");
//            conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
//            conn.setDoOutput(true);
//            conn.setDoInput(true);
//
//            String jsonInputString = String.format(
//                    "{\"integrityToken\":\"%s\",\"expectedNonce\":\"%s\"}",
//                    token, nonce
//            );
//
//            try (OutputStream os = conn.getOutputStream()) {
//                byte[] input = jsonInputString.getBytes("UTF-8");
//                os.write(input, 0, input.length);
//                os.flush();
//            }
//
//            int code = conn.getResponseCode();
//            BufferedReader reader;
//            if (code >= 200 && code < 300) {
//                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
//            } else {
//                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
//            }
//
//            StringBuilder response = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                response.append(line.trim());
//            }
//
//            String serverResponse = response.toString();
//            Log.d(TAG, "Server Response: " + serverResponse);
//
//            // ✅ Extract the 'verified' boolean from JSON
//            isVerified = serverResponse.contains("\"verified\":true");
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error sending token to server", e);
//        } finally {
//            if (conn != null) conn.disconnect();
//
//            // 🔄 Now switch to UI thread
//            boolean finalIsVerified = isVerified;
//            runOnUiThread(() -> {
//                Bundle bundle = new Bundle();
//                bundle.putBoolean("verified_result", finalIsVerified);
//
//                NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment_content_main);
//                if (finalIsVerified) {
//                    navController.navigate(R.id.SecondFragment, bundle);
//                } else {
//                    navController.navigate(R.id.FirstFragment, bundle);
//                }
//            });
//        }
//    }

}