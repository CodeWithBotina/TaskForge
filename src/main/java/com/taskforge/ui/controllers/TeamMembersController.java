package com.taskforge.ui.controllers;

import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the TeamMembersView.fxml.
 * This class manages the display and interaction with members of a specific team.
 * It allows team owners to invite new members, change roles of existing members,
 * and remove members. It also displays pending invitations.
 */
public class TeamMembersController {

    @FXML
    private Label teamNameLabel;
    @FXML
    private ChoiceBox<User> inviteUserChoiceBox;
    @FXML
    private ChoiceBox<UserTeamMembership.Role> inviteRoleChoiceBox;
    @FXML
    private Label inviteMessageLabel;

    @FXML
    private TableView<UserTeamMembership> membersTable;
    @FXML
    private TableColumn<UserTeamMembership, Integer> memberIdColumn;
    @FXML
    private TableColumn<UserTeamMembership, String> memberNameColumn;
    @FXML
    private TableColumn<UserTeamMembership, String> memberEmailColumn;
    @FXML
    private TableColumn<UserTeamMembership, UserTeamMembership.Role> memberRoleColumn;
    @FXML
    private TableColumn<UserTeamMembership, UserTeamMembership.InvitationStatus> memberStatusColumn;
    @FXML
    private TableColumn<UserTeamMembership, Void> memberActionsColumn; // For Change Role, Remove buttons

    @FXML
    private Label messageLabel;

    private Team currentTeam; // The team whose members are being managed
    private User currentUser; // The currently logged-in user (must be an owner to manage)
    private UserManagerService userManagerService;
    private ObservableList<UserTeamMembership> memberList = FXCollections.observableArrayList();

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

        // Populate Invite Role ChoiceBox
        inviteRoleChoiceBox.setItems(FXCollections.observableArrayList(UserTeamMembership.Role.values()));
        inviteRoleChoiceBox.getSelectionModel().select(UserTeamMembership.Role.MEMBER); // Default

