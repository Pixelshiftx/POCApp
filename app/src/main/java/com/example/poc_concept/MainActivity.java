package com.example.poc_concept;

import android.os.Bundle;
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

        IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(Long.parseLong(PROJECT_NUMBER))
                .setNonce("secure_nonce_123456") // Replace with secure, random nonce for production
                .build();

        integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener(response -> {
                    String token = response.token();
                    Log.d(TAG, "Integrity Token: " + token);
                    Toast.makeText(this, "Integrity Token received!", Toast.LENGTH_SHORT).show();
                    // TODO: send token to server for validation
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
}