package com.taskforge.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ModelTests {

    @Test
    void testAttachment() {
        LocalDateTime now = LocalDateTime.now();
        Task task = new Task();
        task.setId(1);

        Attachment attachment1 = new Attachment(1, task, "file.txt", "/path/to/file", now);
        Attachment attachment2 = new Attachment(task, "file.txt", "/path/to/file", now);
        attachment2.setId(1);

        assertEquals(1, attachment1.getId());
        assertEquals(task, attachment1.getTask());
        assertEquals("file.txt", attachment1.getFileName());
        assertEquals("/path/to/file", attachment1.getFilePath());
        assertEquals(now, attachment1.getUploadedAt());

        assertEquals(attachment1, attachment2);
        assertEquals(attachment1.hashCode(), attachment2.hashCode());
        assertTrue(attachment1.toString().contains("file.txt"));
    }

    @Test
    void testComment() {
        LocalDateTime now = LocalDateTime.now();
        Task task = new Task();
        task.setId(1);
        User user = new User();
        user.setId(1);

        Comment comment1 = new Comment(1, task, user, "Test comment", now);
        Comment comment2 = new Comment(task, user, "Test comment", now);
        comment2.setId(1);

        assertEquals(1, comment1.getId());
        assertEquals(task, comment1.getTask());
        assertEquals(user, comment1.getAuthor());
        assertEquals("Test comment", comment1.getCommentText());
        assertEquals(now, comment1.getCreatedAt());

        assertEquals(comment1, comment2);
        assertEquals(comment1.hashCode(), comment2.hashCode());
        assertTrue(comment1.toString().contains("Test comment"));
    }

    @Test
    void testNotification() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(1);

        Notification notification1 = new Notification(1, user, "Test message", now, false, 123, Notification.NotificationType.TEAM_INVITATION);
        Notification notification2 = new Notification(user, "Test message", now, 123, Notification.NotificationType.TEAM_INVITATION);
        notification2.setId(1);

        assertEquals(1, notification1.getId());
        assertEquals(user, notification1.getRecipient());
        assertEquals("Test message", notification1.getMessage());
        assertEquals(now, notification1.getSentAt());
        assertFalse(notification1.isRead());
        assertEquals(123, notification1.getRelatedEntityId());
        assertEquals(Notification.NotificationType.TEAM_INVITATION, notification1.getNotificationType());

        assertEquals(notification1, notification2);
        assertEquals(notification1.hashCode(), notification2.hashCode());
        assertTrue(notification1.toString().contains("Test message"));
    }

    @Test
    void testPriorityEnum() {
        assertEquals("Low", Priority.LOW.toString());
        assertEquals("Medium", Priority.MEDIUM.toString());
        assertEquals("High", Priority.HIGH.toString());
    }

    @Test
    void testProject() {
        Team team = new Team();
        team.setId(1);

        Project project1 = new Project(1, "Test Project", team);
        Project project2 = new Project("Test Project", team);
        project2.setId(1);

        assertEquals(1, project1.getId());
        assertEquals("Test Project", project1.getName());
        assertEquals(team, project1.getTeam());

        assertEquals(project1, project2);
        assertEquals(project1.hashCode(), project2.hashCode());
        assertTrue(project1.toString().contains("Test Project"));
    }

    @Test
    void testStatusEnum() {
        assertEquals("Pending", Status.PENDING.toString());
        assertEquals("In progress", Status.IN_PROGRESS.toString());
        assertEquals("Completed", Status.COMPLETED.toString());
        assertEquals("Blocked", Status.BLOCKED.toString());
    }

    @Test
    void testTask() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(1);
        Project project = new Project();
        project.setId(1);

        Task task1 = new Task(1, "Test Task", "Description", now, Priority.HIGH, Status.PENDING, user, project, Visibility.PUBLIC, user);
        Task task2 = new Task("Test Task", "Description", now, Priority.HIGH, Status.PENDING, user, project, Visibility.PUBLIC, user);
        task2.setId(1);

        assertEquals(1, task1.getId());
        assertEquals("Test Task", task1.getTitle());
        assertEquals("Description", task1.getDescription());
        assertEquals(now, task1.getDueDate());
        assertEquals(Priority.HIGH, task1.getPriority());
        assertEquals(Status.PENDING, task1.getStatus());
        assertEquals(user, task1.getAssignedTo());
        assertEquals(project, task1.getProject());
        assertEquals(Visibility.PUBLIC, task1.getVisibility());
        assertEquals(user, task1.getCreator());

        assertEquals(task1, task2);
        assertEquals(task1.hashCode(), task2.hashCode());
        assertTrue(task1.toString().contains("Test Task"));
    }

    @Test
    void testTeam() {
        Team team1 = new Team(1, "Test Team");
        Team team2 = new Team("Test Team");
        team2.setId(1);

        assertEquals(1, team1.getId());
        assertEquals("Test Team", team1.getName());

        assertEquals(team1, team2);
        assertEquals(team1.hashCode(), team2.hashCode());
        assertTrue(team1.toString().contains("Test Team"));
    }

    @Test
    void testUser() {
        User user1 = new User(1, "testuser", "test@example.com", "hashedpassword");
        User user2 = new User("testuser", "test@example.com", "hashedpassword");
        user2.setId(1);

        assertEquals(1, user1.getId());
        assertEquals("testuser", user1.getUsername());
        assertEquals("test@example.com", user1.getEmail());
        assertEquals("hashedpassword", user1.getPasswordHash());

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertTrue(user1.toString().contains("testuser"));
        assertFalse(user1.toString().contains("hashedpassword")); // Password should not be in toString
    }

    @Test
    void testUserTeamMembership() {
        User user = new User();
        user.setId(1);
        Team team = new Team();
        team.setId(1);

        UserTeamMembership membership1 = new UserTeamMembership(user, team, UserTeamMembership.Role.MEMBER, UserTeamMembership.InvitationStatus.ACCEPTED);
        UserTeamMembership membership2 = new UserTeamMembership(1, 1, UserTeamMembership.Role.MEMBER, UserTeamMembership.InvitationStatus.ACCEPTED);

        assertEquals(user, membership1.getUser());
        assertEquals(team, membership1.getTeam());
        assertEquals(UserTeamMembership.Role.MEMBER, membership1.getRole());
        assertEquals(UserTeamMembership.InvitationStatus.ACCEPTED, membership1.getInvitationStatus());
        assertEquals(1, membership1.getUserId());
        assertEquals(1, membership1.getTeamId());

        // Can't directly compare membership1 and membership2 because membership2 has placeholder objects
        assertEquals(1, membership2.getUserId());
        assertEquals(1, membership2.getTeamId());
        assertTrue(membership1.toString().contains("MEMBER"));
    }

    @Test
    void testVisibilityEnum() {
        assertEquals("Public", Visibility.PUBLIC.toString());
        assertEquals("Restricted", Visibility.RESTRICTED.toString());
        assertEquals("Private", Visibility.PRIVATE.toString());
    }
}