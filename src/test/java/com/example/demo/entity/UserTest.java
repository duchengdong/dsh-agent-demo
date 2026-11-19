package com.example.demo.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void allArgsConstructorExposesAllFields() {
        User user = new User(7L, "alice", "Alice", false);

        assertEquals(7L, user.getId());
        assertEquals("alice", user.getUsername());
        assertEquals("Alice", user.getDisplayName());
        assertFalse(user.isEnabled());
    }

    @Test
    void noArgConstructorAndSettersAllowBuildingAnEntity() {
        User user = new User();
        user.setId(8L);
        user.setUsername("bob");
        user.setDisplayName("Bob");
        user.setEnabled(true);

        assertEquals(8L, user.getId());
        assertEquals("bob", user.getUsername());
        assertEquals("Bob", user.getDisplayName());
        assertTrue(user.isEnabled());
    }

    @Test
    void equalsAndHashCodeUseOnlyThePrimaryKey() {
        User original = new User(1L, "demo", "Demo User", true);
        User sameId = new User(1L, "other", "Other name", false);
        User differentId = new User(2L, "demo", "Demo User", true);

        assertEquals(original, original);
        assertEquals(original, sameId);
        assertEquals(original.hashCode(), sameId.hashCode());
        assertNotEquals(original, differentId);
        assertNotEquals(original, null);
        assertNotEquals(original, "not-a-user");
    }

    @Test
    void toStringIncludesFieldValues() {
        String text = new User(1L, "demo", "Demo User", true).toString();

        assertTrue(text.contains("id=1"));
        assertTrue(text.contains("username='demo'"));
        assertTrue(text.contains("displayName='Demo User'"));
        assertTrue(text.contains("enabled=true"));
    }
}
