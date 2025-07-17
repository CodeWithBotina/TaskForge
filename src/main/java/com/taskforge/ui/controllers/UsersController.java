package com.taskforge.ui.controllers;

import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
import com.taskforge.model.User;
import com.taskforge.service.UserManagerService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Controller for the UsersView.fxml.
 * This class manages the display of all users in the system.
 * It populates a TableView with user data and provides functionality
 * to view user information. It restricts editing to the current user's profile
 * and prevents deletion of any user account.
 */
public class UsersController {

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, Integer> userIdColumn;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> emailColumn;
    @FXML
    private TableColumn<User, Void> actionsColumn; // For the "Edit" button for the current user

    @FXML
    private Label messageLabel;

    private User currentUser; // The currently logged-in user
    private UserManagerService userManagerService;
    private ObservableList<User> userList = FXCollections.observableArrayList();

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
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO);

        // Initialize Service
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);

        // Configure table columns
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Custom cell factory for Actions column (Edit button for current user only)
        actionsColumn.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button editButton = new Button("Edit My Profile");
            private final HBox pane = new HBox(5, editButton);

            {
                editButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    if (currentUser != null && user.getId() == currentUser.getId()) {
                        handleEditUser(user);
                    } else {
                        // This should ideally not be reachable if button is only visible for current user
                        displayMessage("You can only edit your own profile.", true);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    // Only show the edit button for the currently logged-in user's row
                    if (currentUser != null && user.getId() == currentUser.getId()) {
                        setGraphic(pane);
                    } else {
                        setGraphic(null); // Hide button for other users
                    }
                }
            }
        });

        usersTable.setItems(userList);

        // Users will be refreshed when setCurrentUser is called by DashboardController
    }

    /**
     * Sets the currently logged-in user. This method should be called by the DashboardController
     * after this view is loaded. Once the user is set, it triggers a refresh of users.
     *
     * @param user The User object representing the logged-in user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            handleRefreshUsers(); // Automatically load users
        } else {
            displayMessage("No user logged in. Cannot display users.", true);
            userList.clear(); // Clear any existing users
        }
    }

    /**
     * Handles the action when the "Refresh Users" button is clicked.
     * This method reloads all users from the database.
     */
    @FXML
    private void handleRefreshUsers() {
        System.out.println("Refreshing all users...");
        userList.clear(); // Clear existing items
        List<User> users = userManagerService.getAllUsers();
        userList.addAll(users); // Add refreshed users

        if (users.isEmpty()) {
            displayMessage("No users found in the system.", false);
        } else {
            displayMessage("Users refreshed successfully.", false);
        }
        // Force update of the actions column to reflect current user status
        usersTable.refresh();
    }

    /**
     * Handles the action for the "Edit" button for the current user's row.
     * This method opens a new modal dialog to edit the current user's profile.
     *
     * @param user The User object to be edited (should always be the currentUser).
     */
    private void handleEditUser(User user) {
        if (currentUser == null || user.getId() != currentUser.getId()) {
            displayMessage("You can only edit your own profile.", true);
            return;
        }

        try {
            // Load the FXML for the edit user view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/EditUserView.fxml"));
            Parent editUserRoot = loader.load();

            // Get the controller and pass the user to be edited
            EditUserController editUserController = loader.getController();
            editUserController.setUser(user);

            // Create a new stage for the dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit My Profile: " + user.getUsername());
            dialogStage.initModality(Modality.WINDOW_MODAL); // Make it a modal dialog
            dialogStage.initOwner(usersTable.getScene().getWindow()); // Set the owner window
            Scene scene = new Scene(editUserRoot);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false); // Make the dialog not resizable
            dialogStage.showAndWait(); // Show the dialog and wait for it to be closed

            // After the dialog is closed, refresh the users table in case changes were made
            handleRefreshUsers();

        } catch (IOException e) {
            System.err.println("Error loading EditUserView.fxml: " + e.getMessage());
            e.printStackTrace();
            displayMessage("Error opening profile edit dialog.", true);
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
