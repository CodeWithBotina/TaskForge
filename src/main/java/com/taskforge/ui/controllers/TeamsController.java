package com.taskforge.ui.controllers;

import com.taskforge.dao.ProjectDAO;
import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
import com.taskforge.model.Team;
import com.taskforge.model.User;
import com.taskforge.model.UserTeamMembership;
import com.taskforge.service.ProjectManagerService;
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
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the TeamsView.fxml.
 * This class manages the display and interaction with teams in the system.
 * It provides functionalities for creating new teams, editing existing ones,
 * inviting users to teams, and managing team members.
 * It enforces ownership-based permissions for team and membership management.
 */
public class TeamsController {

    @FXML
    private TextField newTeamNameField;
    @FXML
    private Label newTeamMessageLabel;

    @FXML
    private TableView<Team> teamsTable;
    @FXML
    private TableColumn<Team, Integer> teamIdColumn;
    @FXML
    private TableColumn<Team, String> teamNameColumn;
    @FXML
    private TableColumn<Team, String> teamMembersColumn; // To display formatted list of members
    @FXML
    private TableColumn<Team, Void> teamActionsColumn; // For buttons like Edit, Delete, Manage Members

    @FXML
    private Label messageLabel;

    private User currentUser; // The currently logged-in user
    private UserManagerService userManagerService;
    private ProjectManagerService projectManagerService; // Needed for cascade delete check for projects

    private ObservableList<Team> teamList = FXCollections.observableArrayList();

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
        ProjectDAO projectDAO = new ProjectDAO(teamDAO); // For ProjectManagerService

