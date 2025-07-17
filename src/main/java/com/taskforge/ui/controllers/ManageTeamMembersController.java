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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the ManageTeamMembersView.fxml.
 * This class manages the user interface for adding, removing, and managing roles
 * of members within a specific team. It also handles sending notifications for invitations.
 */
public class ManageTeamMembersController {

    @FXML
    private Label teamNameLabel;
    @FXML
    private ChoiceBox<User> addMemberUserChoiceBox;
    @FXML
    private ChoiceBox<UserTeamMembership.Role> addMemberRoleChoiceBox;
    @FXML
    private Label addMemberMessageLabel;
    @FXML
    private ListView<UserTeamMembership> membersListView;
    @FXML
    private Label membersListMessageLabel;

    private Team currentTeam; // The team whose members are being managed
    private UserManagerService userManagerService;
    private NotificationDAO notificationDAO; // To send notifications

    private ObservableList<UserTeamMembership> currentMemberships = FXCollections.observableArrayList();

    /**
     * Initializes the controller. This method is automatically called by JavaFX
     * after the FXML file has been loaded. It sets up choice boxes and initializes services.
     */
    @FXML
    public void initialize() {
        // Initialize DAOs
        UserDAO userDAO = new UserDAO();
        TeamDAO teamDAO = new TeamDAO();
        UserTeamDAO userTeamDAO = new UserTeamDAO(userDAO, teamDAO);
        NotificationDAO notificationDAO = new NotificationDAO(userDAO); // Initialize NotificationDAO

        // Initialize Service
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);
        this.notificationDAO = notificationDAO; // Assign NotificationDAO

        // Populate Role ChoiceBox for adding new members
        addMemberRoleChoiceBox.setItems(FXCollections.observableArrayList(UserTeamMembership.Role.values()));
        addMemberRoleChoiceBox.getSelectionModel().select(UserTeamMembership.Role.MEMBER); // Default to MEMBER

