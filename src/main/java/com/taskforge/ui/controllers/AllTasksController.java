package com.taskforge.ui.controllers;

import com.taskforge.dao.NotificationDAO;
import com.taskforge.dao.ProjectDAO;
import com.taskforge.dao.TaskDAO;
import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO; // Import UserTeamDAO
import com.taskforge.model.Task;
import com.taskforge.model.Priority;
import com.taskforge.model.Status;
import com.taskforge.model.User; // Import User model
import com.taskforge.model.Visibility; // Import Visibility enum
import com.taskforge.service.TaskManagerService;
import com.taskforge.service.UserManagerService; // Import UserManagerService
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the AllTasksView.fxml.
 * This class manages the display and interaction with all tasks in the system
 * that are visible to the currently logged-in user.
 * It populates a TableView with task data and handles refreshing the list.
 */
public class AllTasksController {

    @FXML
    private TableView<Task> allTasksTable;
    @FXML
    private TableColumn<Task, String> titleColumn;
    @FXML
    private TableColumn<Task, String> descriptionColumn;
    @FXML
    private TableColumn<Task, String> dueDateColumn; // Will display formatted LocalDateTime
    @FXML
    private TableColumn<Task, Priority> priorityColumn;
    @FXML
    private TableColumn<Task, Status> statusColumn;
    @FXML
    private TableColumn<Task, String> assignedToColumn; // Will display assigned user's username
    @FXML
    private TableColumn<Task, String> projectColumn; // Will display project name
    @FXML
    private TableColumn<Task, Visibility> visibilityColumn; // New column for Visibility
    @FXML
    private TableColumn<Task, String> creatorColumn; // New column for Creator
    @FXML
    private TableColumn<Task, Void> actionsColumn; // For buttons like Edit/Delete
    @FXML
    private Label messageLabel;

    private User currentUser; // To store the currently logged-in user
    private TaskManagerService taskManagerService;
    private ObservableList<Task> taskList = FXCollections.observableArrayList();

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It sets up the table columns and
     * initializes service dependencies.
     */
    @FXML
    public void initialize() {
        System.out.println("AllTasksController: initialize() called. HashCode: " + this.hashCode());
        // Initialize DAOs and Services
        UserDAO userDAO = new UserDAO();
        TeamDAO teamDAO = new TeamDAO();
        ProjectDAO projectDAO = new ProjectDAO(teamDAO);
        TaskDAO taskDAO = new TaskDAO(userDAO, projectDAO);
        NotificationDAO notificationDAO = new NotificationDAO(userDAO);
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO);

        // Initialize UserManagerService first as TaskManagerService depends on it
        UserManagerService userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);
        // Corrected TaskManagerService instantiation to pass userManagerService
        this.taskManagerService = new TaskManagerService(taskDAO, userDAO, projectDAO, notificationDAO, userTeamDAO, userManagerService);

        // Configure table columns
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        // New columns
        visibilityColumn.setCellValueFactory(new PropertyValueFactory<>("visibility"));
        creatorColumn.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    task.getCreator() != null ? task.getCreator().getUsername() : "Unknown"
            );
        });

        // Custom cell factory for Due Date to format LocalDateTime
        dueDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dueDate = cellData.getValue().getDueDate();
            return new javafx.beans.property.SimpleStringProperty(
                    dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A"
            );
        });

        // Custom cell factory for Assigned To to display username
        assignedToColumn.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    task.getAssignedTo() != null ? task.getAssignedTo().getUsername() : "Unassigned"
            );
        });

        // Custom cell factory for Project to display project name
        projectColumn.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    task.getProject() != null ? task.getProject().getName() : "None"
            );
        });

        // TODO: Implement actionsColumn for Edit/Delete buttons (if applicable for All Tasks view)
        // actionsColumn.setCellFactory(param -> new TableCell<Task, Void>() { ... });

        allTasksTable.setItems(taskList);

        // Tasks will be refreshed when setCurrentUser is called
    }

    /**
     * Sets the currently logged-in user. This method should be called by the DashboardController
     * after this view is loaded.
     *
     * @param user The User object representing the logged-in user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("AllTasksController: setCurrentUser called. User: " + (user != null ? user.getUsername() : "null") + " (Instance: " + this.hashCode() + ")");
        // Removed direct call to handleRefreshAllTasks() from here.
        // It will now be explicitly called by DashboardController after setCurrentUser.
    }

    /**
     * Loads and displays all tasks visible to the current user.
     * This method should be called after the currentUser has been set.
     */
    public void loadTasksForCurrentUser() {
        System.out.println("AllTasksController: loadTasksForCurrentUser called. Current User: " + (currentUser != null ? currentUser.getUsername() : "null") + " (Instance: " + this.hashCode() + ")");
        if (currentUser == null) {
            displayMessage("Cannot load tasks: No user is logged in.", true);
            taskList.clear();
            return;
        }
        System.out.println("Loading all tasks visible to user: " + currentUser.getUsername() + "...");
        taskList.clear(); // Clear existing items
        List<Task> tasks = taskManagerService.getAllVisibleTasks(currentUser.getId());
        taskList.addAll(tasks);

        if (tasks.isEmpty()) {
            displayMessage("No tasks found or visible to you in the system.", false);
        } else {
            displayMessage("All visible tasks loaded successfully.", false);
        }
    }

    /**
     * Handles the action when the "Refresh All Tasks" button is clicked.
     * This method reloads all tasks from the database, filtered by visibility for the current user.
     */
    @FXML
    private void handleRefreshAllTasks() {
        System.out.println("AllTasksController: handleRefreshAllTasks called by button. Current User: " + (currentUser != null ? currentUser.getUsername() : "null") + " (Instance: " + this.hashCode() + ")");
        loadTasksForCurrentUser(); // Delegate to the dedicated loading method
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
