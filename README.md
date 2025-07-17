# TaskForge

TaskForge is a desktop task management application built with JavaFX and SQLite. It allows users to manage personal tasks, collaborate within teams on projects, and receive notifications for important events like team invitations and task assignments.

## Table of Contents
1. [Class Diagram](#class-diagram)
2. [Flowchart](#flowchart)
3. [User Manual](#user-manual)
   - [1. Getting Started](#1-getting-started)
   - [2. Login and Registration](#2-login-and-registration)
   - [3. Dashboard Overview](#3-dashboard-overview)
   - [4. Managing My Tasks](#4-managing-my-tasks)
   - [5. Viewing All Tasks](#5-viewing-all-tasks)
   - [6. Project Management](#6-project-management)
   - [7. Team Management](#7-team-management)
   - [8. User Management](#8-user-management)
   - [9. Notifications](#9-notifications)
   - [10. Logout](#10-logout)
4. [Requirements](#requirements)
   - [Functional Requirements](#functional-requirements)
   - [Non-Functional Requirements](#non-functional-requirements)

## Class Diagram
The following diagram illustrates the core classes and their relationships within the TaskForge application.

```mermaid
classDiagram
    direction LR
    class User {
        +int id
        +String username
        +String email
        +String passwordHash
        +createUser()
        +getUserById()
        +getUserByUsername()
        +getUserByEmail()
        +getAllUsers()
        +updateUser()
        +deleteUser()
    }

    class Team {
        +int id
        +String name
        +createTeam()
        +getTeamById()
        +getTeamByName()
        +getAllTeams()
        +updateTeam()
        +deleteTeam()
    }

    class Project {
        +int id
        +String name
        +Team team
        +createProject()
        +getProjectById()
        +getProjectsByName()
        +getProjectsByTeamId()
        +getAllProjects()
        +updateProject()
        +deleteProject()
    }

    class Task {
        +int id
        +String title
        +String description
        +LocalDateTime dueDate
        +Priority priority
        +Status status
        +User assignedTo
        +Project project
        +Visibility visibility
        +User creator
        +createTask()
        +getTaskById()
        +getAllTasks()
        +getTasksByAssignedUserId()
        +getTasksByProjectId()
        +updateTask()
        +deleteTask()
    }

    class Comment {
        +int id
        +Task task
        +User author
        +String commentText
        +LocalDateTime createdAt
        +createComment()
        +getCommentById()
        +getCommentsByTaskId()
        +getAllComments()
        +updateComment()
        +deleteComment()
    }

    class Attachment {
        +int id
        +Task task
        +String fileName
        +String filePath
        +LocalDateTime uploadedAt
        +createAttachment()
        +getAttachmentById()
        +getAttachmentsByTaskId()
        +getAllAttachments()
        +updateAttachment()
        +deleteAttachment()
    }

    class Notification {
        +int id
        +User recipient
        +String message
        +LocalDateTime sentAt
        +boolean isRead
        +int relatedEntityId
        +NotificationType notificationType
        +createNotification()
        +getNotificationById()
        +getNotificationsByUserId()
        +getUnreadNotificationsByUserId()
        +getAllNotifications()
        +updateNotification()
        +markNotificationAsRead()
        +deleteNotification()
    }

    class UserTeamMembership {
        +User user
        +Team team
        +Role role
        +InvitationStatus invitationStatus
        +createMembership()
        +getMembership()
        +getMembershipsByUserId()
        +getMembershipsByTeamId()
        +updateMembership()
        +deleteMembership()
    }

    class AuthService {
        -UserDAO userDAO
        +registerUser()
        +authenticateUser()
    }

    class UserManagerService {
        -UserDAO userDAO
        -TeamDAO teamDAO
        -UserTeamDAO userTeamDAO
        -NotificationDAO notificationDAO
        +registerUser()
        +authenticateUser()
        +updateUser()
        +deleteUser()
        +getUserById()
        +getAllUsers()
        +createTeam()
        +updateTeam()
        +deleteTeam()
        +getTeamById()
        +getAllTeams()
        +getTeamsForUser()
        +isTeamOwner()
        +areUsersInSameTeam()
        +isUserMemberOfTeam()
        +getTeamMemberships()
        +inviteUserToTeam()
        +acceptTeamInvitation()
        +rejectTeamInvitation()
        +removeUserFromTeam()
        +updateTeamMemberRole()
        +getMembership()
        +getUsersInTeam()
    }

    class ProjectManagerService {
        -ProjectDAO projectDAO
        -TeamDAO teamDAO
        +createProject()
        +getProjectById()
        +getAllProjects()
        +getProjectsByTeam()
        +updateProject()
        +deleteProject()
    }

    class TaskManagerService {
        -TaskDAO taskDAO
        -UserDAO userDAO
        -ProjectDAO projectDAO
        -NotificationDAO notificationDAO
        -UserTeamDAO userTeamDAO
        -UserManagerService userManagerService
        +createTask()
        +getTaskById()
        +getTasksByAssignedUser()
        +getAllVisibleTasks()
        +updateTask()
        +deleteTask()
    }

    class DatabaseManager {
        -URL
        +getConnection()
        +initializeDatabase()
    }

    class SecurityUtil {
        +hashPassword()
        +checkPassword()
    }

    User "1" -- "*" UserTeamMembership : has
    Team "1" -- "*" UserTeamMembership : has
    UserTeamMembership "many" -- "1" User : belongs to
    UserTeamMembership "many" -- "1" Team : belongs to

    Team "1" -- "0..*" Project : has
    Project "1" -- "0..*" Task : contains
    User "1" -- "0..*" Task : assigns/creates
    Task "1" -- "0..*" Comment : has
    Task "1" -- "0..*" Attachment : has
    User "1" -- "0..*" Notification : receives

    AuthService ..> UserDAO
    UserManagerService ..> UserDAO
    UserManagerService ..> TeamDAO
    UserManagerService ..> UserTeamDAO
    UserManagerService ..> NotificationDAO
    ProjectManagerService ..> ProjectDAO
    ProjectManagerService ..> TeamDAO
    TaskManagerService ..> TaskDAO
    TaskManagerService ..> UserDAO
    TaskManagerService ..> ProjectDAO
    TaskManagerService ..> NotificationDAO
    TaskManagerService ..> UserTeamDAO
    TaskManagerService ..> UserManagerService

    UserDAO ..> DatabaseManager
    TeamDAO ..> DatabaseManager
    ProjectDAO ..> DatabaseManager
    TaskDAO ..> DatabaseManager
    CommentDAO ..> DatabaseManager
    AttachmentDAO ..> DatabaseManager
    NotificationDAO ..> DatabaseManager
    UserTeamDAO ..> DatabaseManager

    AuthService ..> SecurityUtil
```

## Flowchart
Main Application Flow

```mermaid
graph TD
    A[Start Application] --> B{Database Initialized?};
    B -- No --> C[Create Tables];
    B -- Yes --> D[Load LoginView.fxml];
    C --> D;
    D --> E{User Action};
    E -- Login --> F[Authenticate User];
    E -- Register --> G[Load RegisterView.fxml];
    F -- Success --> H[Load DashboardView.fxml];
    F -- Failure --> D;
    G -- Register User --> I[Create User Record];
    I -- Success --> D;
    I -- Failure --> G;
    H --> J[Dashboard Menu];
    J -- Select My Tasks --> K[Load MyTasksView.fxml];
    J -- Select All Tasks --> L[Load AllTasksView.fxml];
    J -- Select Projects --> M[Load ProjectsView.fxml];
    J -- Select Teams --> N[Load TeamsView.fxml];
    J -- Select Users --> O[Load UsersView.fxml];
    J -- Select Notifications --> P[Load NotificationsView.fxml];
    J -- Logout --> D;

    K --> K_Refresh[Refresh My Tasks];
    L --> L_Refresh[Refresh All Tasks];
    M --> M_Refresh[Refresh Projects];
    N --> N_Refresh[Refresh Teams];
    O --> O_Refresh[Refresh Users];
    P --> P_Refresh[Refresh Notifications];

    subgraph Task Management
        K_Refresh --> K_Display[Display Tasks];
        K_Display -- Add Task --> K_Add[Create Task Form];
        K_Add --> K_Refresh;
        K_Display -- Edit Task --> K_Edit[Open EditTask Dialog];
        K_Edit --> K_Refresh;
        K_Display -- Back to Dashboard --> J;

        L_Refresh --> L_Display[Display All Visible Tasks];
        L_Display -- Back to Dashboard --> J;
    end

    subgraph Project Management
        M_Refresh --> M_Display[Display Projects];
        M_Display -- Add Project --> M_Add[Create Project Form];
        M_Add --> M_Refresh;
        M_Display -- Edit Project --> M_Edit[Open EditProject Dialog];
        M_Edit --> M_Refresh;
        M_Display -- Delete Project --> M_Delete[Confirm & Delete Project];
        M_Delete --> M_Refresh;
        M_Display -- Back to Dashboard --> J;
    end

    subgraph Team Management
        N_Refresh --> N_Display[Display Teams];
        N_Display -- Add Team --> N_Add[Create Team Form];
        N_Add --> N_Refresh;
        N_Display -- Edit Team --> N_Edit[Open EditTeam Dialog];
        N_Edit --> N_Refresh;
        N_Display -- Manage Members --> N_Manage[Open ManageTeamMembers Dialog];
        N_Manage -- Invite Member --> N_Invite[Send Invitation];
        N_Invite --> N_Manage;
        N_Manage -- Remove Member --> N_Remove[Confirm & Remove Member];
        N_Remove --> N_Manage;
        N_Display -- Back to Dashboard --> J;
    end

    subgraph User Management
        O_Refresh --> O_Display[Display Users];
        O_Display -- View User Details --> O_Detail[Open UserDetail Dialog];
        O_Detail --> O_Display;
        O_Display -- Back to Dashboard --> J;
    end

    subgraph Notification Management
        P_Refresh --> P_Display[Display Notifications];
        P_Display -- View Notification --> P_Detail[Open NotificationDetail Dialog];
        P_Detail -- Accept Team Invite --> P_Accept[Update Membership Status];
        P_Accept --> P_Detail;
        P_Detail -- Reject Team Invite --> P_Reject[Delete Membership];
        P_Reject --> P_Detail;
        P_Detail -- Mark as Read --> P_Read[Update Notification Status];
        P_Read --> P_Detail;
        P_Detail -- Delete Notification --> P_Delete[Delete Notification Record];
        P_Delete --> P_Display;
        P_Display -- Back to Dashboard --> J;
    end
```

## User Manual
This manual will guide you through the TaskForge application.

### 1. Getting Started
To run TaskForge, you need:
- Java Development Kit (JDK) 21 or later
- Apache Maven (for building and running)

**Steps to Run:**
1. Delete the existing database (if any):
   ```bash
   del taskforge.db
   ```
   (On Linux/macOS: `rm taskforge.db`)
2. Build the project and install dependencies:
   ```bash
   mvn clean install
   ```
3. Run the application:
   ```bash
   mvn javafx:run
   ```

### 2. Login and Registration
**Registration:**
1. On the initial screen, click "Go to Register"
2. Fill in your desired Username, Email, and Password
3. Confirm your password
4. Click "Register". If successful, you will see a confirmation message
5. Click "Back to Login" to return to the login screen

**Login:**
1. Enter your registered Username and Password
2. Click "Login". If successful, you will be directed to the Dashboard

### 3. Dashboard Overview
The Dashboard is the main hub of the application:
- Left navigation menu with sections: "My Tasks", "All Tasks", "Projects", "Teams", "Users", and "Notifications"
- Your logged-in username is displayed at the top right
- Click "Logout" to return to the Login screen

### 4. Managing My Tasks
This section displays tasks specifically assigned to you.

**Create New Task:**
1. Fill in the "Title", "Description" (optional), "Due Date" (optional), "Priority", and "Visibility" fields
2. Click "Add Task". The task will be assigned to you as the creator

**Refresh Tasks:** Click "Refresh Tasks" to update the list

**Edit Task:**
1. Select a task from the table
2. Click the "Edit" button
3. Modify task details in the dialog
4. Click "Save Changes" or "Cancel"

**Delete Task:**
1. Select a task from the table
2. Click the "Delete" button
3. The task will be permanently removed (Only the creator can delete)

### 5. Viewing All Tasks
This section displays all tasks in the system that are visible to you based on their visibility settings (Public, Restricted, Private).

**Refresh All Tasks:** Click "Refresh All Tasks" to update the list

### 6. Project Management
This section allows you to create and manage projects.

**Create New Project:**
1. Enter a "Project Name"
2. Optionally select a "Team"
3. Click "Add Project"

**Refresh Projects:** Click "Refresh Projects" to update the list

**Edit Project:**
1. Select a project from the table
2. Click the "Edit" button
3. Modify details in the dialog
4. Click "Save Changes" or "Cancel"

**Delete Project:**
1. Select a project from the table
2. Click the "Delete" button
3. The project will be removed

### 7. Team Management
This section allows you to create and manage teams, and invite users.

**Create New Team:**
1. Enter a "Team Name"
2. Click "Add Team". Creator becomes owner

**Refresh Teams:** Click "Refresh Teams" to update the list

**Edit Team:**
1. Select a team from the table
2. Click the "Edit" button
3. Change the team's name
4. Click "Save Changes" or "Cancel"

**Delete Team:**
1. Select a team from the table
2. Click the "Delete" button
3. The team will be removed

**Manage Members:**
1. Select a team
2. Click "Manage Members"
3. In the dialog:
    - Invite Member: Select user and role, click "Invite Member"
    - Remove Selected Member: Select member, click remove
4. Click "Close" when done

### 8. User Management
This section allows you to view all registered users.

**Refresh Users:** Click "Refresh Users" to update the list

### 9. Notifications
This section displays all notifications sent to your account.

**Refresh Notifications:** Click "Refresh Notifications" to update

**Notification Actions:**
- Team Invitation: Click "Accept" or "Reject"
- Mark as Read: Click "Mark as Read"
- Delete: Click "Delete" to remove

### 10. Logout
Click the "Logout" button in the top right corner to return to the Login screen.

## Requirements

### Functional Requirements

**User Management:**
- FR1.1: Users shall be able to register with a unique username and email
- FR1.2: Users shall be able to log in with their registered credentials
- FR1.3: Users shall be able to view a list of all registered users
- FR1.4: Users shall be able to update their own profile information
- FR1.5: Users shall be able to change their password
- FR1.6: Users shall be able to delete their own account

**Task Management:**
- FR2.1: Users shall be able to create new tasks with title, description, due date, priority, and visibility
- FR2.2: Tasks shall have status (Pending, In Progress, Completed, Blocked)
- FR2.3: Tasks can be assigned to specific users
- FR2.4: Tasks can be associated with projects
- FR2.5: Users shall be able to view tasks assigned to them ("My Tasks")
- FR2.6: Users shall be able to view all visible tasks ("All Tasks")
    - FR2.6.1: Public tasks visible to all
    - FR2.6.2: Restricted tasks visible to creator and team members
    - FR2.6.3: Private tasks visible only to creator

[Rest of the functional requirements...]

### Non-Functional Requirements

**Performance:**
- NFR1.1: The application should respond within 1-2 seconds
- NFR1.2: Database operations should be efficient for up to 1000 records

**Usability:**
- NFR2.1: UI should be intuitive and easy to navigate
- NFR2.2: Clear feedback messages for user actions
- NFR2.3: Consistent visual design

**Reliability:**
- NFR3.1: Handle common errors gracefully
- NFR3.2: Ensure reliable data persistence

**Security:**
- NFR4.1: Store passwords securely using BCrypt
- NFR4.2: Prevent unauthorized access
- NFR4.3: Role-based access control

**Maintainability:**
- NFR5.1: Well-structured, modular code following MVC
- NFR5.2: Well-commented code with Javadoc
- NFR5.3: Maven dependency management

**Scalability (Basic):**
- NFR6.1: SQLite sufficient for single-user/small-team
- NFR6.2: Architecture allows for future database migration