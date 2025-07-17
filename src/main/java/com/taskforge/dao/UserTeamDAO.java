package com.taskforge.dao;

import com.taskforge.model.Team;
import com.taskforge.model.User;
import com.taskforge.model.UserTeamMembership;
import com.taskforge.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for the UserTeamMembership entity.
 * This class provides methods to perform CRUD operations on the 'UserTeamMemberships' table
 * in the database, managing the many-to-many relationship between Users and Teams.
 */
public class UserTeamDAO {

    private final UserDAO userDAO;
    private final TeamDAO teamDAO;

    /**
     * Constructs a UserTeamDAO with UserDAO and TeamDAO dependencies.
     *
     * @param userDAO The Data Access Object for User entities.
     * @param teamDAO The Data Access Object for Team entities.
     */
    public UserTeamDAO(UserDAO userDAO, TeamDAO teamDAO) {
        this.userDAO = userDAO;
        this.teamDAO = teamDAO;
    }

    /**
     * Creates a new user-team membership record in the database.
     *
     * @param membership The UserTeamMembership object to be saved.
     * @return true if the membership was created successfully, false otherwise.
     */
    public boolean createMembership(UserTeamMembership membership) {
        String sql = "INSERT INTO UserTeamMemberships(user_id, team_id, role, invitation_status) VALUES(?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Use getId() on User and Team objects
            pstmt.setInt(1, membership.getUser().getId());
            pstmt.setInt(2, membership.getTeam().getId());
            pstmt.setString(3, membership.getRole().name());
            pstmt.setString(4, membership.getInvitationStatus().name());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user-team membership: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves a specific user-team membership by user ID and team ID.
     *
     * @param userId The ID of the user.
     * @param teamId The ID of the team.
     * @return An Optional containing the UserTeamMembership object if found, or an empty Optional.
     */
    public Optional<UserTeamMembership> getMembership(int userId, int teamId) {
        String sql = "SELECT user_id, team_id, role, invitation_status FROM UserTeamMemberships WHERE user_id = ? AND team_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, teamId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractMembershipFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user-team membership: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Retrieves all memberships for a given user.
     *
     * @param userId The ID of the user.
     * @return A list of UserTeamMembership objects for the given user.
     */
    public List<UserTeamMembership> getMembershipsByUserId(int userId) {
        List<UserTeamMembership> memberships = new ArrayList<>();
        String sql = "SELECT user_id, team_id, role, invitation_status FROM UserTeamMemberships WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    memberships.add(extractMembershipFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving memberships by user ID: " + e.getMessage());
        }
        return memberships;
    }

    /**
     * Retrieves all memberships for a given team.
     *
     * @param teamId The ID of the team.
     * @return A list of UserTeamMembership objects for the given team.
     */
    public List<UserTeamMembership> getMembershipsByTeamId(int teamId) {
        List<UserTeamMembership> memberships = new ArrayList<>();
        String sql = "SELECT user_id, team_id, role, invitation_status FROM UserTeamMemberships WHERE team_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, teamId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    memberships.add(extractMembershipFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving memberships by team ID: " + e.getMessage());
        }
        return memberships;
    }

    /**
     * Updates an existing user-team membership record in the database.
     *
     * @param membership The UserTeamMembership object containing the updated information.
     * @return true if the membership was updated successfully, false otherwise.
     */
    public boolean updateMembership(UserTeamMembership membership) {
        String sql = "UPDATE UserTeamMemberships SET role = ?, invitation_status = ? WHERE user_id = ? AND team_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, membership.getRole().name());
            pstmt.setString(2, membership.getInvitationStatus().name());
            // Use getId() on User and Team objects
            pstmt.setInt(3, membership.getUser().getId());
            pstmt.setInt(4, membership.getTeam().getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user-team membership: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deletes a user-team membership record from the database.
     *
     * @param userId The ID of the user in the membership.
     * @param teamId The ID of the team in the membership.
     * @return true if the membership was deleted successfully, false otherwise.
     */
    public boolean deleteMembership(int userId, int teamId) {
        String sql = "DELETE FROM UserTeamMemberships WHERE user_id = ? AND team_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, teamId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user-team membership: " + e.getMessage());
        }
        return false;
    }

    /**
     * Helper method to extract a UserTeamMembership object from a ResultSet.
     * This centralizes the logic for mapping database rows to UserTeamMembership objects.
     *
     * @param rs The ResultSet containing the membership data.
     * @return A UserTeamMembership object populated with data from the ResultSet.
     * @throws SQLException If a database access error occurs or this method is called on a closed result set.
     */
    private UserTeamMembership extractMembershipFromResultSet(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        int teamId = rs.getInt("team_id");
        UserTeamMembership.Role role = UserTeamMembership.Role.valueOf(rs.getString("role"));
        UserTeamMembership.InvitationStatus status = UserTeamMembership.InvitationStatus.valueOf(rs.getString("invitation_status"));

        User user = userDAO.getUserById(userId)
                .orElseThrow(() -> new SQLException("Associated User not found for membership: " + userId));
        Team team = teamDAO.getTeamById(teamId)
                .orElseThrow(() -> new SQLException("Associated Team not found for membership: " + teamId));

        // Use the new constructor that accepts User and Team objects
        return new UserTeamMembership(user, team, role, status);
    }
}
