package com.taskforge.ui.controllers;

import com.taskforge.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the DashboardView.fxml.
 * This class serves as the main navigation hub for the application after a user logs in.
 * It manages the display of different functional views (My Tasks, All Tasks, Projects, Teams, Users, Notifications)
 * within the main application window and ensures the logged-in user context is passed to sub-controllers.
 */
public class DashboardController {

    @FXML
    private BorderPane rootLayout; // The root layout of the dashboard view
    @FXML
    private Label loggedInUserLabel; // Label to display the logged-in user's name

    private User loggedInUser; // Stores the currently logged-in user

    /**
     * Sets the currently logged-in user for the dashboard.
     * This method is called by the LoginController after successful authentication.
     * It updates the welcome message and prepares the dashboard for the specific user.
     *
     * @param user The User object representing the logged-in user.
     */
    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        if (loggedInUser != null) {
            loggedInUserLabel.setText("Welcome, " + loggedInUser.getUsername() + "!");
            // Optionally, load a default view (e.g., My Tasks) when the dashboard opens
            handleMyTasks();
        }
    }

    /**
     * Handles the action when the "Logout" button is clicked.
     * Navigates the user back to the LoginView.
     */
    @FXML
    private void handleLogout() {
        try {
            // Clear the logged-in user
            this.loggedInUser = null;

            // Load the LoginView FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/LoginView.fxml"));
            Parent loginRoot = loader.load();

            // Get the current stage from the root layout
            Stage stage = (Stage) rootLayout.getScene().getWindow();
            Scene scene = new Scene(loginRoot, 800, 600);
            stage.setScene(scene);
            stage.setTitle("TaskForge - Login / Register");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading LoginView.fxml during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles navigation to the "My Tasks" view.
     * Loads the MyTasksView.fxml and sets it as the center content of the dashboard.
     * Passes the logged-in user to the MyTasksController.
     */
    @FXML
    private void handleMyTasks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/MyTasksView.fxml"));
            Parent myTasksRoot = loader.load();

            MyTasksController myTasksController = loader.getController();
            myTasksController.setCurrentUser(loggedInUser); // Pass the logged-in user
            // Explicitly load tasks after setting the user
            myTasksController.loadTasksForCurrentUser();

            rootLayout.setCenter(myTasksRoot);
        } catch (IOException e) {
            System.err.println("Error loading MyTasksView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles navigation to the "All Tasks" view.
     * Loads the AllTasksView.fxml and sets it as the center content of the dashboard.
     * Passes the logged-in user to the AllTasksController.
     */
    @FXML
    private void handleAllTasks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/AllTasksView.fxml"));
            Parent allTasksRoot = loader.load();

            AllTasksController allTasksController = loader.getController();
            allTasksController.setCurrentUser(loggedInUser); // Pass the logged-in user
            // Explicitly load tasks after setting the user
            allTasksController.loadTasksForCurrentUser();

            rootLayout.setCenter(allTasksRoot);
        } catch (IOException e) {
            System.err.println("Error loading AllTasksView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles navigation to the "Projects" view.
     * Loads the ProjectsView.fxml and sets it as the center content of the dashboard.
     */
    @FXML
    private void handleProjects() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/ProjectsView.fxml"));
            Parent projectsRoot = loader.load();

            // ProjectsController does not currently need the currentUser, but can be extended
            // ProjectsController projectsController = loader.getController();
            // projectsController.setCurrentUser(loggedInUser); // If needed

            rootLayout.setCenter(projectsRoot);
        } catch (IOException e) {
            System.err.println("Error loading ProjectsView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles navigation to the "Teams" view.
     * Loads the TeamsView.fxml and sets it as the center content of the dashboard.
     * Passes the logged-in user to the TeamsController.
     */
    @FXML
    private void handleTeams() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/TeamsView.fxml"));
            Parent teamsRoot = loader.load();

            TeamsController teamsController = loader.getController();
            teamsController.setCurrentUser(loggedInUser); // Pass the logged-in user

            rootLayout.setCenter(teamsRoot);
        } catch (IOException e) {
            System.err.println("Error loading TeamsView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles navigation to the "Users" view.
     * Loads the UsersView.fxml and sets it as the center content of the dashboard.
     * Passes the logged-in user to the UsersController.
     */
    @FXML
    private void handleUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/UsersView.fxml"));
            Parent usersRoot = loader.load();

            UsersController usersController = loader.getController();
            usersController.setCurrentUser(loggedInUser); // Pass the logged-in user

            rootLayout.setCenter(usersRoot);
        } catch (IOException e) {
            System.err.println("Error loading UsersView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles navigation to the "Notifications" view.
     * Loads the NotificationsView.fxml and sets it as the center content of the dashboard.
     * Passes the logged-in user to the NotificationsController.
     */
    @FXML
    private void handleNotifications() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/NotificationsView.fxml"));
            Parent notificationsRoot = loader.load();

            NotificationsController notificationsController = loader.getController();
            notificationsController.setCurrentUser(loggedInUser); // Pass the logged-in user
            notificationsController.loadNotificationsForCurrentUser(); // Explicitly load notifications

            rootLayout.setCenter(notificationsRoot);
        } catch (IOException e) {
            System.err.println("Error loading NotificationsView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
