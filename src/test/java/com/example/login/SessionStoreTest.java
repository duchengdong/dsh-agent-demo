package com.example.login;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionStoreTest {

    @Test
    void resolvesValidToken() {
        SessionStore store = new SessionStore(Duration.ofMinutes(30));
        String token = store.create("demo");
        assertEquals("demo", store.resolve(token).orElse(null));
    }

    @Test
    void tokenExpiresAfterTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        SessionStore store = new SessionStore(Duration.ofMinutes(30), clock);
        String token = store.create("demo");

        clock.advance(Duration.ofMinutes(29));
        assertTrue(store.resolve(token).isPresent());

        clock.advance(Duration.ofMinutes(1));
        assertFalse(store.resolve(token).isPresent());
    }

    @Test
    void revokedTokenNoLongerResolves() {
        SessionStore store = new SessionStore();
        String token = store.create("demo");
        assertTrue(store.revoke(token));
        assertFalse(store.resolve(token).isPresent());
        assertFalse(store.revoke(token));
    }

    @Test
    void tokensAreUnique() {
        SessionStore store = new SessionStore();
        assertNotEquals(store.create("demo"), store.create("demo"));
    }

    @Test
    void rejectsNullOrEmptyInput() {
        SessionStore store = new SessionStore();
        assertFalse(store.resolve(null).isPresent());
        assertFalse(store.resolve("").isPresent());
        assertFalse(store.revoke(null));
    }

    @Test
    void exposesConfiguredTtl() {
        SessionStore store = new SessionStore(Duration.ofSeconds(90));
        assertEquals(90, store.ttl().getSeconds());
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