        // Initialize Services
        this.userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);
        this.projectManagerService = new ProjectManagerService(projectDAO, teamDAO);

        // Configure table columns
        teamIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        teamNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Custom cell factory for Team Members to display a comma-separated list of usernames
        teamMembersColumn.setCellValueFactory(cellData -> {
            Team team = cellData.getValue();
            // Fetch active members for the team
            List<User> members = userManagerService.getUsersInTeam(team.getId());
            String memberNames = members.stream()
                    .map(User::getUsername)
                    .collect(Collectors.joining(", "));
            return new javafx.beans.property.SimpleStringProperty(memberNames);
        });

        // Custom cell factory for Actions column (Edit, Delete, Manage Members buttons)
        teamActionsColumn.setCellFactory(new Callback<TableColumn<Team, Void>, TableCell<Team, Void>>() {
            @Override
            public TableCell<Team, Void> call(final TableColumn<Team, Void> param) {
                final TableCell<Team, Void> cell = new TableCell<Team, Void>() {
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Delete");
                    private final Button manageMembersButton = new Button("Members");
                    private final HBox pane = new HBox(5, editButton, deleteButton, manageMembersButton);

                    {
                        editButton.setOnAction(event -> {
                            Team team = getTableView().getItems().get(getIndex());
                            handleEditTeam(team);
                        });
                        deleteButton.setOnAction(event -> {
                            Team team = getTableView().getItems().get(getIndex());
                            handleDeleteTeam(team);
                        });
                        manageMembersButton.setOnAction(event -> {
                            Team team = getTableView().getItems().get(getIndex());
                            handleManageTeamMembers(team);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Team team = getTableView().getItems().get(getIndex());
                            // Only show action buttons if the current user is an OWNER of this team
                            if (currentUser != null && userManagerService.isTeamOwner(currentUser.getId(), team.getId())) {
                                setGraphic(pane);
                                editButton.setDisable(false);
                                deleteButton.setDisable(false);
                                manageMembersButton.setDisable(false);
                            } else {
                                setGraphic(null); // Hide buttons for non-owners
                            }
                        }
                    }
                };
                return cell;
            }
        });

        teamsTable.setItems(teamList);

        // Teams will be refreshed when setCurrentUser is called by DashboardController
    }

    /**
     * Sets the currently logged-in user. This method should be called by the DashboardController
     * after this view is loaded. Once the user is set, it triggers a refresh of teams.
     *
     * @param user The User object representing the logged-in user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            handleRefreshTeams(); // Automatically load teams for the user
        } else {
            displayMessage("No user logged in. Cannot display teams.", true);
            teamList.clear(); // Clear any existing teams
        }
    }

    /**
     * Handles the action when the "Add Team" button is clicked.
     * Collects input from the form fields and attempts to create a new team.
     * The current user is automatically set as the owner.
     */
    @FXML
    private void handleAddTeam() {
        String teamName = newTeamNameField.getText();

        if (teamName.isEmpty()) {
            displayNewTeamMessage("Team name cannot be empty.", true);
            return;
        }
        if (currentUser == null) {
            displayNewTeamMessage("Cannot create team: No user logged in.", true);
            return;
        }

        Optional<Team> createdTeam = userManagerService.createTeam(teamName, currentUser.getId());

        if (createdTeam.isPresent()) {
            displayNewTeamMessage("Team '" + createdTeam.get().getName() + "' created successfully!", false);
            handleClearForm(); // Clear the form after successful addition
            handleRefreshTeams(); // Refresh the table to show the new team
        } else {
            displayNewTeamMessage("Failed to create team. Team name might already exist.", true);
        }
    }

    /**
     * Handles the action when the "Clear Form" button is clicked.
     * Clears all input fields in the new team creation form.
     */
    @FXML
    private void handleClearForm() {
        newTeamNameField.clear();
        newTeamMessageLabel.setText(""); // Clear any messages
    }

    /**
     * Handles the action when the "Refresh Teams" button is clicked.
     * This method reloads all teams from the database that the current user is a member of.
     */
    @FXML
    private void handleRefreshTeams() {
        if (currentUser == null) {
            displayMessage("Cannot refresh teams: No user is logged in.", true);
            return;
        }
        System.out.println("Refreshing teams for user: " + currentUser.getUsername());
        teamList.clear(); // Clear existing items
        // Fetch only teams the current user is an active member of
        List<Team> teams = userManagerService.getTeamsForUser(currentUser.getId());
        teamList.addAll(teams); // Add refreshed teams

        if (teams.isEmpty()) {
            displayMessage("You are not a member of any teams.", false);
        } else {
            displayMessage("Teams refreshed successfully.", false);
        }
        // Force update of the actions column to reflect current user's ownership
        teamsTable.refresh();
    }

    /**
     * Handles the action for the "Edit" button in a team row.
     * This method opens a new modal dialog to edit the selected team.
     * Only callable by a team owner.
     *
     * @param team The Team object to be edited.
     */
    private void handleEditTeam(Team team) {
        if (currentUser == null || !userManagerService.isTeamOwner(currentUser.getId(), team.getId())) {
            displayMessage("You do not have permission to edit this team.", true);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/EditTeamView.fxml"));
            Parent editTeamRoot = loader.load();

            EditTeamController editTeamController = loader.getController();
            editTeamController.setTeam(team);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Team: " + team.getName());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(teamsTable.getScene().getWindow());
            Scene scene = new Scene(editTeamRoot);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            handleRefreshTeams(); // Refresh the table after dialog closes
        } catch (IOException e) {
            System.err.println("Error loading EditTeamView.fxml: " + e.getMessage());
            e.printStackTrace();
            displayMessage("Error opening team edit dialog.", true);
        }
    }

    /**
     * Handles the action for the "Delete" button in a team row.
     * This method deletes the selected team from the database.
     * Only callable by a team owner.
     *
     * @param team The Team object to be deleted.
     */
    private void handleDeleteTeam(Team team) {
        if (currentUser == null || !userManagerService.isTeamOwner(currentUser.getId(), team.getId())) {
            displayMessage("You do not have permission to delete this team.", true);
            return;
        }

        // In a real application, you'd want a confirmation dialog here.
        // Also, consider if projects associated with this team should be handled (e.g., re-assigned or deleted)
        // For now, cascade delete in DBManager handles projects.

        boolean success = userManagerService.deleteTeam(team.getId());
        if (success) {
            displayMessage("Team '" + team.getName() + "' deleted successfully.", false);
            handleRefreshTeams(); // Refresh the table after deletion
        } else {
            displayMessage("Failed to delete team '" + team.getName() + "'.", true);
        }
    }

    /**
     * Handles the action for the "Manage Members" button in a team row.
     * This method opens a new modal dialog to manage members of the selected team.
     * Only callable by a team owner.
     *
     * @param team The Team object whose members are to be managed.
     */
    private void handleManageTeamMembers(Team team) {
        if (currentUser == null || !userManagerService.isTeamOwner(currentUser.getId(), team.getId())) {
            displayMessage("You do not have permission to manage members of this team.", true);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/TeamMembersView.fxml"));
            Parent teamMembersRoot = loader.load();

            TeamMembersController teamMembersController = loader.getController();
            teamMembersController.setTeam(team);
            teamMembersController.setCurrentUser(currentUser); // Pass current user for permission checks

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Manage Members for: " + team.getName());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(teamsTable.getScene().getWindow());
            Scene scene = new Scene(teamMembersRoot);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            handleRefreshTeams(); // Refresh the table after dialog closes
        } catch (IOException e) {
            System.err.println("Error loading TeamMembersView.fxml: " + e.getMessage());
            e.printStackTrace();
            displayMessage("Error opening team members management dialog.", true);
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
     * Displays a message specifically for the new team creation section.
     *
     * @param message The message text to display.
     * @param isError True if the message is an error (will be red), false for success/info (will be black).
     */
    private void displayNewTeamMessage(String message, boolean isError) {
        newTeamMessageLabel.setText(message);
        if (isError) {
            newTeamMessageLabel.setStyle("-fx-text-fill: red;");
        } else {
            newTeamMessageLabel.setStyle("-fx-text-fill: black;");
        }
    }
}
