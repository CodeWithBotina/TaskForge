package com.taskforge.ui.controllers;

import com.taskforge.dao.UserDAO;
import com.taskforge.model.User;
import com.taskforge.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Controller for the RegisterView.fxml.
 * This class handles user interactions on the registration screen,
 * delegating business logic to the AuthService and managing navigation.
 */
public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button registerButton;
    @FXML
    private Label messageLabel;

    private AuthService authService;

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {
        // Initialize the AuthService with a new UserDAO instance.
        this.authService = new AuthService(new UserDAO());
    }

    /**
     * Handles the action when the Register button is clicked.
     * Attempts to register a new user with the provided username, email, and password.
     * Checks if passwords match.
     */
    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            displayMessage("All fields are required for registration.", true);
            return;
        }

        if (!password.equals(confirmPassword)) {
            displayMessage("Passwords do not match.", true);
            return;
        }

        Optional<User> registeredUser = authService.registerUser(username, email, password);

        if (registeredUser.isPresent()) {
            displayMessage("Registration successful! You can now log in.", false);
            // Clear fields after successful registration
            usernameField.clear();
            emailField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
            // Optionally, navigate back to login after successful registration
            // handleBackToLogin(new ActionEvent()); // Auto-navigate
        } else {
            // Error message will be displayed by AuthService, but we can add a generic one here too
            displayMessage("Registration failed. Please check inputs or try a different username/email.", true);
        }
    }

    /**
     * Handles the action when the "Back to Login" button is clicked.
     * Navigates the user back to the LoginView.
     *
     * @param event The ActionEvent that triggered this method.
     */
    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/LoginView.fxml"));
            Parent root = loader.load();

            // Get the current stage from the button
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 800, 600); // Maintain consistent window size
            stage.setScene(scene);
            stage.setTitle("TaskForge - Login"); // Update window title
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading LoginView.fxml: " + e.getMessage());
            e.printStackTrace();
            displayMessage("Error loading login screen.", true);
        }
    }

    /**
     * Displays a message to the user in the messageLabel.
     *
     * @param message The message text to display.
     * @param isError True if the message is an error (will be red), false for success/info (will be black).
     */
    private void displayMessage(String message, boolean isError) {
        messageLabel.setText(message);
        if (isError) {
            messageLabel.setStyle("-fx-text-fill: red;");
        } else {
            messageLabel.setStyle("-fx-text-fill: black;");
        }
    }
}