        // Configure Invite User ChoiceBox
        inviteUserChoiceBox.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user != null ? user.getUsername() : null;
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });

        // Configure table columns
        // Corrected: Access ID from the nested User object
        memberIdColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getUser().getId()).asObject());
        // Corrected: Access username from the nested User object
        memberNameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUser().getUsername()));
        // Corrected: Access email from the nested User object
        memberEmailColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUser().getEmail()));
        memberRoleColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getRole()));
        memberStatusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getInvitationStatus()));

        // Custom cell factory for Actions column (Change Role, Remove buttons)
        memberActionsColumn.setCellFactory(param -> new TableCell<UserTeamMembership, Void>() {
            private final Button changeRoleButton = new Button("Change Role");
            private final Button removeButton = new Button("Remove");
            private final HBox pane = new HBox(5, changeRoleButton, removeButton);

            {
                changeRoleButton.setOnAction(event -> {
                    UserTeamMembership membership = getTableView().getItems().get(getIndex());
                    handleChangeRole(membership);
                });
                removeButton.setOnAction(event -> {
                    UserTeamMembership membership = getTableView().getItems().get(getIndex());
                    handleRemoveMember(membership);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    UserTeamMembership membership = getTableView().getItems().get(getIndex());
                    // Only show action buttons if the current user is an OWNER of this team
                    // and the member is not the current user themselves (cannot remove/change own role via this UI)
                    // and the member is not the only owner
                    boolean isCurrentUserOwner = (currentUser != null && currentTeam != null && userManagerService.isTeamOwner(currentUser.getId(), currentTeam.getId()));
                    boolean isTargetUserCurrent = (currentUser != null && membership.getUser().getId() == currentUser.getId());
                    boolean isTargetUserOnlyOwner = (membership.getRole() == UserTeamMembership.Role.OWNER &&
                            userManagerService.getTeamMemberships(currentTeam.getId()).stream()
                                    .filter(m -> m.getRole() == UserTeamMembership.Role.OWNER && m.getInvitationStatus() == UserTeamMembership.InvitationStatus.ACCEPTED)
                                    .count() == 1);

                    if (isCurrentUserOwner && !isTargetUserCurrent && !isTargetUserOnlyOwner) {
                        setGraphic(pane);
                        changeRoleButton.setDisable(membership.getInvitationStatus() != UserTeamMembership.InvitationStatus.ACCEPTED); // Can only change role of accepted members
                        removeButton.setDisable(false);
                    } else {
                        setGraphic(null); // Hide buttons for non-owners, self, or only owner
                    }
                }
            }
        });

        membersTable.setItems(memberList);
    }

    /**
     * Sets the team whose members are to be managed.
     * This method should be called by the parent controller (e.g., TeamsController)
     * before showing this view.
     *
     * @param team The Team object to manage members for.
     */
    public void setTeam(Team team) {
        this.currentTeam = team;
        if (currentTeam != null) {
            teamNameLabel.setText("Team: " + currentTeam.getName());
            populateInviteUserChoiceBox(); // Populate users for invitation
            handleRefreshMembers(); // Load members for this team
        }
    }

    /**
     * Sets the currently logged-in user. This is crucial for permission checks
     * within the team member management.
     *
     * @param user The User object representing the logged-in user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Refresh members to update action button visibility based on current user's role
        if (currentTeam != null) {
            handleRefreshMembers();
        }
    }

    /**
     * Populates the `inviteUserChoiceBox` with users who are not yet members
     * (or do not have a pending invitation) of the current team.
     */
    private void populateInviteUserChoiceBox() {
        if (currentTeam == null) return;

        List<User> allUsers = userManagerService.getAllUsers();
        List<UserTeamMembership> existingMemberships = userManagerService.getTeamMemberships(currentTeam.getId());

        // Filter out users who are already members or have a pending invitation
        List<User> invitableUsers = allUsers.stream()
                .filter(user -> existingMemberships.stream()
                        .noneMatch(membership -> membership.getUser().equals(user) &&
                                (membership.getInvitationStatus() == UserTeamMembership.InvitationStatus.ACCEPTED ||
                                        membership.getInvitationStatus() == UserTeamMembership.InvitationStatus.PENDING)))
                .collect(Collectors.toList());

        inviteUserChoiceBox.setItems(FXCollections.observableArrayList(invitableUsers));
        inviteUserChoiceBox.getSelectionModel().clearSelection(); // Clear previous selection
    }

    /**
     * Handles sending an invitation to a selected user for the current team.
     */
    @FXML
    private void handleSendInvitation() {
        User selectedUser = inviteUserChoiceBox.getValue();
        UserTeamMembership.Role selectedRole = inviteRoleChoiceBox.getValue();

        if (selectedUser == null) {
            displayInviteMessage("Please select a user to invite.", true);
            return;
        }
        if (selectedRole == null) {
            displayInviteMessage("Please select a role for the invited user.", true);
            return;
        }
        if (currentTeam == null) {
            displayInviteMessage("No team selected.", true);
            return;
        }
        if (currentUser == null || !userManagerService.isTeamOwner(currentUser.getId(), currentTeam.getId())) {
            displayInviteMessage("You must be a team owner to send invitations.", true);
            return;
        }

        boolean success = userManagerService.inviteUserToTeam(selectedUser.getId(), currentTeam.getId(), selectedRole);

        if (success) {
            displayInviteMessage("Invitation sent to " + selectedUser.getUsername() + " successfully!", false);
            populateInviteUserChoiceBox(); // Refresh invitable users
            handleRefreshMembers(); // Refresh members table to show pending invitation
        } else {
            displayInviteMessage("Failed to send invitation. User might already be a member or invited.", true);
        }
    }

    /**
     * Handles refreshing the list of team members.
     */
    @FXML
    private void handleRefreshMembers() {
        if (currentTeam == null) {
            displayMessage("No team selected to refresh members.", true);
            return;
        }
        System.out.println("Refreshing members for team: " + currentTeam.getName());
        memberList.clear(); // Clear existing items
        List<UserTeamMembership> memberships = userManagerService.getTeamMemberships(currentTeam.getId());
        memberList.addAll(memberships);

        if (memberList.isEmpty()) {
            displayMessage("No members found for this team.", false);
        } else {
            displayMessage("Team members refreshed successfully.", false);
        }
        // Refresh the table to ensure action buttons are updated based on permissions
        membersTable.refresh();
    }

    /**
     * Handles changing the role of a team member.
     * Opens a dialog to select a new role.
     *
     * @param membership The UserTeamMembership to modify.
     */
    private void handleChangeRole(UserTeamMembership membership) {
        if (currentUser == null || !userManagerService.isTeamOwner(currentUser.getId(), currentTeam.getId())) {
            displayMessage("You do not have permission to change roles.", true);
            return;
        }
        if (membership.getInvitationStatus() != UserTeamMembership.InvitationStatus.ACCEPTED) {
            displayMessage("Cannot change role of a non-accepted member.", true);
            return;
        }
        if (membership.getUser().getId() == currentUser.getId()) {
            displayMessage("You cannot change your own role via this interface.", true);
            return;
        }
        // Prevent changing the role of the only owner
        if (membership.getRole() == UserTeamMembership.Role.OWNER &&
                userManagerService.getTeamMemberships(currentTeam.getId()).stream()
                        .filter(m -> m.getRole() == UserTeamMembership.Role.OWNER && m.getInvitationStatus() == UserTeamMembership.InvitationStatus.ACCEPTED)
                        .count() == 1) {
            displayMessage("Cannot change role: This user is the only owner of the team.", true);
            return;
        }

        // Simple way to get new role (for now, could use a dialog)
        // For a more robust solution, you'd open a small dialog with a ChoiceBox for roles.
        // For this example, let's assume a toggle or a simple prompt.
        // For now, let's just print a message and skip actual role change until a proper UI is built.
        displayMessage("Role change functionality to be implemented. For now, please manually update if needed.", false);
        System.out.println("Attempting to change role for " + membership.getUser().getUsername() + " from " + membership.getRole());

        // Example of how you would do it if you had a dialog for new role selection:
        // Optional<UserTeamMembership.Role> newRole = showRoleSelectionDialog();
        // if (newRole.isPresent()) {
        //     boolean success = userManagerService.updateTeamMemberRole(membership.getUser().getId(), currentTeam.getId(), newRole.get());
        //     if (success) {
        //         displayMessage("Role updated successfully.", false);
        //         handleRefreshMembers();
        //     } else {
        //         displayMessage("Failed to update role.", true);
        //     }
        // }
    }

    /**
     * Handles removing a member from the team.
     *
     * @param membership The UserTeamMembership to remove.
     */
    private void handleRemoveMember(UserTeamMembership membership) {
        if (currentUser == null || !userManagerService.isTeamOwner(currentUser.getId(), currentTeam.getId())) {
            displayMessage("You do not have permission to remove members.", true);
            return;
        }
        if (membership.getUser().getId() == currentUser.getId()) {
            displayMessage("You cannot remove yourself from the team via this interface. Use 'Leave Team' if available.", true);
            return;
        }
        // Prevent removing the only owner
        if (membership.getRole() == UserTeamMembership.Role.OWNER &&
                userManagerService.getTeamMemberships(currentTeam.getId()).stream()
                        .filter(m -> m.getRole() == UserTeamMembership.Role.OWNER && m.getInvitationStatus() == UserTeamMembership.InvitationStatus.ACCEPTED)
                        .count() == 1) {
            displayMessage("Cannot remove: This user is the only owner of the team.", true);
            return;
        }

        // In a real application, you'd want a confirmation dialog here.
        boolean success = userManagerService.removeUserFromTeam(membership.getUser().getId(), currentTeam.getId());
        if (success) {
            displayMessage("Member " + membership.getUser().getUsername() + " removed successfully.", false);
            populateInviteUserChoiceBox(); // Refresh invitable users
            handleRefreshMembers(); // Refresh members table
        } else {
            displayMessage("Failed to remove member.", true);
        }
    }

    /**
     * Handles closing the dialog window.
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) teamNameLabel.getScene().getWindow();
        stage.close();
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
     * Displays a message specifically for the invite new member section.
     *
     * @param message The message text to display.
     * @param isError True if the message is an error (will be red), false for success/info (will be black).
     */
    private void displayInviteMessage(String message, boolean isError) {
        inviteMessageLabel.setText(message);
        if (isError) {
            inviteMessageLabel.setStyle("-fx-text-fill: red;");
        } else {
            inviteMessageLabel.setStyle("-fx-text-fill: black;");
        }
    }
}
