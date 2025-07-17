package com.taskforge.ui.controllers;

import com.taskforge.dao.NotificationDAO;
import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
import com.taskforge.model.Notification;
import com.taskforge.model.Team;
import com.taskforge.model.User;
import com.taskforge.model.UserTeamMembership;
import com.taskforge.service.UserManagerService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controller for the NotificationDetailView.fxml.
 * This class displays the details of a single notification and provides
 * actions based on the notification type, such as accepting/rejecting team invitations.
 */
public class NotificationDetailController {

    @FXML
    private Label notificationMessageLabel;
    @FXML
    private Label notificationDateLabel;
    @FXML
    private Label notificationTypeLabel;
    @FXML
    private HBox actionButtonsContainer; // Container for dynamic action buttons
    @FXML
    private Label messageLabel; // For displaying general messages/errors

    private Notification currentNotification;
    private User currentUser; // The user viewing the notification
    private NotificationDAO notificationDAO;
    private UserManagerService userManagerService;

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It sets up service dependencies.
     */
    @FXML
    public void initialize() {
        // Initialize DAOs
        UserDAO userDAO = new UserDAO();
        TeamDAO teamDAO = new TeamDAO();
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO);

        // Initialize Services
        this.notificationDAO = new NotificationDAO(userDAO);
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);
    }

    /**
     * Sets the notification to be displayed and the current user.
     * This method should be called by the parent controller (e.g., NotificationsController)
     * before showing this view.
     *
     * @param notification The Notification object to display.
     * @param currentUser The User object currently logged in.
     */
    public void setNotification(Notification notification, User currentUser) {
        this.currentNotification = notification;
        this.currentUser = currentUser;
        populateNotificationDetails();
        setupActionButtons();
    }

    /**
     * Populates the UI elements with the details of the current notification.
     */
    private void populateNotificationDetails() {
        if (currentNotification != null) {
            notificationMessageLabel.setText(currentNotification.getMessage());
            notificationDateLabel.setText(currentNotification.getSentAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            notificationTypeLabel.setText("Type: " + currentNotification.getNotificationType().toString().replace("_", " "));

            if (currentNotification.isRead()) {
                notificationMessageLabel.setStyle("-fx-font-weight: normal; -fx-text-fill: gray;");
            } else {
                notificationMessageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
            }
        }
    }

    /**
     * Sets up action buttons dynamically based on the notification type.
     */
    private void setupActionButtons() {
        actionButtonsContainer.getChildren().clear(); // Clear existing buttons

        if (currentNotification == null || currentUser == null) {
            return;
        }

        // Always add a "Mark as Read" and "Delete" button
        Button markAsReadButton = new Button("Mark as Read");
        markAsReadButton.setOnAction(event -> handleMarkAsRead());
        markAsReadButton.setDisable(currentNotification.isRead()); // Disable if already read

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> handleDeleteNotification());

        actionButtonsContainer.getChildren().addAll(markAsReadButton, deleteButton);

        // Add specific buttons for TEAM_INVITATION
        if (currentNotification.getNotificationType() == Notification.NotificationType.TEAM_INVITATION) {
            int teamId = currentNotification.getRelatedEntityId();
            Optional<UserTeamMembership> membershipOptional = userManagerService.getMembership(currentUser.getId(), teamId); // Corrected method call

            if (membershipOptional.isPresent() && membershipOptional.get().getInvitationStatus() == UserTeamMembership.InvitationStatus.PENDING) {
                Button acceptButton = new Button("Accept Invitation");
                acceptButton.setOnAction(event -> handleAcceptInvitation());

                Button rejectButton = new Button("Reject Invitation");
                rejectButton.setOnAction(event -> handleRejectInvitation());

                actionButtonsContainer.getChildren().addAll(acceptButton, rejectButton);
                markAsReadButton.setVisible(false); // Hide mark as read for actionable invites
            }
        }
    }

    /**
     * Handles marking the current notification as read.
     */
    @FXML
    private void handleMarkAsRead() {
        if (currentNotification != null) {
            if (notificationDAO.markNotificationAsRead(currentNotification.getId())) {
                currentNotification.setRead(true); // Update model
                populateNotificationDetails(); // Refresh UI
                setupActionButtons(); // Update button states
                displayMessage("Notification marked as read.", false);
            } else {
                displayMessage("Failed to mark notification as read.", true);
            }
        }
    }

    /**
     * Handles deleting the current notification.
     */
    @FXML
    private void handleDeleteNotification() {
        if (currentNotification != null) {
            if (notificationDAO.deleteNotification(currentNotification.getId())) {
                displayMessage("Notification deleted.", false);
                // Close the detail view after deletion
                Stage stage = (Stage) notificationMessageLabel.getScene().getWindow();
                stage.close();
            } else {
                displayMessage("Failed to delete notification.", true);
            }
        }
    }

    /**
     * Handles accepting a team invitation.
     */
    @FXML
    private void handleAcceptInvitation() {
        if (currentNotification == null || currentUser == null) {
            displayMessage("Error: Invalid notification or user context.", true);
            return;
        }
        if (currentNotification.getNotificationType() != Notification.NotificationType.TEAM_INVITATION) {
            displayMessage("This is not a team invitation.", true);
            return;
        }

        int teamId = currentNotification.getRelatedEntityId();
        Optional<Team> teamOptional = userManagerService.getTeamById(teamId);

        if (teamOptional.isEmpty()) {
            displayMessage("Team not found for this invitation.", true);
            handleDeleteNotification(); // Delete notification if team is gone
            return;
        }

        boolean success = userManagerService.acceptTeamInvitation(currentUser.getId(), teamId);

        if (success) {
            displayMessage("Successfully accepted invitation to team '" + teamOptional.get().getName() + "'.", false);
            // Mark the notification as read and close the detail view
            notificationDAO.markNotificationAsRead(currentNotification.getId());
            Stage stage = (Stage) notificationMessageLabel.getScene().getWindow();
            stage.close();
        } else {
            displayMessage("Failed to accept invitation. It might have been withdrawn or you are already a member.", true);
        }
    }

    /**
     * Handles rejecting a team invitation.
     */
    @FXML
    private void handleRejectInvitation() {
        if (currentNotification == null || currentUser == null) {
            displayMessage("Error: Invalid notification or user context.", true);
            return;
        }
        if (currentNotification.getNotificationType() != Notification.NotificationType.TEAM_INVITATION) {
            displayMessage("This is not a team invitation.", true);
            return;
        }

        int teamId = currentNotification.getRelatedEntityId();
        Optional<Team> teamOptional = userManagerService.getTeamById(teamId);

        if (teamOptional.isEmpty()) {
            displayMessage("Team not found for this invitation.", true);
            handleDeleteNotification(); // Delete notification if team is gone
            return;
        }

        boolean success = userManagerService.rejectTeamInvitation(currentUser.getId(), teamId);

        if (success) {
            displayMessage("Successfully rejected invitation to team '" + teamOptional.get().getName() + "'.", false);
            // Mark the notification as read and close the detail view
            notificationDAO.markNotificationAsRead(currentNotification.getId());
            Stage stage = (Stage) notificationMessageLabel.getScene().getWindow();
            stage.close();
        } else {
            displayMessage("Failed to reject invitation. It might have been withdrawn or you are no longer invited.", true);
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
