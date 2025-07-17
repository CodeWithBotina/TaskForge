package com.taskforge.ui.controllers;

import com.taskforge.dao.NotificationDAO;
import com.taskforge.dao.ProjectDAO;
import com.taskforge.dao.TaskDAO;
import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
import com.taskforge.model.Priority;
import com.taskforge.model.Project;
import com.taskforge.model.Status;
import com.taskforge.model.Task;
import com.taskforge.model.User;
import com.taskforge.model.UserTeamMembership; // Import UserTeamMembership
import com.taskforge.model.Visibility; // Import Visibility enum
import com.taskforge.service.ProjectManagerService;
import com.taskforge.service.TaskManagerService;
import com.taskforge.service.UserManagerService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the EditTaskView.fxml.
 * This class manages the form for editing an existing task's details,
 * including its title, description, due date, priority, status, assigned user,
 * associated project, and its visibility.
 * It enforces rules for task editing permissions, assignment, and project association.
 */
public class EditTaskController {

    @FXML
    private Label taskIdLabel;
    @FXML
    private TextField editTaskTitleField;
    @FXML
    private TextArea editTaskDescriptionArea;
    @FXML
    private DatePicker editTaskDueDatePicker;
    @FXML
    private ChoiceBox<Priority> editTaskPriorityChoiceBox;
    @FXML
    private ChoiceBox<Status> editTaskStatusChoiceBox;
    @FXML
    private ChoiceBox<User> editTaskAssignedToChoiceBox; // Displays User objects
    @FXML
    private ChoiceBox<Project> editTaskProjectChoiceBox; // Displays Project objects
    @FXML
    private ChoiceBox<Visibility> editTaskVisibilityChoiceBox; // New: ChoiceBox for Visibility
    @FXML
    private Label messageLabel;

    private Task taskToEdit; // The task object being edited
    private User currentUser; // The currently logged-in user (updater), passed from MyTasksController
    private TaskManagerService taskManagerService;
    private UserManagerService userManagerService;
    private ProjectManagerService projectManagerService;

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It sets up the ChoiceBoxes and
     * initializes service dependencies.
     */
    @FXML
    public void initialize() {
        // Initialize DAOs
        UserDAO userDAO = new UserDAO();
        TeamDAO teamDAO = new TeamDAO();
        ProjectDAO projectDAO = new ProjectDAO(teamDAO);
        TaskDAO taskDAO = new TaskDAO(userDAO, projectDAO);
        NotificationDAO notificationDAO = new NotificationDAO(userDAO);
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO); // Required for UserManagerService and TaskManagerService

