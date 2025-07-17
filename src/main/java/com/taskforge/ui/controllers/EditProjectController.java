package com.taskforge.ui.controllers;

import com.taskforge.dao.ProjectDAO;
import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
import com.taskforge.model.Project;
import com.taskforge.model.Team;
import com.taskforge.service.ProjectManagerService;
import com.taskforge.service.UserManagerService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

/**
 * Controller for the EditProjectView.fxml.
 * This class manages the form for editing an existing project's details,
 * including its name and associated team.
 */
public class EditProjectController {

    @FXML
    private Label projectIdLabel;
    @FXML
    private TextField editProjectNameField;
    @FXML
    private ChoiceBox<Team> editProjectTeamChoiceBox;
    @FXML
    private Label messageLabel;

    private Project projectToEdit; // The project object being edited
    private ProjectManagerService projectManagerService;
    private UserManagerService userManagerService; // Needed to get all teams for choice box

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It sets up the ChoiceBox and
     * initializes service dependencies.
     */
    @FXML
    public void initialize() {
        // Initialize DAOs
        UserDAO userDAO = new UserDAO();
        TeamDAO teamDAO = new TeamDAO();
        ProjectDAO projectDAO = new ProjectDAO(teamDAO);
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO); // Required for UserManagerService

        // Initialize Services
        this.projectManagerService = new ProjectManagerService(projectDAO, teamDAO);
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);

        // Populate Team ChoiceBox
        List<Team> allTeams = userManagerService.getAllTeams();
        // Add a "None" option for unassigned projects
        ObservableList<Team> teamsWithNone = FXCollections.observableArrayList();
        teamsWithNone.add(null); // Represents "No Team"
        teamsWithNone.addAll(allTeams);
        editProjectTeamChoiceBox.setItems(teamsWithNone);

        // Set a custom string converter for Team objects in ChoiceBox
        editProjectTeamChoiceBox.setConverter(new StringConverter<Team>() {
            @Override
            public String toString(Team team) {
                return team != null ? team.getName() : "None";
            }

            @Override
            public Team fromString(String string) {
                // Not needed for this use case (selection, not typing)
                return null;
            }
        });
    }

    /**
     * Sets the project object to be edited and populates the form fields with its data.
     * This method should be called by the parent controller (e.g., ProjectsController)
     * before showing this view.
     *
     * @param project The Project object whose details are to be edited.
     */
    public void setProject(Project project) {
        this.projectToEdit = project;
        if (projectToEdit != null) {
            projectIdLabel.setText(String.valueOf(projectToEdit.getId()));
            editProjectNameField.setText(projectToEdit.getName());

            // Select the associated team in the ChoiceBox
            if (projectToEdit.getTeam() != null) {
                editProjectTeamChoiceBox.getSelectionModel().select(projectToEdit.getTeam());
            } else {
                // Select the "None" option if no team is associated
                editProjectTeamChoiceBox.getSelectionModel().selectFirst();
            }
        }
    }

    /**
     * Handles the action when the "Save Changes" button is clicked.
     * Validates input, updates the project object, and persists changes to the database.
     */
    @FXML
    private void handleSaveChanges() {
        if (projectToEdit == null) {
            displayMessage("No project selected for editing.", true);
            return;
        }

        String newProjectName = editProjectNameField.getText();
        Team newTeam = editProjectTeamChoiceBox.getValue();
        int newTeamId = (newTeam != null) ? newTeam.getId() : 0; // 0 if "None" selected

        // Basic validation
        if (newProjectName.isEmpty()) {
            displayMessage("Project name cannot be empty.", true);
            return;
        }

        // Update the project using ProjectManagerService
        boolean success = projectManagerService.updateProject(
                projectToEdit.getId(),
                newProjectName,
                newTeamId
        );

        if (success) {
            displayMessage("Project '" + newProjectName + "' updated successfully!", false);
            // Optionally close the window after successful save
            Stage stage = (Stage) projectIdLabel.getScene().getWindow();
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
        Stage stage = (Stage) projectIdLabel.getScene().getWindow();
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
