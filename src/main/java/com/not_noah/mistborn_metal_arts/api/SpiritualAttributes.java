package com.not_noah.mistborn_metal_arts.api;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class SpiritualAttributes {
    public String identity;
    public float stability;
    public float contamination;
    public float scarring;

    private Map<String, Float> connections = new HashMap<>();

    public Map<String, Float> getConnections() {
        return connections;
    }

    public static String generateIdentity() {
        // Generate secure random bytes
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        try {
            // Get MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(randomBytes);

            // Convert bytes to Hexadecimal String
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be supported in every standard JVM
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public float getConnection(String connectorType) {
        if (this.connections.containsKey(connectorType)) {
            return this.connections.get(connectorType);
        }
        return 0.0F;
    }

    public void merge(SpiritualAttributes other) {
        if (other == null)
            return;
        this.stability += other.stability;
        this.contamination += other.contamination;
        this.scarring += other.scarring;
        if (other.connections != null) {
            other.connections.forEach((k, v) -> {
                this.connections.merge(k, v, Float::sum);
            });
        }
    }
}