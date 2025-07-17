package com.taskforge.ui.controllers;

import com.taskforge.dao.NotificationDAO;
import com.taskforge.dao.ProjectDAO;
import com.taskforge.dao.TaskDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO; // Import UserTeamDAO
import com.taskforge.dao.TeamDAO; // Import TeamDAO
import com.taskforge.model.Priority;
import com.taskforge.model.Project;
import com.taskforge.model.Status;
import com.taskforge.model.Task;
import com.taskforge.model.User;
import com.taskforge.model.Visibility; // Import Visibility enum
import com.taskforge.service.ProjectManagerService;
import com.taskforge.service.TaskManagerService;
import com.taskforge.service.UserManagerService; // Import UserManagerService
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the MyTasksView.fxml.
 * This class manages the display and interaction with tasks assigned to the current user.
 * It provides functionalities for creating new tasks, editing existing ones,
 * and filtering tasks. It also handles task visibility and assignment rules.
 */
public class MyTasksController {

    @FXML
    private TextField newTaskTitleField;
    @FXML
    private TextArea newTaskDescriptionArea;
    @FXML
    private DatePicker newTaskDueDatePicker;
    @FXML
    private ChoiceBox<Priority> newTaskPriorityChoiceBox;
    @FXML
    private ChoiceBox<Visibility> newTaskVisibilityChoiceBox; // New ChoiceBox for Visibility
    @FXML
    private Label newTaskMessageLabel;

    @FXML
    private TableView<Task> myTasksTable;
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
    private TableColumn<Task, String> projectColumn; // Will display project name
    @FXML
    private TableColumn<Task, Visibility> visibilityColumn; // New column for Visibility
    @FXML
    private TableColumn<Task, Void> actionsColumn; // For buttons like Edit/Delete

    @FXML
    private Label messageLabel;

