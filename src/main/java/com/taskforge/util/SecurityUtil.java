package com.taskforge.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Utility class for handling security-related operations, primarily password hashing.
 * It uses the PBKDF2WithHmacSHA256 algorithm for secure password storage and verification.
 * This class is designed to be stateless and provides static methods for common security tasks.
 */
public class SecurityUtil {

    // Recommended iterations for PBKDF2 (NIST recommends at least 10,000 for PBKDF2-HMAC-SHA256)
    private static final int ITERATIONS = 10000;
    // Key length in bits (256 bits for SHA256)
    private static final int KEY_LENGTH = 256;
    // Salt length in bytes
    private static final int SALT_LENGTH = 16; // 128 bits

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SecurityUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Hashes a plain-text password using the PBKDF2WithHmacSHA256 algorithm.
     * A random salt is generated for each hash, ensuring that identical passwords produce different hashes.
     * The salt and the hashed password are combined and Base64 encoded for storage.
     *
     * @param plainPassword The plain-text password to hash. Must not be null or empty.
     * @return The Base64 encoded string containing the salt and the derived key,
     * formatted as "salt:hashedPassword".
     * @throws IllegalArgumentException if the {@code plainPassword} is null or empty.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }

        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt); // Generate a random salt

            PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // Combine salt and hash, then Base64 encode for storage
            String encodedSalt = Base64.getEncoder().encodeToString(salt);
            String encodedHash = Base64.getEncoder().encodeToString(hash);

            return encodedSalt + ":" + encodedHash;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.err.println("Error hashing password: " + e.getMessage());
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored hashed password.
     * The stored hashed password is expected to be in "salt:hashedPassword" Base64 encoded format.
     *
     * @param plainPassword The plain-text password provided by the user for verification.
     * @param storedHashedPassword The hashed password retrieved from the database,
     * which includes the salt and the derived key.
     * @return {@code true} if the {@code plainPassword} matches the {@code storedHashedPassword},
     * {@code false} otherwise.
     * @throws IllegalArgumentException if either {@code plainPassword} or {@code storedHashedPassword}
     * is null or empty, or if {@code storedHashedPassword} is in an invalid format.
     */
    public static boolean checkPassword(String plainPassword, String storedHashedPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Plain password cannot be null or empty for checking.");
        }
        if (storedHashedPassword == null || storedHashedPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Hashed password cannot be null or empty for checking.");
        }

        String[] parts = storedHashedPassword.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid stored hashed password format.");
        }

        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);

            PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] enteredHash = skf.generateSecret(spec).getEncoded();

            return java.security.MessageDigest.isEqual(storedHash, enteredHash);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid stored hashed password format.", e);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.err.println("Error checking password: " + e.getMessage());
            return false;
        }
    }
}
