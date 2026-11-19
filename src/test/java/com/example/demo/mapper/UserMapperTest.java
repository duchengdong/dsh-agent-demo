package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
    }

    @Test
    void insertAssignsSequentialIdsWhenIdIsMissing() {
        User alice = mapper.insert(new User(null, "alice", "Alice", true));
        User bob = mapper.insert(new User(null, "bob", "Bob", true));

        assertEquals(1L, alice.getId());
        assertEquals(2L, bob.getId());
        assertEquals(2, mapper.count());
    }

    @Test
    void insertKeepsExplicitIdAndContinuesAfterIt() {
        User explicit = mapper.insert(new User(10L, "alice", "Alice", true));
        User generated = mapper.insert(new User(null, "bob", "Bob", true));

        assertEquals(10L, explicit.getId());
        assertEquals(11L, generated.getId());
    }

    @Test
    void insertRejectsDuplicateId() {
        mapper.insert(new User(7L, "alice", "Alice", true));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.insert(new User(7L, "bob", "Bob", true)));

        assertTrue(error.getMessage().contains("7"));
        assertEquals(1, mapper.count());
    }

    @Test
    void findByIdReturnsStoredUserOrEmpty() {
        User alice = mapper.insert(new User(null, "alice", "Alice", true));

        Optional<User> found = mapper.findById(alice.getId());
        assertTrue(found.isPresent());
        assertSame(alice, found.get());

        assertFalse(mapper.findById(999L).isPresent());
    }

    @Test
    void findAllPreservesInsertionOrder() {
        User alice = mapper.insert(new User(null, "alice", "Alice", true));
        User bob = mapper.insert(new User(null, "bob", "Bob", false));

        List<User> users = mapper.findAll();

        assertEquals(2, users.size());
        assertSame(alice, users.get(0));
        assertSame(bob, users.get(1));
    }

    @Test
    void findAllReturnsACopyThatDoesNotAffectTheMapper() {
        mapper.insert(new User(null, "alice", "Alice", true));

        List<User> snapshot = mapper.findAll();
        snapshot.clear();

        assertEquals(1, mapper.count());
    }

    @Test
    void updateReplacesExistingRecord() {
        User alice = mapper.insert(new User(null, "alice", "Alice", true));
        User renamed = new User(alice.getId(), "alice", "Alice Smith", false);

        assertTrue(mapper.update(renamed));
        assertSame(renamed, mapper.findById(alice.getId()).get());
    }

    @Test
    void updateReturnsFalseWhenRecordDoesNotExist() {
        assertFalse(mapper.update(new User(42L, "ghost", "Ghost", true)));
        assertFalse(mapper.update(new User(null, "ghost", "Ghost", true)));
        assertEquals(0, mapper.count());
    }

    @Test
    void deleteByIdRemovesOnlyMatchingRecord() {
        User alice = mapper.insert(new User(null, "alice", "Alice", true));
        mapper.insert(new User(null, "bob", "Bob", true));

        assertTrue(mapper.deleteById(alice.getId()));
        assertFalse(mapper.deleteById(alice.getId()));
        assertEquals(1, mapper.count());
        assertFalse(mapper.findById(alice.getId()).isPresent());
    }

    @Test
    void nullUserIsRejected() {
        assertThrows(NullPointerException.class, () -> mapper.insert(null));
        assertThrows(NullPointerException.class, () -> mapper.update(null));
    }
}
