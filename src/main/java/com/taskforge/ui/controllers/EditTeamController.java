package com.taskforge.ui.controllers;

import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
import com.taskforge.model.Team;
import com.taskforge.service.UserManagerService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Controller for the EditTeamView.fxml.
 * This class manages the form for editing an existing team's details,
 * including its name.
 */
public class EditTeamController {

    @FXML
    private Label teamIdLabel;
    @FXML
    private TextField editTeamNameField;
    @FXML
    private Label messageLabel;

    private Team teamToEdit; // The team object being edited
    private UserManagerService userManagerService; // Needed for updating team details

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It initializes service dependencies.
     */
    @FXML
    public void initialize() {
        // Initialize DAOs
        UserDAO userDAO = new UserDAO();
        TeamDAO teamDAO = new TeamDAO();
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO);

        // Initialize Service
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);
    }

    /**
     * Sets the team object to be edited and populates the form fields with its data.
     * This method should be called by the parent controller (e.g., TeamsController)
     * before showing this view.
     *
     * @param team The Team object whose details are to be edited.
     */
    public void setTeam(Team team) {
        this.teamToEdit = team;
        if (teamToEdit != null) {
            teamIdLabel.setText(String.valueOf(teamToEdit.getId()));
            editTeamNameField.setText(teamToEdit.getName());
        }
    }

    /**
     * Handles the action when the "Save Changes" button is clicked.
     * Validates input, updates the team object, and persists changes to the database.
     */
    @FXML
    private void handleSaveChanges() {
        if (teamToEdit == null) {
            displayMessage("No team selected for editing.", true);
            return;
        }

        String newTeamName = editTeamNameField.getText();

        // Basic validation
        if (newTeamName.isEmpty()) {
            displayMessage("Team name cannot be empty.", true);
            return;
        }

        // Update the team using UserManagerService
        boolean success = userManagerService.updateTeam(
                teamToEdit.getId(),
                newTeamName
        );

        if (success) {
            displayMessage("Team '" + newTeamName + "' updated successfully!", false);
            // Optionally close the window after successful save
            Stage stage = (Stage) teamIdLabel.getScene().getWindow();
            stage.close();
        } else {
            displayMessage("Failed to save changes. Check console for details.", true);
        }
    }

    /**
     * Handles the action when the "Cancel" button is clicked.
     * Closes the editing window without saving changes.
     */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) teamIdLabel.getScene().getWindow();
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
