package com.example.login;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void verifiesTheOriginalPassword() {
        String stored = PasswordHasher.hash("s3cret");
        assertTrue(PasswordHasher.verify("s3cret", stored));
        assertFalse(PasswordHasher.verify("s3creT", stored));
    }

    @Test
    void hashesAreSalted() {
        assertNotEquals(PasswordHasher.hash("s3cret"), PasswordHasher.hash("s3cret"));
    }

    @Test
    void rejectsMalformedHashes() {
        assertFalse(PasswordHasher.verify("s3cret", "plaintext"));
        assertFalse(PasswordHasher.verify("s3cret", "pbkdf2_sha256$0$AAAA$BBBB"));
        assertFalse(PasswordHasher.verify("s3cret", "pbkdf2_sha256$notanumber$AAAA$BBBB"));
        assertFalse(PasswordHasher.verify("s3cret", "pbkdf2_sha256$1000$%%%$BBBB"));
        assertFalse(PasswordHasher.verify("s3cret", null));
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("s3cret")));
    }

    @Test
    void storedFormatIsDocumented() {
        String stored = PasswordHasher.hash("s3cret");
        assertTrue(stored.startsWith("pbkdf2_sha256$60000$"), stored);
        assertTrue(stored.split("\\$").length == 4, stored);
    }
}
