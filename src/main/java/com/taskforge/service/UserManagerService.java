package com.taskforge.service;

import com.taskforge.dao.TeamDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
import com.taskforge.dao.NotificationDAO;
import com.taskforge.model.Team;
import com.taskforge.model.User;
import com.taskforge.model.Notification;
import com.taskforge.model.UserTeamMembership;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class responsible for managing user and team related business logic.
 * This class orchestrates operations between UserDAO, TeamDAO, and UserTeamDAO
 * to provide a higher-level API for user and team management, including memberships and invitations.
 */
public class UserManagerService {

    private final UserDAO userDAO;
    private final TeamDAO teamDAO;
    private final UserTeamDAO userTeamDAO;
    private final NotificationDAO notificationDAO; // Added for sending notifications

    /**
     * Constructs a UserManagerService with necessary DAO dependencies.
     * This allows for dependency injection, making the class more testable and modular.
     *
     * @param userDAO The Data Access Object for User entities.
     * @param teamDAO The Data Access Object for Team entities.
     * @param userTeamDAO The Data Access Object for UserTeamMembership entities.
     */
    public UserManagerService(UserDAO userDAO, TeamDAO teamDAO, UserTeamDAO userTeamDAO) {
        this.userDAO = userDAO;
        this.teamDAO = teamDAO;
        this.userTeamDAO = userTeamDAO;
        this.notificationDAO = new NotificationDAO(userDAO); // Initialize NotificationDAO here
    }

    /**
     * Registers a new user. This method is typically called by AuthService, but included for completeness.
     *
     * @param user The User object to register.
     * @return The registered User object with ID, or null if registration fails.
     */
    public User registerUser(User user) {
        return userDAO.createUser(user);
    }

    /**
     * Authenticates a user. This method is typically called by AuthService, but included for completeness.
     *
     * @param username The username.
     * @param passwordHash The hashed password.
     * @return An Optional containing the User if authenticated, otherwise empty.
     */
    public Optional<User> authenticateUser(String username, String passwordHash) {
        Optional<User> user = userDAO.getUserByUsername(username);
        if (user.isPresent() && user.get().getPasswordHash().equals(passwordHash)) {
            return user;
        }
        return Optional.empty();
    }

    /**
     * Updates an existing user's details.
     *
     * @param userId The ID of the user to update.
     * @param newUsername The new username.
     * @param newEmail The new email.
     * @return true if the user was updated successfully, false otherwise.
     */
    public boolean updateUser(int userId, String newUsername, String newEmail) {
        Optional<User> userOptional = userDAO.getUserById(userId);
        if (userOptional.isEmpty()) {
            System.err.println("User update failed: User with ID " + userId + " not found.");
            return false;
        }

        User userToUpdate = userOptional.get();

        // Check if new username or email already exists for another user
        Optional<User> existingUserByUsername = userDAO.getUserByUsername(newUsername);
        if (existingUserByUsername.isPresent() && existingUserByUsername.get().getId() != userId) {
            System.err.println("User update failed: Username '" + newUsername + "' already taken by another user.");
            return false;
        }
        Optional<User> existingUserByEmail = userDAO.getUserByEmail(newEmail);
        if (existingUserByEmail.isPresent() && existingUserByEmail.get().getId() != userId) {
            System.err.println("User update failed: Email '" + newEmail + "' already taken by another user.");
            return false;
        }

        userToUpdate.setUsername(newUsername);
        userToUpdate.setEmail(newEmail);

        boolean success = userDAO.updateUser(userToUpdate);
        if (success) {
            System.out.println("User ID " + userId + " updated successfully.");
        } else {
            System.err.println("User update failed: Database operation failed.");
        }
        return success;
    }

