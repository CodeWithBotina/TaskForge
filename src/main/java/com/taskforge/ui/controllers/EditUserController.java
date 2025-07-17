package com.taskforge.ui.controllers;

import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
import com.taskforge.model.User;
import com.taskforge.service.UserManagerService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Controller for the EditUserView.fxml.
 * This class manages the form for editing a user's profile details,
 * specifically their username and email. It interacts with the UserManagerService
 * to perform the update operations.
 */
public class EditUserController {

    @FXML
    private Label userIdLabel;
    @FXML
    private TextField editUsernameField;
    @FXML
    private TextField editEmailField;
    @FXML
    private Label messageLabel;

    private User userToEdit; // The user object being edited (should always be the current user)
    private UserManagerService userManagerService;

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It initializes service dependencies.
     */
    @FXML
    public void initialize() {
        // Initialize DAOs
        UserDAO userDAO = new UserDAO();
        TeamDAO teamDAO = new TeamDAO(); // Needed for UserTeamDAO
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO); // Needed for UserManagerService

        // Initialize Service
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);
    }

    /**
     * Sets the user object to be edited and populates the form fields with its data.
     * This method should be called by the parent controller (e.g., UsersController)
     * before showing this view.
     *
     * @param user The User object whose details are to be edited.
     */
    public void setUser(User user) {
        this.userToEdit = user;
        if (userToEdit != null) {
            userIdLabel.setText("User ID: " + userToEdit.getId());
            editUsernameField.setText(userToEdit.getUsername());
            editEmailField.setText(userToEdit.getEmail());
        }
    }

    /**
     * Handles the action when the "Save Changes" button is clicked.
     * Validates input, updates the user object, and persists changes to the database.
     */
    @FXML
    private void handleSaveChanges() {
        if (userToEdit == null) {
            displayMessage("No user selected for editing.", true);
            return;
        }

        String newUsername = editUsernameField.getText();
        String newEmail = editEmailField.getText();

        // Basic validation
        if (newUsername.isEmpty() || newEmail.isEmpty()) {
            displayMessage("Username and email cannot be empty.", true);
            return;
        }

        // Attempt to update the user using UserManagerService
        boolean success = userManagerService.updateUser(
                userToEdit.getId(),
                newUsername,
                newEmail
        );

        if (success) {
            displayMessage("Profile updated successfully!", false);
            // Update the userToEdit object in memory as well
            userToEdit.setUsername(newUsername);
            userToEdit.setEmail(newEmail);
            // Optionally close the window after successful save
            Stage stage = (Stage) userIdLabel.getScene().getWindow();
            stage.close();
        } else {
            // Error message from service would be printed to console, display generic failure
            displayMessage("Failed to save changes. Username or email might already exist.", true);
        }
    }

    /**
     * Handles the action when the "Cancel" button is clicked.
     * Closes the editing window without saving changes.
     */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) userIdLabel.getScene().getWindow();
        stage.close();
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