        // Initialize Services
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);
        // Corrected TaskManagerService instantiation to pass userManagerService
        this.taskManagerService = new TaskManagerService(taskDAO, userDAO, projectDAO, notificationDAO, userTeamDAO, userManagerService);
        this.projectManagerService = new ProjectManagerService(projectDAO, teamDAO);

        // Populate Priority ChoiceBox
        editTaskPriorityChoiceBox.setItems(FXCollections.observableArrayList(Priority.values()));

        // Populate Status ChoiceBox
        editTaskStatusChoiceBox.setItems(FXCollections.observableArrayList(Status.values()));

        // Populate Visibility ChoiceBox
        editTaskVisibilityChoiceBox.setItems(FXCollections.observableArrayList(Visibility.values()));

        // Configure Assigned To (Users) ChoiceBox
        editTaskAssignedToChoiceBox.setConverter(new javafx.util.StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user != null ? user.getUsername() : "Unassigned";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });

        // Configure Project ChoiceBox
        editTaskProjectChoiceBox.setConverter(new javafx.util.StringConverter<Project>() {
            @Override
            public String toString(Project project) {
                return project != null ? project.getName() : "No Project";
            }

            @Override
            public Project fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Sets the currently logged-in user. This is crucial for permission checks
     * related to task assignment and project association.
     *
     * @param user The User object representing the logged-in user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        populateAssignedToAndProjectChoiceBoxes(); // Populate based on current user's team memberships
    }

    /**
     * Populates the "Assigned To" and "Project" ChoiceBoxes based on the current user's team memberships.
     * This ensures users can only assign tasks to team members or associate with projects in their teams.
     */
    private void populateAssignedToAndProjectChoiceBoxes() {
        if (currentUser == null) {
            System.err.println("Cannot populate assignedTo and project choice boxes: currentUser is null.");
            return;
        }

        // Populate Assigned To (Users) ChoiceBox - Filter by users in common teams
        List<User> allUsers = userManagerService.getAllUsers();
        ObservableList<User> assignableUsers = FXCollections.observableArrayList();
        assignableUsers.add(null); // Option for "Unassigned"

        // Add users who share at least one team with the current user, plus the current user themselves
        for (User user : allUsers) {
            if (user.equals(currentUser) || userManagerService.areUsersInSameTeam(currentUser.getId(), user.getId())) {
                assignableUsers.add(user);
            }
        }
        editTaskAssignedToChoiceBox.setItems(assignableUsers);


        // Populate Project ChoiceBox - Filter by projects in teams the current user is a member of
        List<Project> allProjects = projectManagerService.getAllProjects();
        ObservableList<Project> assignableProjects = FXCollections.observableArrayList();
        assignableProjects.add(null); // Option for "No Project"

        // Add projects whose associated team the current user is a member of
        for (Project project : allProjects) {
            if (project.getTeam() == null || userManagerService.isUserMemberOfTeam(currentUser.getId(), project.getTeam().getId())) {
                assignableProjects.add(project);
            }
        }
        editTaskProjectChoiceBox.setItems(assignableProjects);

        // Re-select current assignedTo and project if they are still valid options
        if (taskToEdit != null) {
            if (taskToEdit.getAssignedTo() != null && assignableUsers.contains(taskToEdit.getAssignedTo())) {
                editTaskAssignedToChoiceBox.getSelectionModel().select(taskToEdit.getAssignedTo());
            } else {
                editTaskAssignedToChoiceBox.getSelectionModel().selectFirst(); // Select "Unassigned"
            }

            if (taskToEdit.getProject() != null && assignableProjects.contains(taskToEdit.getProject())) {
                editTaskProjectChoiceBox.getSelectionModel().select(taskToEdit.getProject());
            } else {
                editTaskProjectChoiceBox.getSelectionModel().selectFirst(); // Select "No Project"
            }
        }
    }


    /**
     * Sets the task object to be edited and populates the form fields with its data.
     * This method should be called by the parent controller (e.g., MyTasksController)
     * before showing this view.
     *
     * @param task The Task object whose details are to be edited.
     */
    public void setTask(Task task) {
        this.taskToEdit = task;
        if (taskToEdit != null) {
            taskIdLabel.setText(String.valueOf(taskToEdit.getId()));
            editTaskTitleField.setText(taskToEdit.getTitle());
            editTaskDescriptionArea.setText(taskToEdit.getDescription() != null ? taskToEdit.getDescription() : "");
            editTaskDueDatePicker.setValue(taskToEdit.getDueDate() != null ? taskToEdit.getDueDate().toLocalDate() : null);
            editTaskPriorityChoiceBox.getSelectionModel().select(taskToEdit.getPriority());
            editTaskStatusChoiceBox.getSelectionModel().select(taskToEdit.getStatus());
            editTaskVisibilityChoiceBox.getSelectionModel().select(taskToEdit.getVisibility()); // Select Visibility

            // Populate and select assignedTo and project after currentUser is set
            // The actual selection will happen in populateAssignedToAndProjectChoiceBoxes()
        }
    }

    /**
     * Handles the action when the "Save Changes" button is clicked.
     * Validates input, updates the task object, and persists changes to the database.
     */
    @FXML
    private void handleSaveChanges() {
        if (taskToEdit == null) {
            displayMessage("No task selected for editing.", true);
            return;
        }
        if (currentUser == null) {
            displayMessage("No user logged in. Cannot save changes.", true);
            return;
        }

        String title = editTaskTitleField.getText();
        String description = editTaskDescriptionArea.getText();
        LocalDateTime dueDate = null;
        if (editTaskDueDatePicker.getValue() != null) {
            dueDate = editTaskDueDatePicker.getValue().atTime(LocalTime.MAX); // Set to end of day
        }
        Priority priority = editTaskPriorityChoiceBox.getValue();
        Status status = editTaskStatusChoiceBox.getValue();
        User assignedTo = editTaskAssignedToChoiceBox.getValue(); // Get selected User object
        Project project = editTaskProjectChoiceBox.getValue();   // Get selected Project object
        Visibility visibility = editTaskVisibilityChoiceBox.getValue(); // Get selected Visibility

        // Basic validation
        if (title.isEmpty()) {
            displayMessage("Task title cannot be empty.", true);
            return;
        }
        if (priority == null) {
            displayMessage("Please select a priority.", true);
            return;
        }
        if (status == null) {
            displayMessage("Please select a status.", true);
            return;
        }
        if (visibility == null) {
            displayMessage("Please select a visibility level.", true);
            return;
        }

        // Ensure description is null if empty or only whitespace
        String finalDescription = (description != null && !description.trim().isEmpty()) ? description.trim() : null;

        // Update the task using TaskManagerService
        boolean success = taskManagerService.updateTask(
                taskToEdit.getId(),
                title,
                finalDescription,
                dueDate,
                priority,
                status,
                assignedTo != null ? assignedTo.getId() : 0, // Pass 0 if unassigned
                project != null ? project.getId() : 0,      // Pass 0 if no project
                visibility,
                currentUser.getId() // Pass the ID of the user performing the update (must be creator)
        );

        if (success) {
            displayMessage("Task '" + title + "' updated successfully!", false);
            // Optionally close the window after successful save
            Stage stage = (Stage) taskIdLabel.getScene().getWindow();
            stage.close();
        } else {
            // Error message from service would be printed to console, display generic failure
            displayMessage("Failed to save changes. Check console for details. (e.g., permission, invalid assignment)", true);
        }
    }

    /**
     * Handles the action when the "Cancel" button is clicked.
     * Closes the editing window without saving changes.
     */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) taskIdLabel.getScene().getWindow();
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
