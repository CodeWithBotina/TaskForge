package com.taskforge.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @Test
    void testHashPassword() {
        String password = "securePassword123!";
        String hashedPassword = SecurityUtil.hashPassword(password);

        assertNotNull(hashedPassword, "Hashed password should not be null");
        assertTrue(hashedPassword.contains(":"), "Hashed password should contain salt and hash separated by colon");

        String[] parts = hashedPassword.split(":");
        assertEquals(2, parts.length, "Hashed password should have exactly two parts (salt and hash)");
        assertTrue(parts[0].length() > 0, "Salt should not be empty");
        assertTrue(parts[1].length() > 0, "Hash should not be empty");
    }

    @Test
    void testCheckPasswordCorrect() {
        String password = "correctPassword123!";
        String hashedPassword = SecurityUtil.hashPassword(password);
        assertTrue(SecurityUtil.checkPassword(password, hashedPassword));
    }

    @Test
    void testCheckPasswordIncorrect() {
        String password = "correctPassword123!";
        String wrongPassword = "wrongPassword456!";
        String hashedPassword = SecurityUtil.hashPassword(password);
        assertFalse(SecurityUtil.checkPassword(wrongPassword, hashedPassword));
    }

    @Test
    void testHashPasswordNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.hashPassword(null);
        }, "Should throw IllegalArgumentException for null password");
    }

    @Test
    void testHashPasswordEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.hashPassword("");
        }, "Should throw IllegalArgumentException for empty password");
    }

    @Test
    void testCheckPasswordNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.checkPassword(null, "salt:hash");
        }, "Should throw IllegalArgumentException for null plain password");
    }

    @Test
    void testCheckPasswordEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.checkPassword("", "salt:hash");
        }, "Should throw IllegalArgumentException for empty plain password");
    }

    @Test
    void testCheckPasswordNullStored() {
        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.checkPassword("password", null);
        }, "Should throw IllegalArgumentException for null stored password");
    }

    @Test
    void testCheckPasswordEmptyStored() {
        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.checkPassword("password", "");
        }, "Should throw IllegalArgumentException for empty stored password");
    }

    @Test
    void testCheckPasswordInvalidFormat() {
        String validPassword = "password123";

        // Test various invalid formats
        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.checkPassword(validPassword, "missingcolon");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.checkPassword(validPassword, "toomany:colons:here");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.checkPassword(validPassword, "notbase64:notbase64");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.checkPassword(validPassword, ":emptyhash");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtil.checkPassword(validPassword, "emptysalt:");
        });
    }
}