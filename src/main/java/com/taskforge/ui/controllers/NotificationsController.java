package com.taskforge.ui.controllers;

import com.taskforge.dao.NotificationDAO;
import com.taskforge.dao.TeamDAO; // Added for TeamDAO dependency
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO; // Added for UserTeamDAO dependency
import com.taskforge.model.Notification;
import com.taskforge.model.Team; // Added for Team model
import com.taskforge.model.User;
import com.taskforge.model.UserTeamMembership; // Added for UserTeamMembership model
import com.taskforge.service.UserManagerService; // Added for UserManagerService dependency
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority; // For HBox.setHgrow
import javafx.scene.text.Font;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the NotificationsView.fxml.
 * This class manages the display and interaction with notifications for the current user.
 * It allows users to view their notifications, mark them as read, and delete them.
 * It also provides functionality to accept/reject team invitations directly from notifications.
 */
public class NotificationsController {

    @FXML
    private ListView<Notification> notificationsListView;
    @FXML
    private Label messageLabel;

    private User currentUser; // The currently logged-in user
    private NotificationDAO notificationDAO;
    private UserManagerService userManagerService; // New: To handle team invitation actions
    private ObservableList<Notification> notificationList = FXCollections.observableArrayList();

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It sets up the list view and
     * initializes service dependencies.
     */
    @FXML
    public void initialize() {
        // Initialize DAOs
        UserDAO userDAO = new UserDAO();
        TeamDAO teamDAO = new TeamDAO(); // Needed for UserManagerService
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO); // Needed for UserManagerService

