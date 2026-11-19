package com.example.login;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory user directory. This is a deliberate stand-in for a database in
 * the demo: users disappear on restart.
 */
public final class UserStore {

    private final Map<String, User> users = new ConcurrentHashMap<String, User>();

    public UserStore add(String username, String password, String displayName) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be empty");
        }
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        String name = username.trim();
        users.put(name, new User(name, displayName, PasswordHasher.hash(password)));
        return this;
    }

    public Optional<User> find(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(username));
    }

    public boolean authenticate(String username, String password) {
        Optional<User> user = find(username);
        return user.isPresent() && PasswordHasher.verify(password, user.get().passwordHash());
    }

    public int size() {
        return users.size();
    }

    /** Demo accounts only; passwords are hashed at startup, never stored in clear text. */
    public static UserStore withDemoUsers() {
        return new UserStore()
                .add("demo", "demo123", "Demo User")
                .add("admin", "admin123", "Administrator");
    }

    public static final class User {
        private final String username;
        private final String displayName;
        private final String passwordHash;

        User(String username, String displayName, String passwordHash) {
            this.username = username;
            this.displayName = displayName;
            this.passwordHash = passwordHash;
        }

        public String username() {
            return username;
        }

        public String displayName() {
            return displayName;
        }

        String passwordHash() {
            return passwordHash;
        }
    }
}
