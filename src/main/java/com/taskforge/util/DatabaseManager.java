package com.taskforge.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the SQLite database connection and schema initialization for the TaskForge application.
 * This utility class provides static methods to establish a connection to the database
 * and to create all necessary tables if they do not already exist.
 * It ensures that the database structure is ready for data operations at application startup.
 */
public class DatabaseManager {

    /** The URL for the SQLite database. "jdbc:sqlite:" is the prefix for SQLite.
     * "taskforge.db" is the name of the database file that will be created in the project root.
     */
    private static final String URL = "jdbc:sqlite:taskforge.db";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DatabaseManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Establishes and returns a connection to the SQLite database.
     * This method should be called within a try-with-resources statement
     * to ensure the connection is properly closed.
     *
     * @return A {@link Connection} object to the database.
     * @throws SQLException If a database access error occurs (e.g., driver not found, invalid URL).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * Initializes the database schema by creating tables if they do not already exist.
     * This method should be called once at application startup to ensure that
     * the database structure is prepared for data operations.
     * It also enables foreign key support for SQLite.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Enable foreign key support for SQLite. This must be done for each connection.
            stmt.execute("PRAGMA foreign_keys = ON;");

            // SQL to create the Users table
            String createUsersTable = "CREATE TABLE IF NOT EXISTS Users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL UNIQUE," +
                    "email TEXT NOT NULL UNIQUE," +
                    "password_hash TEXT NOT NULL" +
                    ");";
            stmt.execute(createUsersTable);
            System.out.println("Users table checked/created.");

            // SQL to create the Teams table
            String createTeamsTable = "CREATE TABLE IF NOT EXISTS Teams (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL UNIQUE" +
                    ");";
            stmt.execute(createTeamsTable);
            System.out.println("Teams table checked/created.");

            // SQL to create the UserTeamMemberships table, defining the many-to-many relationship
            String createUserTeamMembershipsTable = "CREATE TABLE IF NOT EXISTS UserTeamMemberships (" +
                    "user_id INTEGER NOT NULL," +
                    "team_id INTEGER NOT NULL," +
                    "role TEXT NOT NULL," + // e.g., 'MEMBER', 'OWNER'
                    "invitation_status TEXT NOT NULL," + // e.g., 'PENDING', 'ACCEPTED', 'REJECTED'
                    "PRIMARY KEY (user_id, team_id)," + // Composite primary key
                    "FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (team_id) REFERENCES Teams(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createUserTeamMembershipsTable);
            System.out.println("UserTeamMemberships table checked/created.");

            // SQL to create the Projects table
            String createProjectsTable = "CREATE TABLE IF NOT EXISTS Projects (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "team_id INTEGER," + // Optional foreign key to Teams
                    "FOREIGN KEY (team_id) REFERENCES Teams(id) ON DELETE SET NULL" + // If team is deleted, set project's team_id to NULL
                    ");";
            stmt.execute(createProjectsTable);
            System.out.println("Projects table checked/created.");

            // SQL to create the Tasks table
            // Includes foreign keys to Users (for assigned_to and creator) and Projects
            String createTasksTable = "CREATE TABLE IF NOT EXISTS Tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "description TEXT," +
                    "due_date TEXT," + // Stored as ISO 8601 string (YYYY-MM-DDTHH:MM:SS)
                    "priority TEXT NOT NULL," + // e.g., 'LOW', 'MEDIUM', 'HIGH'
                    "status TEXT NOT NULL," +   // e.g., 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'BLOCKED'
                    "assigned_to_user_id INTEGER," + // Optional foreign key to Users
                    "project_id INTEGER," +          // Optional foreign key to Projects
                    "visibility TEXT NOT NULL," +    // 'PUBLIC', 'RESTRICTED', 'PRIVATE'
                    "creator_id INTEGER NOT NULL," + // User who created the task
                    "FOREIGN KEY (assigned_to_user_id) REFERENCES Users(id) ON DELETE SET NULL," +
                    "FOREIGN KEY (project_id) REFERENCES Projects(id) ON DELETE SET NULL," +
                    "FOREIGN KEY (creator_id) REFERENCES Users(id) ON DELETE CASCADE" + // If creator is deleted, their tasks are also deleted
                    ");";
            stmt.execute(createTasksTable);
            System.out.println("Tasks table checked/created.");

            // SQL to create the Comments table
            // Includes foreign keys to Tasks and Users (for author)
            String createCommentsTable = "CREATE TABLE IF NOT EXISTS Comments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "task_id INTEGER NOT NULL," +
                    "author_id INTEGER NOT NULL," +
                    "comment_text TEXT NOT NULL," +
                    "created_at TEXT NOT NULL," + // Stored as ISO 8601 string
                    "FOREIGN KEY (task_id) REFERENCES Tasks(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (author_id) REFERENCES Users(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createCommentsTable);
            System.out.println("Comments table checked/created.");

            // SQL to create the Attachments table
            // Includes a foreign key to Tasks
            String createAttachmentsTable = "CREATE TABLE IF NOT EXISTS Attachments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "task_id INTEGER NOT NULL," +
                    "file_name TEXT NOT NULL," +
                    "file_path TEXT NOT NULL," + // Path where the file is stored (e.g., relative path on disk)
                    "uploaded_at TEXT NOT NULL," + // Stored as ISO 8601 string
                    "FOREIGN KEY (task_id) REFERENCES Tasks(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createAttachmentsTable);
            System.out.println("Attachments table checked/created.");

            // SQL to create the Notifications table
            // Includes foreign key to Users (for recipient) and new columns for context
            String createNotificationsTable = "CREATE TABLE IF NOT EXISTS Notifications (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER NOT NULL," + // Recipient of the notification
                    "message TEXT NOT NULL," +
                    "sent_at TEXT NOT NULL," + // Stored as ISO 8601 string
                    "is_read BOOLEAN NOT NULL DEFAULT 0," + // 0 for unread, 1 for read
                    "related_entity_id INTEGER NOT NULL DEFAULT 0," + // ID of related entity (e.g., Team ID for invitation)
                    "notification_type TEXT NOT NULL DEFAULT 'GENERAL'," + // Type of notification (e.g., TEAM_INVITATION)
                    "FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createNotificationsTable);
            System.out.println("Notifications table checked/created.");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