        // Initialize Services
        this.notificationDAO = new NotificationDAO(userDAO);
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO); // Initialize UserManagerService

        // Configure notificationsListView to display notification details and actions
        notificationsListView.setCellFactory(lv -> new ListCell<Notification>() {
            private final HBox hbox = new HBox(10); // Spacing between elements
            private final VBox textContent = new VBox(5); // For message and date
            private final Label messageLabel = new Label();
            private final Label dateLabel = new Label();
            private final HBox actionButtons = new HBox(5); // For action buttons

            private final Button acceptButton = new Button("Accept");
            private final Button rejectButton = new Button("Reject");
            private final Button markAsReadButton = new Button("Mark as Read");
            private final Button deleteButton = new Button("Delete");

            {
                // Set up text content
                messageLabel.setWrapText(true);
                messageLabel.setFont(new Font("System", 14));
                dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
                textContent.getChildren().addAll(messageLabel, dateLabel);
                HBox.setHgrow(textContent, Priority.ALWAYS); // Allow text content to grow

                // Set up action buttons
                actionButtons.getChildren().addAll(acceptButton, rejectButton, markAsReadButton, deleteButton);

                // Add text content and action buttons to the main HBox
                hbox.getChildren().addAll(textContent, actionButtons);

                // Set button actions
                acceptButton.setOnAction(event -> {
                    Notification notification = getItem();
                    if (notification != null) {
                        handleAcceptInvitation(notification);
                    }
                });

                rejectButton.setOnAction(event -> {
                    Notification notification = getItem();
                    if (notification != null) {
                        handleRejectInvitation(notification);
                    }
                });

                markAsReadButton.setOnAction(event -> {
                    Notification notification = getItem();
                    if (notification != null && !notification.isRead()) {
                        handleMarkAsRead(notification);
                    }
                });

                deleteButton.setOnAction(event -> {
                    Notification notification = getItem();
                    if (notification != null) {
                        handleDeleteNotification(notification);
                    }
                });
            }

            @Override
            protected void updateItem(Notification notification, boolean empty) {
                super.updateItem(notification, empty);
                if (empty || notification == null) {
                    setGraphic(null);
                } else {
                    messageLabel.setText(notification.getMessage());
                    dateLabel.setText(notification.getSentAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

                    // Reset button visibility and state
                    acceptButton.setVisible(false);
                    rejectButton.setVisible(false);
                    markAsReadButton.setVisible(true);
                    deleteButton.setVisible(true);

                    // Configure based on notification type and read status
                    if (notification.getNotificationType() == Notification.NotificationType.TEAM_INVITATION) {
                        // For team invitations, check the actual membership status
                        Optional<UserTeamMembership> membershipOptional = userManagerService.getMembership(notification.getRecipient().getId(), notification.getRelatedEntityId());
                        if (membershipOptional.isPresent() && membershipOptional.get().getInvitationStatus() == UserTeamMembership.InvitationStatus.PENDING) {
                            acceptButton.setVisible(true);
                            rejectButton.setVisible(true);
                            markAsReadButton.setVisible(false); // Hide mark as read for actionable invites
                        } else {
                            // If invitation is already accepted/rejected or not found, hide buttons
                            acceptButton.setVisible(false);
                            rejectButton.setVisible(false);
                            markAsReadButton.setVisible(true); // Show mark as read if it's just an old notification
                        }
                    }

                    if (notification.isRead()) {
                        messageLabel.setStyle("-fx-font-weight: normal; -fx-text-fill: gray;");
                        markAsReadButton.setDisable(true);
                        markAsReadButton.setText("Read");
                    } else {
                        messageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
                        markAsReadButton.setDisable(false);
                        markAsReadButton.setText("Mark as Read");
                    }
                    setGraphic(hbox);
                }
            }
        });

        notificationsListView.setItems(notificationList);
    }

    /**
     * Sets the currently logged-in user. This method should be called by the DashboardController
     * after this view is loaded.
     *
     * @param user The User object representing the logged-in user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        // The loading of notifications will now be explicitly called by DashboardController
    }

    /**
     * Loads and displays notifications for the current user.
     * This method should be called after the currentUser has been set.
     */
    public void loadNotificationsForCurrentUser() {
        if (currentUser == null) {
            displayMessage("Cannot load notifications: No user is logged in.", true);
            notificationList.clear();
            return;
        }
        System.out.println("Loading notifications for user: " + currentUser.getUsername());
        notificationList.clear(); // Clear existing items
        // Fetch all notifications for the current user
        List<Notification> notifications = notificationDAO.getNotificationsByUserId(currentUser.getId());
        notificationList.addAll(notifications); // Add refreshed notifications

        if (notifications.isEmpty()) {
            displayMessage("No notifications found.", false);
        } else {
            displayMessage("Notifications loaded successfully.", false);
        }
        notificationsListView.refresh(); // Ensure UI is updated based on new data
    }

    /**
     * Handles the action when the "Refresh Notifications" button is clicked.
     * This method reloads notifications for the current user from the database.
     */
    @FXML
    private void handleRefreshNotifications() {
        loadNotificationsForCurrentUser(); // Delegate to the dedicated loading method
    }

    /**
     * Handles marking a notification as read.
     *
     * @param notification The Notification object to mark as read.
     */
    private void handleMarkAsRead(Notification notification) {
        if (notificationDAO.markNotificationAsRead(notification.getId())) {
            notification.setRead(true); // Update model object
            notificationsListView.refresh(); // Refresh the list view to update UI
            displayMessage("Notification marked as read.", false);
        } else {
            displayMessage("Failed to mark notification as read.", true);
        }
    }

    /**
     * Handles deleting a notification.
     *
     * @param notification The Notification object to delete.
     */
    private void handleDeleteNotification(Notification notification) {
        // In a real application, you'd want a confirmation dialog here.
        if (notificationDAO.deleteNotification(notification.getId())) {
            notificationList.remove(notification); // Remove from ObservableList
            displayMessage("Notification deleted.", false);
        } else {
            displayMessage("Failed to delete notification.", true);
        }
    }

    /**
     * Handles accepting a team invitation from a notification.
     *
     * @param notification The TEAM_INVITATION notification to accept.
     */
    private void handleAcceptInvitation(Notification notification) {
        if (currentUser == null) {
            displayMessage("Error: No user logged in.", true);
            return;
        }
        if (notification.getNotificationType() != Notification.NotificationType.TEAM_INVITATION) {
            displayMessage("This is not a team invitation.", true);
            return;
        }

        int teamId = notification.getRelatedEntityId();
        Optional<Team> teamOptional = userManagerService.getTeamById(teamId);

        if (teamOptional.isEmpty()) {
            displayMessage("Team not found for this invitation.", true);
            // Delete the notification if the team no longer exists
            handleDeleteNotification(notification);
            return;
        }

        boolean success = userManagerService.acceptTeamInvitation(currentUser.getId(), teamId);

        if (success) {
            displayMessage("Successfully accepted invitation to team '" + teamOptional.get().getName() + "'.", false);
            // Mark the notification as read and delete it (or just mark as read)
            notificationDAO.markNotificationAsRead(notification.getId());
            notificationList.remove(notification); // Remove from list, as it's now acted upon
            notificationsListView.refresh();
            // Optionally, send a notification to the team owner about acceptance
            // For now, just a console log
            System.out.println("User " + currentUser.getUsername() + " accepted invitation to team " + teamOptional.get().getName());
        } else {
            displayMessage("Failed to accept invitation. It might have been withdrawn or you are already a member.", true);
            // Keep the notification if acceptance failed, but mark as read if it's no longer actionable
            notificationDAO.markNotificationAsRead(notification.getId());
            notificationsListView.refresh();
        }
    }

    /**
     * Handles rejecting a team invitation from a notification.
     *
     * @param notification The TEAM_INVITATION notification to reject.
     */
    private void handleRejectInvitation(Notification notification) {
        if (currentUser == null) {
            displayMessage("Error: No user logged in.", true);
            return;
        }
        if (notification.getNotificationType() != Notification.NotificationType.TEAM_INVITATION) {
            displayMessage("This is not a team invitation.", true);
            return;
        }

        int teamId = notification.getRelatedEntityId();
        Optional<Team> teamOptional = userManagerService.getTeamById(teamId);

        if (teamOptional.isEmpty()) {
            displayMessage("Team not found for this invitation.", true);
            // Delete the notification if the team no longer exists
            handleDeleteNotification(notification);
            return;
        }

        boolean success = userManagerService.rejectTeamInvitation(currentUser.getId(), teamId);

        if (success) {
            displayMessage("Successfully rejected invitation to team '" + teamOptional.get().getName() + "'.", false);
            // Mark the notification as read and delete it
            notificationDAO.markNotificationAsRead(notification.getId());
            notificationList.remove(notification); // Remove from list
            notificationsListView.refresh();
            System.out.println("User " + currentUser.getUsername() + " rejected invitation to team " + teamOptional.get().getName());
        } else {
            displayMessage("Failed to reject invitation. It might have been withdrawn or you are no longer invited.", true);
            // Keep the notification if rejection failed, but mark as read if it's no longer actionable
            notificationDAO.markNotificationAsRead(notification.getId());
            notificationsListView.refresh();
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
