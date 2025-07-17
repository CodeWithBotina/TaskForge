package com.taskforge.model;

import java.util.Objects;

/**
 * Represents the many-to-many relationship between a User and a Team,
 * including the user's role within that team and their invitation status.
 * This class encapsulates membership properties such as user, team, role, and invitation status.
 */
public class UserTeamMembership {

    private User user;
    private Team team;
    private Role role;
    private InvitationStatus invitationStatus;

    /**
     * Enum for the role of a user within a team.
     */
    public enum Role {
        MEMBER, // Standard team member
        OWNER;  // Team owner with administrative privileges
    }

    /**
     * Enum for the invitation status of a user to a team.
     */
    public enum InvitationStatus {
        PENDING,  // Invitation sent, awaiting user's response
        ACCEPTED, // User has accepted the invitation and is an active member
        REJECTED; // User has rejected the invitation
    }

    /**
     * Default constructor for UserTeamMembership.
     * Useful for frameworks that require a no-argument constructor.
     */
    public UserTeamMembership() {
        // Default constructor
    }

    /**
     * Constructs a new UserTeamMembership with user and team IDs, role, and invitation status.
     * This constructor is primarily used when retrieving data from the database where only IDs are available.
     *
     * @param userId The ID of the user.
     * @param teamId The ID of the team.
     * @param role The role of the user in the team.
     * @param invitationStatus The invitation status.
     */
    public UserTeamMembership(int userId, int teamId, Role role, InvitationStatus invitationStatus) {
        // Note: User and Team objects will need to be fetched and set separately if needed
        // This constructor is mainly for DAO internal use when only IDs are present initially
        this.user = new User(); // Placeholder, ID will be set
        this.user.setId(userId);
        this.team = new Team(); // Placeholder, ID will be set
        this.team.setId(teamId);
        this.role = role;
        this.invitationStatus = invitationStatus;
    }

    /**
     * Constructs a new UserTeamMembership with User and Team objects, role, and invitation status.
     * This constructor is preferred when full User and Team objects are available.
     *
     * @param user The User object.
     * @param team The Team object.
     * @param role The role of the user in the team.
     * @param invitationStatus The invitation status.
     */
    public UserTeamMembership(User user, Team team, Role role, InvitationStatus invitationStatus) {
        this.user = user;
        this.team = team;
        this.role = role;
        this.invitationStatus = invitationStatus;
    }

    // --- Getters and Setters ---

    /**
     * Gets the user associated with this membership.
     * @return The User object.
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user associated with this membership.
     * @param user The User object.
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the ID of the user associated with this membership.
     * This is a convenience method to directly access the user's ID.
     * @return The ID of the user.
     */
    public int getUserId() {
        return user != null ? user.getId() : 0; // Return 0 or throw an exception if user is null
    }

    /**
     * Gets the team associated with this membership.
     * @return The Team object.
     */
    public Team getTeam() {
        return team;
    }

    /**
     * Sets the team associated with this membership.
     * @param team The Team object.
     */
    public void setTeam(Team team) {
        this.team = team;
    }

    /**
     * Gets the ID of the team associated with this membership.
     * This is a convenience method to directly access the team's ID.
     * @return The ID of the team.
     */
    public int getTeamId() {
        return team != null ? team.getId() : 0; // Return 0 or throw an exception if team is null
    }

    /**
     * Gets the role of the user within the team.
     * @return The Role enum.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Sets the role of the user within the team.
     * @param role The Role enum.
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Gets the invitation status of the user for the team.
     * @return The InvitationStatus enum.
     */
    public InvitationStatus getInvitationStatus() {
        return invitationStatus;
    }

    /**
     * Sets the invitation status of the user for the team.
     * @param invitationStatus The InvitationStatus enum.
     */
    public void setInvitationStatus(InvitationStatus invitationStatus) {
        this.invitationStatus = invitationStatus;
    }

    // --- Object Overrides for Equality and Hashing ---

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two UserTeamMembership objects are considered equal if both their
     * user and team objects are equal.
     *
     * @param o The reference object with which to compare.
     * @return true if this object is the same as the obj argument; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTeamMembership that = (UserTeamMembership) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(team, that.team);
    }

    /**
     * Returns a hash code value for the object.
     * Consistent with the equals method, the hash code is based on the user and team.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(user, team);
    }

    /**
     * Returns a string representation of the UserTeamMembership object.
     * Useful for logging and debugging.
     *
     * @return A string representation of the object.
     */
    @Override
    public String toString() {
        return "UserTeamMembership{" +
                "user=" + (user != null ? user.getUsername() : "null") +
                ", team=" + (team != null ? team.getName() : "null") +
                ", role=" + role +
                ", invitationStatus=" + invitationStatus +
                '}';
    }
}
