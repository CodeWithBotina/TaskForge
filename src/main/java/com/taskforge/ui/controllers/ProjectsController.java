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
import javafx.fxml.FXMLLoader; // Import FXMLLoader
import javafx.scene.Parent;    // Import Parent
import javafx.scene.Scene;     // Import Scene
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality; // Import Modality
import javafx.stage.Stage;     // Import Stage
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the ProjectsView.fxml.
 * This class manages the display and interaction with projects in the system.
 * It populates a TableView with project data, handles project creation,
 * and provides actions for managing individual projects.
 */
public class ProjectsController {

    @FXML
    private TextField newProjectNameField;
    @FXML
    private ChoiceBox<Team> newProjectTeamChoiceBox;
    @FXML
    private Label newProjectMessageLabel;

    @FXML
    private TableView<Project> projectsTable;
    @FXML
    private TableColumn<Project, Integer> projectIdColumn;
    @FXML
    private TableColumn<Project, String> projectNameColumn;
    @FXML
    private TableColumn<Project, String> projectTeamColumn; // Will display team name
    @FXML
    private TableColumn<Project, Void> projectActionsColumn; // For buttons like Edit/Delete

    @FXML
    private Label messageLabel;

    private ProjectManagerService projectManagerService;
    private UserManagerService userManagerService; // Needed to get all teams for choice box
    private ObservableList<Project> projectList = FXCollections.observableArrayList();

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It sets up the table columns,
     * initializes service dependencies, and populates the team choice box.
     */
    @FXML
    public void initialize() {
        // Initialize DAOs
        UserDAO userDAO = new UserDAO();
        TeamDAO teamDAO = new TeamDAO();
        ProjectDAO projectDAO = new ProjectDAO(teamDAO);
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO);

        // Initialize Services
        this.projectManagerService = new ProjectManagerService(projectDAO, teamDAO);
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);

        // Configure table columns
        projectIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        projectNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Custom cell factory for Project Team to display team name
        projectTeamColumn.setCellValueFactory(cellData -> {
            Project project = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    project.getTeam() != null ? project.getTeam().getName() : "None"
            );
        });

        // Custom cell factory for Actions column (Edit, Delete buttons)
        projectActionsColumn.setCellFactory(param -> new TableCell<Project, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                // Set button actions
                editButton.setOnAction(event -> {
                    Project project = getTableView().getItems().get(getIndex());
                    handleEditProject(project);
                });
                deleteButton.setOnAction(event -> {
                    Project project = getTableView().getItems().get(getIndex());
                    handleDeleteProject(project);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        projectsTable.setItems(projectList);

        // Populate the Team ChoiceBox for new project creation
        List<Team> allTeams = userManagerService.getAllTeams();
        // Add a "None" option for unassigned projects
        ObservableList<Team> teamsWithNone = FXCollections.observableArrayList();
        teamsWithNone.add(null); // Represents "No Team"
        teamsWithNone.addAll(allTeams);
        newProjectTeamChoiceBox.setItems(teamsWithNone);
        newProjectTeamChoiceBox.getSelectionModel().selectFirst(); // Select "None" by default

        // Set a custom string converter for Team objects in ChoiceBox
        newProjectTeamChoiceBox.setConverter(new StringConverter<Team>() {
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

        // Automatically load all projects when the view is initialized
        handleRefreshProjects();
    }

    /**
     * Handles the action when the "Add Project" button is clicked.
     * Collects input from the form fields and attempts to create a new project.
     */
    @FXML
    private void handleAddProject() {
        String projectName = newProjectNameField.getText();
        Team selectedTeam = newProjectTeamChoiceBox.getValue();
        int teamId = (selectedTeam != null) ? selectedTeam.getId() : 0; // 0 if "None" selected

        // Basic validation for project creation
        if (projectName.isEmpty()) {
            displayNewProjectMessage("Project name cannot be empty.", true);
            return;
        }

        // Attempt to create the project using the ProjectManagerService
        Optional<Project> createdProject = projectManagerService.createProject(projectName, teamId);

        if (createdProject.isPresent()) {
            displayNewProjectMessage("Project '" + createdProject.get().getName() + "' added successfully!", false);
            handleClearForm(); // Clear the form after successful addition
            handleRefreshProjects(); // Refresh the table to show the new project
        } else {
            displayNewProjectMessage("Failed to add project. Check console for details.", true);
        }
    }

    /**
     * Handles the action when the "Clear Form" button is clicked.
     * Clears all input fields in the new project creation form.
     */
    @FXML
    private void handleClearForm() {
        newProjectNameField.clear();
        newProjectTeamChoiceBox.getSelectionModel().selectFirst(); // Reset to "None"
        newProjectMessageLabel.setText(""); // Clear any messages
    }

    /**
     * Handles the action when the "Refresh Projects" button is clicked.
     * This method reloads all projects from the database.
     */
    @FXML
    private void handleRefreshProjects() {
        System.out.println("Refreshing all projects...");
        projectList.clear(); // Clear existing items
        List<Project> projects = projectManagerService.getAllProjects();
        projectList.addAll(projects); // Add refreshed projects

        if (projects.isEmpty()) {
            displayMessage("No projects found in the system.", false);
        } else {
            displayMessage("Projects refreshed successfully.", false);
        }
    }

    /**
     * Handles the action for the "Edit" button in a project row.
     * This method opens a new modal dialog to edit the selected project.
     *
     * @param project The Project object to be edited.
     */
    private void handleEditProject(Project project) {
        try {
            // Load the FXML for the edit project view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/EditProjectView.fxml"));
            Parent editProjectRoot = loader.load();

            // Get the controller and pass the project to be edited
            EditProjectController editProjectController = loader.getController();
            editProjectController.setProject(project);

            // Create a new stage for the dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Project: " + project.getName());
            dialogStage.initModality(Modality.WINDOW_MODAL); // Make it a modal dialog
            dialogStage.initOwner(projectsTable.getScene().getWindow()); // Set the owner window
            Scene scene = new Scene(editProjectRoot);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false); // Make the dialog not resizable
            dialogStage.showAndWait(); // Show the dialog and wait for it to be closed

            // After the dialog is closed, refresh the projects table in case changes were made
            handleRefreshProjects();

        } catch (IOException e) {
            System.err.println("Error loading EditProjectView.fxml: " + e.getMessage());
            e.printStackTrace();
            displayMessage("Error opening project edit dialog.", true);
        }
    }

    /**
     * Handles the action for the "Delete" button in a project row.
     * This method deletes the selected project from the database.
     *
     * @param project The Project object to be deleted.
     */
    private void handleDeleteProject(Project project) {
        System.out.println("Delete Project button clicked for project: " + project.getName());
        boolean success = projectManagerService.deleteProject(project.getId());
        if (success) {
            displayMessage("Project '" + project.getName() + "' deleted successfully.", false);
            handleRefreshProjects(); // Refresh the table after deletion
        } else {
            displayMessage("Failed to delete project '" + project.getName() + "'.", true);
        }
    }

    /**
     * Displays a message to the user in the main messageLabel.
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

    /**
     * Displays a message specifically for the new project creation section.
     *
     * @param message The message text to display.
     * @param isError True if the message is an error (will be red), false for success/info (will be black).
     */
    private void displayNewProjectMessage(String message, boolean isError) {
        newProjectMessageLabel.setText(message);
        if (isError) {
            newProjectMessageLabel.setStyle("-fx-text-fill: red;");
        } else {
            newProjectMessageLabel.setStyle("-fx-text-fill: black;");
        }
    }
}