    private User currentUser; // To store the currently logged-in user
    private TaskManagerService taskManagerService;
    private UserManagerService userManagerService; // New: Needed for user/team context
    private ProjectManagerService projectManagerService; // New: Needed for project context
    private ObservableList<Task> taskList = FXCollections.observableArrayList();

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It sets up the table columns and
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
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO); // Initialize UserTeamDAO

        // Initialize Services
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO); // Initialize UserManagerService
        this.taskManagerService = new TaskManagerService(taskDAO, userDAO, projectDAO, notificationDAO, userTeamDAO, userManagerService); // Pass userTeamDAO and userManagerService
        this.projectManagerService = new ProjectManagerService(projectDAO, teamDAO); // Initialize ProjectManagerService

        // Populate Priority ChoiceBox
        newTaskPriorityChoiceBox.setItems(FXCollections.observableArrayList(Priority.values()));
        newTaskPriorityChoiceBox.getSelectionModel().select(Priority.MEDIUM); // Default selection

        // Populate Visibility ChoiceBox
        newTaskVisibilityChoiceBox.setItems(FXCollections.observableArrayList(Visibility.values()));
        newTaskVisibilityChoiceBox.getSelectionModel().select(Visibility.PRIVATE); // Default selection

        // Configure table columns
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        visibilityColumn.setCellValueFactory(new PropertyValueFactory<>("visibility")); // Set cell value factory for Visibility

        // Custom cell factory for Due Date to format LocalDateTime
        dueDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dueDate = cellData.getValue().getDueDate();
            return new javafx.beans.property.SimpleStringProperty(
                    dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A"
            );
        });

        // Custom cell factory for Project to display project name
        projectColumn.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    task.getProject() != null ? task.getProject().getName() : "None"
            );
        });

        // Custom cell factory for Actions column (Edit, Delete buttons)
        actionsColumn.setCellFactory(param -> new TableCell<Task, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                // Set button actions
                editButton.setOnAction(event -> {
                    Task task = getTableView().getItems().get(getIndex());
                    handleEditTask(task);
                });
                deleteButton.setOnAction(event -> {
                    Task task = getTableView().getItems().get(getIndex());
                    handleDeleteTask(task);
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

        myTasksTable.setItems(taskList);

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
        // Removed direct call to handleRefreshTasks() from here.
        // It will now be explicitly called by DashboardController after setCurrentUser.
    }

    /**
     * Loads and displays tasks assigned to the current user.
     * This method should be called after the currentUser has been set.
     */
    public void loadTasksForCurrentUser() {
        if (currentUser == null) {
            displayMessage("Cannot load tasks: No user is logged in.", true);
            taskList.clear();
            return;
        }
        System.out.println("Loading tasks for user: " + currentUser.getUsername());
        taskList.clear(); // Clear existing items
        // Fetch tasks assigned to the current user that are visible to them
        List<Task> tasks = taskManagerService.getTasksByAssignedUser(currentUser.getId(), currentUser.getId());
        taskList.addAll(tasks); // Add refreshed tasks

        if (tasks.isEmpty()) {
            displayMessage("No tasks assigned to you.", false);
        } else {
            displayMessage("Tasks loaded successfully.", false);
        }
    }

    /**
     * Handles the action when the "Add Task" button is clicked.
     * Collects input from the form fields and attempts to create a new task.
     */
    @FXML
    private void handleAddTask() {
        String title = newTaskTitleField.getText();
        String description = newTaskDescriptionArea.getText();
        LocalDateTime dueDate = null;
        if (newTaskDueDatePicker.getValue() != null) {
            dueDate = newTaskDueDatePicker.getValue().atTime(LocalTime.MAX); // Set to end of day
        }
        Priority priority = newTaskPriorityChoiceBox.getValue();
        Visibility visibility = newTaskVisibilityChoiceBox.getValue(); // Get selected Visibility

        // Basic validation
        if (title.isEmpty()) {
            displayNewTaskMessage("Task title cannot be empty.", true);
            return;
        }
        if (priority == null) {
            displayNewTaskMessage("Please select a priority.", true);
            return;
        }
        if (visibility == null) {
            displayNewTaskMessage("Please select a visibility level.", true);
            return;
        }
        if (currentUser == null) {
            displayNewTaskMessage("Cannot create task: No user logged in.", true);
            return;
        }

        // Project ID and Assigned To User ID are not directly set in this view,
        // so they will be 0 (unassigned/no project) initially.
        // The service layer will handle the default values and rules.
        int assignedToUserId = currentUser.getId(); // Assign to self by default on creation
        int projectId = 0; // No project assigned on creation from this view

        // Ensure description is null if empty or only whitespace
        String finalDescription = (description != null && !description.trim().isEmpty()) ? description.trim() : null;

        Optional<Task> createdTask = taskManagerService.createTask(
                title,
                finalDescription,
                dueDate,
                priority,
                assignedToUserId, // Assigned to creator by default
                projectId,
                visibility,
                currentUser.getId() // Set the creator of the task
        );

        if (createdTask.isPresent()) {
            displayNewTaskMessage("Task '" + createdTask.get().getTitle() + "' added successfully!", false);
            handleClearForm(); // Clear the form after successful addition
            loadTasksForCurrentUser(); // Refresh the table to show the new task
        } else {
            displayNewTaskMessage("Failed to add task. Check console for details.", true);
        }
    }

    /**
     * Handles the action when the "Clear Form" button is clicked.
     * Clears all input fields in the new task creation form.
     */
    @FXML
    private void handleClearForm() {
        newTaskTitleField.clear();
        newTaskDescriptionArea.clear();
        newTaskDueDatePicker.setValue(null);
        newTaskPriorityChoiceBox.getSelectionModel().select(Priority.MEDIUM); // Reset to default
        newTaskVisibilityChoiceBox.getSelectionModel().select(Visibility.PRIVATE); // Reset to default
        newTaskMessageLabel.setText(""); // Clear any messages
    }

    /**
     * Handles the action when the "Refresh Tasks" button is clicked.
     * This method reloads tasks assigned to the current user from the database.
     */
    @FXML
    private void handleRefreshTasks() {
        loadTasksForCurrentUser(); // Delegate to the dedicated loading method
    }

    /**
     * Handles the action for the "Edit" button in a task row.
     * This method opens a new modal dialog to edit the selected task.
     *
     * @param task The Task object to be edited.
     */
    private void handleEditTask(Task task) {
        try {
            // Load the FXML for the edit task view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/EditTaskView.fxml"));
            Parent editTaskRoot = loader.load();

            // Get the controller and pass the task to be edited and the current user
            EditTaskController editTaskController = loader.getController();
            editTaskController.setTask(task);
            editTaskController.setCurrentUser(currentUser); // Pass current user for permission checks

            // Create a new stage for the dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Task: " + task.getTitle());
            dialogStage.initModality(Modality.WINDOW_MODAL); // Make it a modal dialog
            dialogStage.initOwner(myTasksTable.getScene().getWindow()); // Set the owner window
            Scene scene = new Scene(editTaskRoot);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false); // Make the dialog not resizable
            dialogStage.showAndWait(); // Show the dialog and wait for it to be closed

            // After the dialog is closed, refresh the tasks table in case changes were made
            loadTasksForCurrentUser(); // Refresh the table after changes

        } catch (IOException e) {
            System.err.println("Error loading EditTaskView.fxml: " + e.getMessage());
            e.printStackTrace();
            displayMessage("Error opening task edit dialog.", true);
        }
    }

    /**
     * Handles the action for the "Delete" button in a task row.
     * This method deletes the selected task from the database.
     *
     * @param task The Task object to be deleted.
     */
    private void handleDeleteTask(Task task) {
        System.out.println("Delete Task button clicked for task: " + task.getTitle());
        // In a real application, you'd want a confirmation dialog here.
        if (currentUser == null) {
            displayMessage("Cannot delete task: No user logged in.", true);
            return;
        }
        boolean success = taskManagerService.deleteTask(task.getId(), currentUser.getId());
        if (success) {
            displayMessage("Task '" + task.getTitle() + "' deleted successfully.", false);
            loadTasksForCurrentUser(); // Refresh the table after deletion
        } else {
            displayMessage("Failed to delete task '" + task.getTitle() + "'. You must be the creator to delete a task.", true);
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
     * Displays a message specifically for the new task creation section.
     *
     * @param message The message text to display.
     * @param isError True if the message is an error (will be red), false for success/info (will be black).
     */
    private void displayNewTaskMessage(String message, boolean isError) {
        newTaskMessageLabel.setText(message);
        if (isError) {
            newTaskMessageLabel.setStyle("-fx-text-fill: red;");
        } else {
            newTaskMessageLabel.setStyle("-fx-text-fill: black;");
        }
    }
}
