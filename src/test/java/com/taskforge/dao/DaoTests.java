package com.taskforge.dao;

import com.taskforge.model.*;
import com.taskforge.util.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DaoTests {

    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private Statement mockStatement;
    private ResultSet mockResultSet;
    private MockedStatic<DatabaseManager> mockedDatabaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockStatement = mock(Statement.class);
        mockResultSet = mock(ResultSet.class);

        // Mock static DatabaseManager for the entire test class
        mockedDatabaseManager = Mockito.mockStatic(DatabaseManager.class);
        mockedDatabaseManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
    }

    @AfterEach
    void tearDown() {
        mockedDatabaseManager.close();
    }

    @Test
    void testAttachmentDAO() throws SQLException {
        TaskDAO mockTaskDAO = mock(TaskDAO.class);
        AttachmentDAO attachmentDAO = new AttachmentDAO(mockTaskDAO);

        // Setup mock result set for create
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        Task task = new Task();
        task.setId(1);
        Attachment attachment = new Attachment(task, "test.txt", "/path", LocalDateTime.now());
        Attachment created = attachmentDAO.createAttachment(attachment);
        assertNotNull(created);
        assertEquals(1, created.getId());

        // Test getAttachmentById
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getInt("task_id")).thenReturn(1);
        when(mockResultSet.getString("file_name")).thenReturn("test.txt");
        when(mockResultSet.getString("file_path")).thenReturn("/path");
        when(mockResultSet.getString("uploaded_at")).thenReturn("2023-01-01T12:00:00");
        when(mockTaskDAO.getTaskById(1)).thenReturn(Optional.of(task));

        Optional<Attachment> found = attachmentDAO.getAttachmentById(1);
        assertTrue(found.isPresent());
        assertEquals("test.txt", found.get().getFileName());
    }

    @Test
    void testCommentDAO() throws SQLException {
        TaskDAO mockTaskDAO = mock(TaskDAO.class);
        UserDAO mockUserDAO = mock(UserDAO.class);
        CommentDAO commentDAO = new CommentDAO(mockTaskDAO, mockUserDAO);

        // Setup mock result set for create
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        Task task = new Task();
        task.setId(1);
        User user = new User();
        user.setId(1);
        Comment comment = new Comment(task, user, "Test comment", LocalDateTime.now());
        Comment created = commentDAO.createComment(comment);
        assertNotNull(created);
        assertEquals(1, created.getId());

        // Test getCommentById
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getInt("task_id")).thenReturn(1);
        when(mockResultSet.getInt("author_id")).thenReturn(1);
        when(mockResultSet.getString("comment_text")).thenReturn("Test comment");
        when(mockResultSet.getString("created_at")).thenReturn("2023-01-01T12:00:00");
        when(mockTaskDAO.getTaskById(1)).thenReturn(Optional.of(task));
        when(mockUserDAO.getUserById(1)).thenReturn(Optional.of(user));

        Optional<Comment> found = commentDAO.getCommentById(1);
        assertTrue(found.isPresent());
        assertEquals("Test comment", found.get().getCommentText());
    }

    @Test
    void testNotificationDAO() throws SQLException {
        UserDAO mockUserDAO = mock(UserDAO.class);
        NotificationDAO notificationDAO = new NotificationDAO(mockUserDAO);

        // Setup mock result set for create
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        User user = new User();
        user.setId(1);
        Notification notification = new Notification(user, "Test message", LocalDateTime.now(),
                123, Notification.NotificationType.TEAM_INVITATION);
        Notification created = notificationDAO.createNotification(notification);
        assertNotNull(created);
        assertEquals(1, created.getId());

        // Test getNotificationById
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getInt("user_id")).thenReturn(1);
        when(mockResultSet.getString("message")).thenReturn("Test message");
        when(mockResultSet.getString("sent_at")).thenReturn("2023-01-01T12:00:00");
        when(mockResultSet.getBoolean("is_read")).thenReturn(false);
        when(mockResultSet.getInt("related_entity_id")).thenReturn(123);
        when(mockResultSet.getString("notification_type")).thenReturn("TEAM_INVITATION");
        when(mockUserDAO.getUserById(1)).thenReturn(Optional.of(user));

        Optional<Notification> found = notificationDAO.getNotificationById(1);
        assertTrue(found.isPresent());
        assertEquals("Test message", found.get().getMessage());
        assertEquals(Notification.NotificationType.TEAM_INVITATION, found.get().getNotificationType());
    }

    @Test
    void testProjectDAO() throws SQLException {
        TeamDAO mockTeamDAO = mock(TeamDAO.class);
        ProjectDAO projectDAO = new ProjectDAO(mockTeamDAO);

        // Setup mock result set for create
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        Project project = new Project("Test Project", null);
        Project created = projectDAO.createProject(project);
        assertNotNull(created);
        assertEquals(1, created.getId());

        // Test getProjectById with team
        Team team = new Team();
        team.setId(1);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("name")).thenReturn("Test Project");
        when(mockResultSet.getInt("team_id")).thenReturn(1);
        when(mockResultSet.wasNull()).thenReturn(false);
        when(mockTeamDAO.getTeamById(1)).thenReturn(Optional.of(team));

        Optional<Project> found = projectDAO.getProjectById(1);
        assertTrue(found.isPresent());
        assertEquals("Test Project", found.get().getName());
        assertEquals(team, found.get().getTeam());
    }

    @Test
    void testTaskDAO() throws SQLException {
        UserDAO mockUserDAO = mock(UserDAO.class);
        ProjectDAO mockProjectDAO = mock(ProjectDAO.class);
        TaskDAO taskDAO = new TaskDAO(mockUserDAO, mockProjectDAO);

        // Setup mock result set for create
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        User creator = new User();
        creator.setId(1);
        Task task = new Task("Test Task", "Description", LocalDateTime.now(),
                Priority.HIGH, Status.PENDING, null, null, Visibility.PUBLIC, creator);
        Task created = taskDAO.createTask(task);
        assertNotNull(created);
        assertEquals(1, created.getId());

        // Test getTaskById
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("title")).thenReturn("Test Task");
        when(mockResultSet.getString("description")).thenReturn("Description");
        when(mockResultSet.getString("due_date")).thenReturn("2023-01-01T12:00:00");
        when(mockResultSet.getString("priority")).thenReturn("HIGH");
        when(mockResultSet.getString("status")).thenReturn("PENDING");
        when(mockResultSet.getInt("assigned_to_user_id")).thenReturn(0);
        when(mockResultSet.wasNull()).thenReturn(true);
        when(mockResultSet.getInt("project_id")).thenReturn(0);
        when(mockResultSet.getString("visibility")).thenReturn("PUBLIC");
        when(mockResultSet.getInt("creator_id")).thenReturn(1);
        when(mockUserDAO.getUserById(1)).thenReturn(Optional.of(creator));

        Optional<Task> found = taskDAO.getTaskById(1);
        assertTrue(found.isPresent());
        assertEquals("Test Task", found.get().getTitle());
        assertEquals(Visibility.PUBLIC, found.get().getVisibility());
    }

    @Test
    void testTeamDAO() throws SQLException {
        TeamDAO teamDAO = new TeamDAO();

        // Setup mock result set for create
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        Team team = new Team("Test Team");
        Team created = teamDAO.createTeam(team);
        assertNotNull(created);
        assertEquals(1, created.getId());

        // Test getTeamById
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("name")).thenReturn("Test Team");

        Optional<Team> found = teamDAO.getTeamById(1);
        assertTrue(found.isPresent());
        assertEquals("Test Team", found.get().getName());
    }

    @Test
    void testUserDAO() throws SQLException {
        UserDAO userDAO = new UserDAO();

        // Setup mock result set for create
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        User user = new User("testuser", "test@example.com", "hashedpassword");
        User created = userDAO.createUser(user);
        assertNotNull(created);
        assertEquals(1, created.getId());

        // Test getUserById
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("username")).thenReturn("testuser");
        when(mockResultSet.getString("email")).thenReturn("test@example.com");
        when(mockResultSet.getString("password_hash")).thenReturn("hashedpassword");

        Optional<User> found = userDAO.getUserById(1);
        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void testUserTeamDAO() throws SQLException {
        UserDAO mockUserDAO = mock(UserDAO.class);
        TeamDAO mockTeamDAO = mock(TeamDAO.class);  // Fixed: Changed from TaskDAO to TeamDAO
        UserTeamDAO userTeamDAO = new UserTeamDAO(mockUserDAO, mockTeamDAO);

        // Setup mock result set for create
        when(mockResultSet.next()).thenReturn(true);

        User user = new User();
        user.setId(1);
        Team team = new Team();
        team.setId(1);
        UserTeamMembership membership = new UserTeamMembership(user, team,
                UserTeamMembership.Role.MEMBER, UserTeamMembership.InvitationStatus.ACCEPTED);
        boolean created = userTeamDAO.createMembership(membership);
        assertTrue(created);

        // Test getMembership
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("user_id")).thenReturn(1);
        when(mockResultSet.getInt("team_id")).thenReturn(1);
        when(mockResultSet.getString("role")).thenReturn("MEMBER");
        when(mockResultSet.getString("invitation_status")).thenReturn("ACCEPTED");
        when(mockUserDAO.getUserById(1)).thenReturn(Optional.of(user));
        when(mockTeamDAO.getTeamById(1)).thenReturn(Optional.of(team));

        Optional<UserTeamMembership> found = userTeamDAO.getMembership(1, 1);
        assertTrue(found.isPresent());
        assertEquals(UserTeamMembership.Role.MEMBER, found.get().getRole());
        assertEquals(UserTeamMembership.InvitationStatus.ACCEPTED, found.get().getInvitationStatus());
    }

    @Test
    void testProjectDAONullTeam() throws SQLException {
        TeamDAO mockTeamDAO = mock(TeamDAO.class);
        ProjectDAO projectDAO = new ProjectDAO(mockTeamDAO);

        // Test project with null team
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("name")).thenReturn("Test Project");
        when(mockResultSet.getInt("team_id")).thenReturn(0);
        when(mockResultSet.wasNull()).thenReturn(true);

        Optional<Project> found = projectDAO.getProjectById(1);
        assertTrue(found.isPresent());
        assertNull(found.get().getTeam());
    }

    @Test
    void testTaskDAONullFields() throws SQLException {
        UserDAO mockUserDAO = mock(UserDAO.class);
        ProjectDAO mockProjectDAO = mock(ProjectDAO.class);
        TaskDAO taskDAO = new TaskDAO(mockUserDAO, mockProjectDAO);

        User creator = new User();
        creator.setId(1);

        // Test task with null assignedTo and project
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("title")).thenReturn("Test Task");
        when(mockResultSet.getString("description")).thenReturn("Description");
        when(mockResultSet.getString("due_date")).thenReturn(null);
        when(mockResultSet.getString("priority")).thenReturn("HIGH");
        when(mockResultSet.getString("status")).thenReturn("PENDING");
        when(mockResultSet.getInt("assigned_to_user_id")).thenReturn(0);
        when(mockResultSet.wasNull()).thenReturn(true);
        when(mockResultSet.getInt("project_id")).thenReturn(0);
        when(mockResultSet.getString("visibility")).thenReturn("PUBLIC");
        when(mockResultSet.getInt("creator_id")).thenReturn(1);
        when(mockUserDAO.getUserById(1)).thenReturn(Optional.of(creator));

        Optional<Task> found = taskDAO.getTaskById(1);
        assertTrue(found.isPresent());
        assertNull(found.get().getAssignedTo());
        assertNull(found.get().getProject());
        assertNull(found.get().getDueDate());
    }

    @Test
    void testAttachmentDAOErrorHandling() throws SQLException {
        TaskDAO mockTaskDAO = mock(TaskDAO.class);
        AttachmentDAO attachmentDAO = new AttachmentDAO(mockTaskDAO);

        // Test SQLException during create
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));
        Attachment attachment = new Attachment(new Task(), "test.txt", "/path", LocalDateTime.now());
        assertNull(attachmentDAO.createAttachment(attachment));

        // Test empty result set
        when(mockResultSet.next()).thenReturn(false);
        assertFalse(attachmentDAO.getAttachmentById(1).isPresent());
    }
}