    /**
     * Deletes a user from the system.
     *
     * @param userId The ID of the user to delete.
     * @return true if the user was deleted successfully, false otherwise.
     */
    public boolean deleteUser(int userId) {
        boolean success = userDAO.deleteUser(userId);
        if (success) {
            System.out.println("User ID " + userId + " deleted successfully.");
        } else {
            System.err.println("User deletion failed: User with ID " + userId + " not found or database error.");
        }
        return success;
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId The ID of the user.
     * @return An Optional containing the User object if found, or empty.
     */
    public Optional<User> getUserById(int userId) {
        return userDAO.getUserById(userId);
    }

    /**
     * Retrieves all users in the system.
     *
     * @return A list of all User objects.
     */
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    /**
     * Creates a new team and assigns the creator as an OWNER.
     *
     * @param teamName The name of the new team.
     * @param creatorUserId The ID of the user creating and owning the team.
     * @return An Optional containing the created Team object if successful, or empty if creation fails.
     */
    public Optional<Team> createTeam(String teamName, int creatorUserId) {
        if (teamName == null || teamName.trim().isEmpty()) {
            System.err.println("Team creation failed: Team name cannot be empty.");
            return Optional.empty();
        }
        if (teamDAO.getTeamByName(teamName).isPresent()) {
            System.err.println("Team creation failed: Team name '" + teamName + "' already exists.");
            return Optional.empty();
        }

        Optional<User> creatorOptional = userDAO.getUserById(creatorUserId);
        if (creatorOptional.isEmpty()) {
            System.err.println("Team creation failed: Creator user with ID " + creatorUserId + " not found.");
            return Optional.empty();
        }

        Team newTeam = new Team(teamName);
        Team createdTeam = teamDAO.createTeam(newTeam);

        if (createdTeam != null) {
            // Assign the creator as an OWNER of the team
            UserTeamMembership ownerMembership = new UserTeamMembership(
                    creatorOptional.get(),
                    createdTeam,
                    UserTeamMembership.Role.OWNER,
                    UserTeamMembership.InvitationStatus.ACCEPTED
            );
            if (userTeamDAO.createMembership(ownerMembership)) {
                System.out.println("Team created successfully: " + createdTeam.getName() + " and owner assigned.");
                return Optional.of(createdTeam);
            } else {
                System.err.println("Error creating user-team membership: Failed to assign owner. Deleting team.");
                teamDAO.deleteTeam(createdTeam.getId()); // Rollback team creation
                return Optional.empty();
            }
        } else {
            System.err.println("Team creation failed: Database operation failed.");
            return Optional.empty();
        }
    }

    /**
     * Updates an existing team's details.
     *
     * @param teamId The ID of the team to update.
     * @param newTeamName The new name for the team.
     * @return true if the team was updated successfully, false otherwise.
     */
    public boolean updateTeam(int teamId, String newTeamName) {
        Optional<Team> existingTeamOptional = teamDAO.getTeamById(teamId);
        if (existingTeamOptional.isEmpty()) {
            System.err.println("Team update failed: Team with ID " + teamId + " not found.");
            return false;
        }

        // Check if the new team name already exists for another team
        Optional<Team> teamByName = teamDAO.getTeamByName(newTeamName);
        if (teamByName.isPresent() && teamByName.get().getId() != teamId) {
            System.err.println("Team update failed: Team name '" + newTeamName + "' already exists.");
            return false;
        }

        Team teamToUpdate = existingTeamOptional.get();
        teamToUpdate.setName(newTeamName);

        boolean success = teamDAO.updateTeam(teamToUpdate);
        if (success) {
            System.out.println("Team ID " + teamId + " updated successfully.");
        } else {
            System.err.println("Team update failed: Database operation failed.");
        }
        return success;
    }

    /**
     * Deletes a team from the system.
     *
     * @param teamId The ID of the team to delete.
     * @return true if the team was deleted successfully, false otherwise.
     */
    public boolean deleteTeam(int teamId) {
        // In a real application, you might want to check for associated projects/tasks
        // and handle them before deleting the team, or rely on CASCADE DELETE in DB schema.
        boolean success = teamDAO.deleteTeam(teamId);
        if (success) {
            System.out.println("Team ID " + teamId + " deleted successfully.");
        } else {
            System.err.println("Team deletion failed: Team with ID " + teamId + " not found or database error.");
        }
        return success;
    }

    /**
     * Retrieves a team by its ID.
     *
     * @param teamId The ID of the team.
     * @return An Optional containing the Team object if found, or empty.
     */
    public Optional<Team> getTeamById(int teamId) {
        return teamDAO.getTeamById(teamId);
    }

    /**
     * Retrieves all teams in the system.
     *
     * @return A list of all Team objects.
     */
    public List<Team> getAllTeams() {
        return teamDAO.getAllTeams();
    }

    /**
     * Retrieves all teams that a specific user is a member of (status ACCEPTED).
     *
     * @param userId The ID of the user.
     * @return A list of Team objects the user is a member of.
     */
    public List<Team> getTeamsForUser(int userId) {
        return userTeamDAO.getMembershipsByUserId(userId).stream()
                .filter(membership -> membership.getInvitationStatus() == UserTeamMembership.InvitationStatus.ACCEPTED)
                .map(UserTeamMembership::getTeam)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a user is an owner of a specific team.
     *
     * @param userId The ID of the user.
     * @param teamId The ID of the team.
     * @return true if the user is an owner of the team, false otherwise.
     */
    public boolean isTeamOwner(int userId, int teamId) {
        return userTeamDAO.getMembership(userId, teamId)
                .filter(membership -> membership.getRole() == UserTeamMembership.Role.OWNER &&
                        membership.getInvitationStatus() == UserTeamMembership.InvitationStatus.ACCEPTED)
                .isPresent();
    }

    /**
     * Checks if two users are members of at least one common team (status ACCEPTED).
     *
     * @param userId1 The ID of the first user.
     * @param userId2 The ID of the second user.
     * @return true if they share at least one common team, false otherwise.
     */
    public boolean areUsersInSameTeam(int userId1, int userId2) {
        List<Team> teams1 = getTeamsForUser(userId1);
        List<Team> teams2 = getTeamsForUser(userId2);

        // Check for any common team
        return teams1.stream().anyMatch(teams2::contains);
    }

    /**
     * Checks if a user is an active member of a specific team (status ACCEPTED).
     *
     * @param userId The ID of the user.
     * @param teamId The ID of the team.
     * @return true if the user is an active member of the team, false otherwise.
     */
    public boolean isUserMemberOfTeam(int userId, int teamId) {
        return userTeamDAO.getMembership(userId, teamId)
                .filter(membership -> membership.getInvitationStatus() == UserTeamMembership.InvitationStatus.ACCEPTED)
                .isPresent();
    }

    /**
     * Retrieves all UserTeamMembership objects for a given team.
     *
     * @param teamId The ID of the team.
     * @return A list of UserTeamMembership objects for the team.
     */
    public List<UserTeamMembership> getTeamMemberships(int teamId) {
        return userTeamDAO.getMembershipsByTeamId(teamId);
    }

    /**
     * Invites a user to a team, creating a PENDING membership and sending a notification.
     *
     * @param userId The ID of the user to invite.
     * @param teamId The ID of the team to invite the user to.
     * @param role The role the invited user will have if they accept.
     * @return true if the invitation was successfully sent, false otherwise.
     */
    public boolean inviteUserToTeam(int userId, int teamId, UserTeamMembership.Role role) {
        Optional<User> userOptional = userDAO.getUserById(userId);
        Optional<Team> teamOptional = teamDAO.getTeamById(teamId);

        if (userOptional.isEmpty()) {
            System.err.println("Invitation failed: User with ID " + userId + " not found.");
            return false;
        }
        if (teamOptional.isEmpty()) {
            System.err.println("Invitation failed: Team with ID " + teamId + " not found.");
            return false;
        }

        // Check if user is already a member or has a pending invitation
        if (userTeamDAO.getMembership(userId, teamId).isPresent()) {
            System.err.println("Invitation failed: User " + userOptional.get().getUsername() + " is already a member or has a pending invitation for team " + teamOptional.get().getName() + ".");
            return false;
        }

        UserTeamMembership newMembership = new UserTeamMembership(
                userOptional.get(),
                teamOptional.get(),
                role,
                UserTeamMembership.InvitationStatus.PENDING
        );

        boolean success = userTeamDAO.createMembership(newMembership);

        if (success) {
            // Send a notification to the invited user
            String message = String.format("You have been invited to join the team '%s' as a %s.",
                    teamOptional.get().getName(), role.name().toLowerCase());
            Notification notification = new Notification(
                    userOptional.get(),
                    message,
                    LocalDateTime.now(),
                    teamId, // relatedEntityId is the team ID
                    Notification.NotificationType.TEAM_INVITATION
            );
            notificationDAO.createNotification(notification);
            System.out.println("User " + userOptional.get().getUsername() + " invited to team " + teamOptional.get().getName() + ".");
        } else {
            System.err.println("Invitation failed: Database error during membership creation.");
        }
        return success;
    }

    /**
     * Accepts a pending team invitation for a user.
     *
     * @param userId The ID of the user accepting the invitation.
     * @param teamId The ID of the team for which the invitation is being accepted.
     * @return true if the invitation was successfully accepted, false otherwise.
     */
    public boolean acceptTeamInvitation(int userId, int teamId) {
        Optional<UserTeamMembership> membershipOptional = userTeamDAO.getMembership(userId, teamId);

        if (membershipOptional.isEmpty()) {
            System.err.println("Accept invitation failed: No membership found for user " + userId + " in team " + teamId + ".");
            return false;
        }

        UserTeamMembership membership = membershipOptional.get();
        if (membership.getInvitationStatus() != UserTeamMembership.InvitationStatus.PENDING) {
            System.err.println("Accept invitation failed: Membership for user " + userId + " in team " + teamId + " is not in PENDING status. Current status: " + membership.getInvitationStatus());
            return false;
        }

        membership.setInvitationStatus(UserTeamMembership.InvitationStatus.ACCEPTED);
        boolean success = userTeamDAO.updateMembership(membership);

        if (success) {
            System.out.println("User " + userId + " successfully accepted invitation to team " + teamId + ".");
        } else {
            System.err.println("Accept invitation failed: Database error during membership update.");
        }
        return success;
    }

    /**
     * Rejects a pending team invitation for a user.
     *
     * @param userId The ID of the user rejecting the invitation.
     * @param teamId The ID of the team for which the invitation is being rejected.
     * @return true if the invitation was successfully rejected, false otherwise.
     */
    public boolean rejectTeamInvitation(int userId, int teamId) {
        Optional<UserTeamMembership> membershipOptional = userTeamDAO.getMembership(userId, teamId);

        if (membershipOptional.isEmpty()) {
            System.err.println("Reject invitation failed: No membership found for user " + userId + " in team " + teamId + ".");
            return false;
        }

        UserTeamMembership membership = membershipOptional.get();
        if (membership.getInvitationStatus() != UserTeamMembership.InvitationStatus.PENDING) {
            System.err.println("Reject invitation failed: Membership for user " + userId + " in team " + teamId + " is not in PENDING status. Current status: " + membership.getInvitationStatus());
            return false;
        }

        // For rejection, we can simply delete the membership record
        boolean success = userTeamDAO.deleteMembership(userId, teamId);

        if (success) {
            System.out.println("User " + userId + " successfully rejected invitation to team " + teamId + ".");
        } else {
            System.err.println("Reject invitation failed: Database error during membership deletion.");
        }
        return success;
    }

    /**
     * Removes a user from a team. This can be used by an owner to remove a member,
     * or by a member to leave a team (if permitted by UI/logic).
     *
     * @param userId The ID of the user to remove.
     * @param teamId The ID of the team to remove the user from.
     * @return true if the user was successfully removed, false otherwise.
     */
    public boolean removeUserFromTeam(int userId, int teamId) {
        Optional<UserTeamMembership> membershipOptional = userTeamDAO.getMembership(userId, teamId);
        if (membershipOptional.isEmpty()) {
            System.err.println("Removal failed: User " + userId + " is not a member of team " + teamId + ".");
            return false;
        }

        // Additional logic could be added here to check permissions (e.g., only owner can remove others)
        // For now, it directly attempts to delete the membership.

        boolean success = userTeamDAO.deleteMembership(userId, teamId);
        if (success) {
            System.out.println("User " + userId + " removed from team " + teamId + " successfully.");
        } else {
            System.err.println("Removal failed: Database error during membership deletion.");
        }
        return success;
    }

    /**
     * Updates the role of a team member.
     *
     * @param userId The ID of the user whose role is to be updated.
     * @param teamId The ID of the team.
     * @param newRole The new role for the user.
     * @return true if the role was updated successfully, false otherwise.
     */
    public boolean updateTeamMemberRole(int userId, int teamId, UserTeamMembership.Role newRole) {
        Optional<UserTeamMembership> membershipOptional = userTeamDAO.getMembership(userId, teamId);
        if (membershipOptional.isEmpty()) {
            System.err.println("Role update failed: User " + userId + " is not a member of team " + teamId + ".");
            return false;
        }

        UserTeamMembership membershipToUpdate = membershipOptional.get();
        membershipToUpdate.setRole(newRole);

        boolean success = userTeamDAO.updateMembership(membershipToUpdate);
        if (success) {
            System.out.println("User " + userId + "'s role in team " + teamId + " updated to " + newRole + ".");
        } else {
            System.err.println("Role update failed: Database error during membership update.");
        }
        return success;
    }

    /**
     * Retrieves a specific user-team membership.
     *
     * @param userId The ID of the user.
     * @param teamId The ID of the team.
     * @return An Optional containing the UserTeamMembership object if found, or an empty Optional.
     */
    public Optional<UserTeamMembership> getMembership(int userId, int teamId) {
        return userTeamDAO.getMembership(userId, teamId);
    }

    /**
     * Retrieves a list of users who are members of a specific team (status ACCEPTED).
     *
     * @param teamId The ID of the team.
     * @return A list of User objects who are active members of the team.
     */
    public List<User> getUsersInTeam(int teamId) {
        return userTeamDAO.getMembershipsByTeamId(teamId).stream()
                .filter(membership -> membership.getInvitationStatus() == UserTeamMembership.InvitationStatus.ACCEPTED)
                .map(UserTeamMembership::getUser)
                .collect(Collectors.toList());
    }
}