        // Configure addMemberUserChoiceBox to display usernames
        addMemberUserChoiceBox.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user != null ? user.getUsername() : null;
            }

            @Override
            public User fromString(String string) {
                return null; // Not used for selection
            }
        });

        // Configure membersListView to display membership details
        membersListView.setCellFactory(lv -> new ListCell<UserTeamMembership>() {
            @Override
            protected void updateItem(UserTeamMembership membership, boolean empty) {
                super.updateItem(membership, empty);
                if (empty || membership == null) {
                    setText(null);
                } else {
                    String userName = membership.getUser() != null ? membership.getUser().getUsername() : "Unknown User";
                    String teamRole = membership.getRole().toString();
                    String inviteStatus = membership.getInvitationStatus().toString();
                    setText(userName + " (" + teamRole + ") - Status: " + inviteStatus);
                }
            }
        });

        membersListView.setItems(currentMemberships);
    }

    /**
     * Sets the team whose members are to be managed. This method should be called
     * by the parent controller (e.g., TeamsController) before showing this view.
     * It populates the team name label and refreshes the member list.
     *
     * @param team The Team object to manage.
     */
    public void setTeam(Team team) {
        this.currentTeam = team;
        if (currentTeam != null) {
            teamNameLabel.setText("Team: " + currentTeam.getName());
            populateAddMemberUserChoiceBox(); // Populate users available for invitation
            refreshMembersList(); // Load and display current members
        } else {
            teamNameLabel.setText("Team: N/A");
            addMemberUserChoiceBox.getItems().clear();
            currentMemberships.clear();
            displayMembersListMessage("No team selected.", true);
        }
    }

    /**
     * Populates the addMemberUserChoiceBox with users who are NOT already members of the current team.
     */
    private void populateAddMemberUserChoiceBox() {
        List<User> allUsers = userManagerService.getAllUsers();
        List<User> currentTeamUsers = userManagerService.getUsersInTeam(currentTeam.getId());

        // Filter out users who are already members of the current team
        List<User> availableUsers = allUsers.stream()
                .filter(user -> !currentTeamUsers.contains(user))
                .collect(Collectors.toList());

        addMemberUserChoiceBox.setItems(FXCollections.observableArrayList(availableUsers));
        addMemberUserChoiceBox.getSelectionModel().clearSelection(); // No user selected by default
    }

    /**
     * Refreshes the list of current team members in the ListView.
     */
    private void refreshMembersList() {
        if (currentTeam == null) {
            displayMembersListMessage("Cannot refresh members: No team selected.", true);
            currentMemberships.clear();
            return;
        }
        System.out.println("Refreshing members for team: " + currentTeam.getName());

        // Get all memberships for the current team
        List<UserTeamMembership> memberships = userManagerService.getTeamMemberships(currentTeam.getId());
        currentMemberships.setAll(memberships); // Update ObservableList

        if (memberships.isEmpty()) {
            displayMembersListMessage("No members found for this team.", false);
        } else {
            displayMembersListMessage("Members list refreshed.", false);
        }
    }

    /**
     * Handles the action when the "Invite Member" button is clicked.
     * Creates a new UserTeamMembership with PENDING status and sends a notification.
     */
    @FXML
    private void handleInviteMember() {
        User selectedUser = addMemberUserChoiceBox.getValue();
        UserTeamMembership.Role selectedRole = addMemberRoleChoiceBox.getValue();

        if (selectedUser == null) {
            displayAddMemberMessage("Please select a user to invite.", true);
            return;
        }
        if (selectedRole == null) {
            displayAddMemberMessage("Please select a role for the new member.", true);
            return;
        }
        if (currentTeam == null) {
            displayAddMemberMessage("No team selected to invite members to.", true);
            return;
        }

        // Attempt to add the user to the team with PENDING status
        boolean success = userManagerService.inviteUserToTeam(
                selectedUser.getId(),
                currentTeam.getId(),
                selectedRole
        );

        if (success) {
            displayAddMemberMessage("Invitation sent to " + selectedUser.getUsername() + " for role " + selectedRole.toString() + ".", false);

            // Send a notification to the invited user
            String notificationMessage = String.format(
                    "You have been invited to join the team '%s' as a %s. Please check your notifications to accept or reject.",
                    currentTeam.getName(), selectedRole.toString().toLowerCase()
            );
            notificationDAO.createNotification(new Notification(selectedUser, notificationMessage, LocalDateTime.now()));

            // Refresh lists
            populateAddMemberUserChoiceBox(); // Update available users
            refreshMembersList(); // Update current members list
        } else {
            displayAddMemberMessage("Failed to send invitation. User might already be invited or a member.", true);
        }
    }

    /**
     * Handles the action when the "Remove Selected Member" button is clicked.
     * Removes the selected user's membership from the team.
     */
    @FXML
    private void handleRemoveMember() {
        UserTeamMembership selectedMembership = membersListView.getSelectionModel().getSelectedItem();

        if (selectedMembership == null) {
            displayMembersListMessage("Please select a member to remove.", true);
            return;
        }

        // Prevent removing the owner if they are the only owner
        // This logic might need to be more robust in a real application (e.g., transfer ownership)
        if (selectedMembership.getRole() == UserTeamMembership.Role.OWNER) {
            long ownerCount = currentMemberships.stream()
                    .filter(m -> m.getRole() == UserTeamMembership.Role.OWNER)
                    .count();
            if (ownerCount <= 1) {
                displayMembersListMessage("Cannot remove the last owner of the team. Assign a new owner first.", true);
                return;
            }
        }

        boolean success = userManagerService.removeUserFromTeam(
                selectedMembership.getUserId(),
                selectedMembership.getTeamId()
        );

        if (success) {
            displayMembersListMessage(selectedMembership.getUser().getUsername() + " removed from team.", false);
            populateAddMemberUserChoiceBox(); // Update available users
            refreshMembersList(); // Refresh the list
        } else {
            displayMembersListMessage("Failed to remove member.", true);
        }
    }

    /**
     * Handles the action when the "Change Role" button is clicked.
     * This method would typically open a dialog to change the role of a selected member.
     */
    @FXML
    private void handleChangeRole() {
        UserTeamMembership selectedMembership = membersListView.getSelectionModel().getSelectedItem();

        if (selectedMembership == null) {
            displayMembersListMessage("Please select a member to change their role.", true);
            return;
        }

        System.out.println("Change Role button clicked for: " + selectedMembership.getUser().getUsername());
        displayMembersListMessage("Change Role functionality is not yet implemented.", false);
        // TODO: Implement a dialog to select new role and update via userManagerService
    }

    /**
     * Handles the action when the "Close" button is clicked.
     * Closes the current dialog window.
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) teamNameLabel.getScene().getWindow();
        stage.close();
    }

    /**
     * Displays a message to the user in the addMemberMessageLabel.
     *
     * @param message The message text to display.
     * @param isError True if the message is an error (will be red), false for success/info (will be black).
     */
    private void displayAddMemberMessage(String message, boolean isError) {
        addMemberMessageLabel.setText(message);
        if (isError) {
            addMemberMessageLabel.setStyle("-fx-text-fill: red;");
        } else {
            addMemberMessageLabel.setStyle("-fx-text-fill: black;");
        }
    }

    /**
     * Displays a message to the user in the membersListMessageLabel.
     *
     * @param message The message text to display.
     * @param isError True if the message is an error (will be red), false for success/info (will be black).
     */
    private void displayMembersListMessage(String message, boolean isError) {
        membersListMessageLabel.setText(message);
        if (isError) {
            membersListMessageLabel.setStyle("-fx-text-fill: red;");
        } else {
            membersListMessageLabel.setStyle("-fx-text-fill: black;");
        }
    }
}
