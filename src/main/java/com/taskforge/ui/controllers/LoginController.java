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
 * Controller for the LoginView.fxml.
 * This class handles user interactions on the login screen,
 * delegating business logic to the AuthService and managing navigation to registration.
 */
public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
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
     * Handles the action when the Login button is clicked.
     * Attempts to authenticate the user using the provided username and password.
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            displayMessage("Username and password cannot be empty.", true);
            return;
        }

        Optional<User> authenticatedUser = authService.authenticateUser(username, password);

        if (authenticatedUser.isPresent()) {
            displayMessage("Login successful! Welcome, " + authenticatedUser.get().getUsername() + ".", false);
            // Navigate to the main application dashboard
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/DashboardView.fxml"));
                Parent dashboardRoot = loader.load();

                // Get the controller of the loaded FXML
                DashboardController dashboardController = loader.getController();
                // Pass the authenticated user to the DashboardController
                dashboardController.setLoggedInUser(authenticatedUser.get());

                // Get the current stage from the login button
                Stage stage = (Stage) loginButton.getScene().getWindow();
                Scene scene = new Scene(dashboardRoot, 1000, 700); // Set a suitable size for the dashboard
                stage.setScene(scene);
                stage.setTitle("TaskForge - Dashboard"); // Update window title
                stage.show();
            } catch (IOException e) {
                System.err.println("Error loading DashboardView.fxml after login: " + e.getMessage());
                e.printStackTrace();
                displayMessage("Login successful, but failed to load dashboard. Please restart.", true);
            }
        } else {
            displayMessage("Login failed. Invalid username or password.", true);
        }
    }

    /**
     * Handles the action when the "Go to Register" button is clicked.
     * Navigates the user to the RegisterView.
     *
     * @param event The ActionEvent that triggered this method.
     */
    @FXML
    private void handleGoToRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/RegisterView.fxml"));
            Parent root = loader.load();

            // Get the current stage from the button
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 800, 600); // Maintain consistent window size
            stage.setScene(scene);
            stage.setTitle("TaskForge - Register"); // Update window title
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading RegisterView.fxml: " + e.getMessage());
            e.printStackTrace();
            displayMessage("Error loading registration screen.", true);
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
