package com.example.poc_concept;


import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    public static String sha256Base64Url(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        // Base64 URL-safe, no padding
        return Base64.encodeToString(hashBytes, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }
}