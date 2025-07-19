package com.taskforge.service;

import com.taskforge.dao.UserDAO;
import com.taskforge.model.User;
import com.taskforge.util.SecurityUtil;

import java.util.Optional;

/**
 * Service class responsible for user authentication and registration.
 * This class orchestrates operations between the UserDAO and SecurityUtil
 * to provide secure user management functionalities.
 * It encapsulates the business logic for user registration and login,
 * including password hashing and uniqueness checks for username and email.
 */
public class AuthService {

    /** The Data Access Object for User entities, used to interact with the database. */
    private final UserDAO userDAO;

    /**
     * Constructs an AuthService with a UserDAO dependency.
     * This allows for dependency injection, making the class more testable.
     *
     * @param userDAO The Data Access Object for User entities.
     */
    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Registers a new user in the system.
     * Before saving, the plain password is hashed using {@link SecurityUtil}.
     * This method performs checks to ensure the username and email are unique
     * before attempting to create the user record in the database.
     *
     * @param username The desired username for the new user.
     * @param email The email address for the new user.
     * @param plainPassword The plain-text password for the new user.
     * @return An {@code Optional} containing the registered {@link User} object if successful,
     * or an empty {@code Optional} if registration fails (e.g., username/email already exists,
     * or password is invalid).
     */
    public Optional<User> registerUser(String username, String email, String plainPassword) {
        // Basic input validation
        if (username == null || username.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                plainPassword == null || plainPassword.trim().isEmpty()) {
            System.err.println("Registration failed: Username, email, or password cannot be empty.");
            return Optional.empty();
        }

        // Check if username or email already exists
        if (userDAO.getUserByUsername(username).isPresent()) {
            System.err.println("Registration failed: Username '" + username + "' already exists.");
            return Optional.empty();
        }
        if (userDAO.getUserByEmail(email).isPresent()) {
            System.err.println("Registration failed: Email '" + email + "' already exists.");
            return Optional.empty();
        }

        try {
            // Hash the plain password before storing
            String hashedPassword = SecurityUtil.hashPassword(plainPassword);

            // Create a new User object
            User newUser = new User(username, email, hashedPassword);

            // Persist the user to the database
            User createdUser = userDAO.createUser(newUser);

            if (createdUser != null) {
                System.out.println("User registered successfully: " + createdUser.getUsername());
                return Optional.of(createdUser);
            } else {
                System.err.println("Registration failed: Database error during user creation.");
                return Optional.empty();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Registration failed due to password issue: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Authenticates a user by checking their username and password.
     * It retrieves the user by username and then verifies the provided plain-text password
     * against the stored hashed password using {@link SecurityUtil}.
     *
     * @param username The username provided by the user.
     * @param plainPassword The plain-text password provided by the user.
     * @return An {@code Optional} containing the authenticated {@link User} object if credentials are valid,
     * or an empty {@code Optional} if authentication fails (e.g., user not found, incorrect password).
     */
    public Optional<User> authenticateUser(String username, String plainPassword) {
        // Basic input validation
        if (username == null || username.trim().isEmpty() ||
                plainPassword == null || plainPassword.trim().isEmpty()) {
            System.err.println("Authentication failed: Username or password cannot be empty.");
            return Optional.empty();
        }

        Optional<User> userOptional = userDAO.getUserByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            try {
                // Check the plain password against the stored hash
                if (SecurityUtil.checkPassword(plainPassword, user.getPasswordHash())) {
                    System.out.println("User authenticated successfully: " + user.getUsername());
                    return Optional.of(user);
                } else {
                    System.err.println("Authentication failed: Incorrect password for user '" + username + "'.");
                    return Optional.empty();
                }
            } catch (RuntimeException e) { // Catch RuntimeException from SecurityUtil.checkPassword
                System.err.println("Authentication failed due to password check error: " + e.getMessage());
                return Optional.empty();
            }
        } else {
            System.err.println("Authentication failed: User '" + username + "' not found.");
            return Optional.empty();
        }
    }
}
