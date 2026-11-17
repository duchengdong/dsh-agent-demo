package com.example.login;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** In-memory bearer-token sessions with a fixed time-to-live. */
public final class SessionStore {

    public static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();
    private final Clock clock;
    private final Duration ttl;

    public SessionStore() {
        this(DEFAULT_TTL, Clock.systemUTC());
    }

    public SessionStore(Duration ttl) {
        this(ttl, Clock.systemUTC());
    }

    public SessionStore(Duration ttl, Clock clock) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.ttl = ttl;
        this.clock = clock;
    }

    public String create(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("username must not be empty");
        }
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        sessions.put(token, new Session(username, clock.instant().plus(ttl)));
        return token;
    }

    public Optional<String> resolve(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (!clock.instant().isBefore(session.expiresAt)) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session.username);
    }

    public boolean revoke(String token) {
        return token != null && sessions.remove(token) != null;
    }

    public Duration ttl() {
        return ttl;
    }

    public int size() {
        return sessions.size();
    }

    private static final class Session {
        private final String username;
        private final Instant expiresAt;

        Session(String username, Instant expiresAt) {
            this.username = username;
            this.expiresAt = expiresAt;
        }
    }
}
