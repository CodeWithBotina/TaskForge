package com.taskforge.service;

import com.taskforge.dao.UserDAO;
import com.taskforge.model.User;
import com.taskforge.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserDAO userDAO;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userDAO);
    }

    @Test
    void registerUser_Success() {
        // Arrange
        String username = "testuser";
        String email = "test@example.com";
        String password = "password123";
        User mockUser = new User(username, email, "hashedPassword");

        when(userDAO.getUserByUsername(username)).thenReturn(Optional.empty());
        when(userDAO.getUserByEmail(email)).thenReturn(Optional.empty());
        when(userDAO.createUser(any(User.class))).thenReturn(mockUser);

        // Act
        Optional<User> result = authService.registerUser(username, email, password);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userDAO, times(1)).createUser(any(User.class));
    }

    @Test
    void registerUser_UsernameExists() {
        // Arrange
        String username = "existinguser";
        String email = "test@example.com";
        String password = "password123";

        when(userDAO.getUserByUsername(username)).thenReturn(Optional.of(new User()));

        // Act
        Optional<User> result = authService.registerUser(username, email, password);

        // Assert
        assertFalse(result.isPresent());
        verify(userDAO, never()).createUser(any(User.class));
    }

    @Test
    void authenticateUser_Success() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String hashedPassword = SecurityUtil.hashPassword(password);
        User mockUser = new User(username, "test@example.com", hashedPassword);

        when(userDAO.getUserByUsername(username)).thenReturn(Optional.of(mockUser));

        // Act
        Optional<User> result = authService.authenticateUser(username, password);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
    }

    @Test
    void authenticateUser_WrongPassword() {
        // Arrange
        String username = "testuser";
        String correctPassword = "password123";
        String wrongPassword = "wrongpassword";
        String hashedPassword = SecurityUtil.hashPassword(correctPassword);
        User mockUser = new User(username, "test@example.com", hashedPassword);

        when(userDAO.getUserByUsername(username)).thenReturn(Optional.of(mockUser));

        // Act
        Optional<User> result = authService.authenticateUser(username, wrongPassword);

        // Assert
        assertFalse(result.isPresent());
    }
}