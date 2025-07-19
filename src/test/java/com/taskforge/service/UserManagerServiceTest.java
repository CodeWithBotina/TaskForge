package com.taskforge.service;

import com.taskforge.dao.*;
import com.taskforge.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class UserManagerServiceTest {

    @Mock private UserDAO userDAO;
    @Mock private TeamDAO teamDAO;
    @Mock private UserTeamDAO userTeamDAO;
    @Mock private NotificationDAO notificationDAO;

    private UserManagerService userManagerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userManagerService = new UserManagerService(userDAO, teamDAO, userTeamDAO);
    }

    @Test
    void createTeam_Success() {
        // Arrange
        String teamName = "Test Team";
        int creatorId = 1;
        User creator = new User();
        creator.setId(creatorId);
        Team mockTeam = new Team(teamName);

        when(userDAO.getUserById(creatorId)).thenReturn(Optional.of(creator));
        when(teamDAO.getTeamByName(teamName)).thenReturn(Optional.empty());
        when(teamDAO.createTeam(any(Team.class))).thenReturn(mockTeam);
        when(userTeamDAO.createMembership(any(UserTeamMembership.class))).thenReturn(true);

        // Act
        Optional<Team> result = userManagerService.createTeam(teamName, creatorId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(teamName, result.get().getName());
    }

    @Test
    void inviteUserToTeam_Success() {
        // Arrange
        int userId = 1;
        int teamId = 1;
        User mockUser = new User("testuser", "test@test.com", "hash");
        mockUser.setId(userId);
        Team mockTeam = new Team("Test Team");
        mockTeam.setId(teamId);

        // Use reflection to inject our mock NotificationDAO
        try {
            Field notificationDAOField = UserManagerService.class.getDeclaredField("notificationDAO");
            notificationDAOField.setAccessible(true);
            notificationDAOField.set(userManagerService, notificationDAO);
        } catch (Exception e) {
            fail("Failed to inject mock NotificationDAO", e);
        }

        when(userDAO.getUserById(userId)).thenReturn(Optional.of(mockUser));
        when(teamDAO.getTeamById(teamId)).thenReturn(Optional.of(mockTeam));
        when(userTeamDAO.getMembership(userId, teamId)).thenReturn(Optional.empty());
        when(userTeamDAO.createMembership(any(UserTeamMembership.class))).thenReturn(true);

        // Act
        boolean result = userManagerService.inviteUserToTeam(
                userId, teamId, UserTeamMembership.Role.MEMBER
        );

        // Assert
        assertTrue(result);
        verify(notificationDAO, times(1)).createNotification(any(Notification.class));
    }

    @Test
    void areUsersInSameTeam_True() {
        // Arrange
        int userId1 = 1;
        int userId2 = 2;
        Team sharedTeam = new Team("Shared Team");
        sharedTeam.setId(1);

        UserTeamMembership membership1 = new UserTeamMembership(
                new User(), sharedTeam, UserTeamMembership.Role.MEMBER,
                UserTeamMembership.InvitationStatus.ACCEPTED
        );
        UserTeamMembership membership2 = new UserTeamMembership(
                new User(), sharedTeam, UserTeamMembership.Role.MEMBER,
                UserTeamMembership.InvitationStatus.ACCEPTED
        );

        when(userTeamDAO.getMembershipsByUserId(userId1))
                .thenReturn(Collections.singletonList(membership1));
        when(userTeamDAO.getMembershipsByUserId(userId2))
                .thenReturn(Collections.singletonList(membership2));

        // Act
        boolean result = userManagerService.areUsersInSameTeam(userId1, userId2);

        // Assert
        assertTrue(result);
    }

    @Test
    void updateUser_Success() {
        // Arrange
        int userId = 1;
        User existingUser = new User("old", "old@test.com", "hash");
        existingUser.setId(userId);

        when(userDAO.getUserById(userId)).thenReturn(Optional.of(existingUser));
        when(userDAO.getUserByUsername("new")).thenReturn(Optional.empty());
        when(userDAO.getUserByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userDAO.updateUser(any(User.class))).thenReturn(true);

        // Act
        boolean result = userManagerService.updateUser(userId, "new", "new@test.com");

        // Assert
        assertTrue(result);
        assertEquals("new", existingUser.getUsername());
    }
}