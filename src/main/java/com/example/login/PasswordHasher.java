package com.example.login;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2-HMAC-SHA256 password hashing. Stored format:
 * {@code pbkdf2_sha256$<iterations>$<salt-base64url>$<hash-base64url>}.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 60_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        return PREFIX + "$" + ITERATIONS + "$" + encode(salt) + "$" + encode(derived);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (iterations <= 0) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = decode(parts[2]);
            expected = decode(parts[3]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (salt.length == 0 || expected.length == 0) {
            return false;
        }
        byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("invalid PBKDF2 parameters", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
