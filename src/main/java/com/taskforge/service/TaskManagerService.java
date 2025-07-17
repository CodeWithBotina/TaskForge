package com.taskforge.service;

import com.taskforge.dao.NotificationDAO;
import com.taskforge.dao.ProjectDAO;
import com.taskforge.dao.TaskDAO;
import com.taskforge.dao.UserDAO;
import com.taskforge.dao.UserTeamDAO;
import com.taskforge.model.Notification;
import com.taskforge.model.Priority;
import com.taskforge.model.Project;
import com.taskforge.model.Status;
import com.taskforge.model.Task;
import com.taskforge.model.User;
import com.taskforge.model.UserTeamMembership;
import com.taskforge.model.Visibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class responsible for managing task-related business logic.
 * This class orchestrates operations between TaskDAO, UserDAO, ProjectDAO,
 * NotificationDAO, and UserTeamDAO to provide a higher-level API for task management.
 * It handles task creation, retrieval, updates, deletion, and enforces business rules
 * such as visibility and assignment. It also manages notifications related to tasks.
 */
public class TaskManagerService {

    private final TaskDAO taskDAO;
    private final UserDAO userDAO;
    private final ProjectDAO projectDAO;
    private final NotificationDAO notificationDAO;
    private final UserTeamDAO userTeamDAO; // Dependency for team membership checks
    private final UserManagerService userManagerService; // Dependency for user/team related checks

    /**
     * Constructs a TaskManagerService with necessary DAO and Service dependencies.
     * This allows for dependency injection, making the class more testable and modular.
     *
     * @param taskDAO The Data Access Object for Task entities.
     * @param userDAO The Data Access Object for User entities.
     * @param projectDAO The Data Access Object for Project entities.
     * @param notificationDAO The Data Access Object for Notification entities.
     * @param userTeamDAO The Data Access Object for UserTeamMembership entities.
     * @param userManagerService The Service for managing users and teams.
     */
    public TaskManagerService(TaskDAO taskDAO, UserDAO userDAO, ProjectDAO projectDAO,
                              NotificationDAO notificationDAO, UserTeamDAO userTeamDAO, UserManagerService userManagerService) {
        this.taskDAO = taskDAO;
        this.userDAO = userDAO;
        this.projectDAO = projectDAO;
        this.notificationDAO = notificationDAO;
        this.userTeamDAO = userTeamDAO;
        this.userManagerService = userManagerService;
    }

    /**
     * Creates a new task in the system.
     * Assigns the task to a user and/or project if valid IDs are provided.
     * Sets the creator of the task.
     *
     * @param title The title of the task.
     * @param description A detailed description of the task (can be null).
     * @param dueDate The date and time when the task is due (can be null).
     * @param priority The priority level of the task.
     * @param assignedToUserId The ID of the user to assign the task to (0 or negative for unassigned).
     * @param projectId The ID of the project to associate with the task (0 or negative for no project).
     * @param visibility The visibility level of the task.
     * @param creatorId The ID of the user who created this task.
     * @return An Optional containing the created Task object if successful, or an empty Optional if creation fails.
     */
    public Optional<Task> createTask(String title, String description, LocalDateTime dueDate,
                                     Priority priority, int assignedToUserId, int projectId,
                                     Visibility visibility, int creatorId) {
        // Basic input validation
        if (title == null || title.trim().isEmpty()) {
            System.err.println("Task creation failed: Task title cannot be empty.");
            return Optional.empty();
        }
        if (priority == null) {
            System.err.println("Task creation failed: Priority cannot be null.");
            return Optional.empty();
        }
        if (visibility == null) {
            System.err.println("Task creation failed: Visibility cannot be null.");
            return Optional.empty();
        }

        Optional<User> creatorOptional = userDAO.getUserById(creatorId);
        if (creatorOptional.isEmpty()) {
            System.err.println("Task creation failed: Creator user with ID " + creatorId + " not found.");
            return Optional.empty();
        }

        User assignedTo = null;
        if (assignedToUserId > 0) {
            assignedTo = userDAO.getUserById(assignedToUserId).orElse(null);
            if (assignedTo == null) {
                System.err.println("Task creation failed: Assigned user with ID " + assignedToUserId + " not found.");
                return Optional.empty();
            }
        }

        Project project = null;
        if (projectId > 0) {
            project = projectDAO.getProjectById(projectId).orElse(null);
            if (project == null) {
                System.err.println("Task creation failed: Project with ID " + projectId + " not found.");
                return Optional.empty();
            }
        }

        // Default status for new tasks
        Task newTask = new Task(title, description, dueDate, priority, Status.PENDING, assignedTo, project, visibility, creatorOptional.get());
        Task createdTask = taskDAO.createTask(newTask);

        if (createdTask != null) {
            System.out.println("Task created successfully: " + createdTask.getTitle());
            // Send notification if assigned to a different user
            if (assignedTo != null && assignedTo.getId() != creatorId) {
                String message = String.format("You have been assigned to a new task: '%s' by %s.",
                        createdTask.getTitle(), creatorOptional.get().getUsername());
                notificationDAO.createNotification(new Notification(assignedTo, message, LocalDateTime.now(), createdTask.getId(), Notification.NotificationType.TASK_ASSIGNMENT));
            }
            return Optional.of(createdTask);
        } else {
            System.err.println("Task creation failed: Database operation failed.");
            return Optional.empty();
        }
    }

    /**
     * Retrieves a task by its ID.
     *
     * @param taskId The ID of the task to retrieve.
     * @return An Optional containing the Task object if found, or an empty Optional.
     */
    public Optional<Task> getTaskById(int taskId) {
        return taskDAO.getTaskById(taskId);
    }

    /**
     * Retrieves all tasks assigned to a specific user.
     *
     * @param assignedUserId The ID of the user to whom tasks are assigned.
     * @param currentUserId The ID of the currently logged-in user (for visibility checks).
     * @return A list of Task objects assigned to the given user that are visible to the current user.
     */
    public List<Task> getTasksByAssignedUser(int assignedUserId, int currentUserId) {
        List<Task> tasks = taskDAO.getTasksByAssignedUserId(assignedUserId);
        return filterTasksByVisibility(tasks, currentUserId);
    }

    /**
     * Retrieves all tasks that are visible to a specific user.
     * This includes public tasks, tasks created by the user, tasks assigned to the user,
     * and restricted tasks if the user shares a team with the creator.
     *
     * @param currentUserId The ID of the user for whom to retrieve visible tasks.
     * @return A list of Task objects visible to the current user.
     */
    public List<Task> getAllVisibleTasks(int currentUserId) {
        List<Task> allTasks = taskDAO.getAllTasks();
        return filterTasksByVisibility(allTasks, currentUserId);
    }

    /**
     * Filters a list of tasks based on the visibility rules for a given user.
     *
     * @param tasks The list of tasks to filter.
     * @param currentUserId The ID of the user who is viewing the tasks.
     * @return A new list containing only the tasks visible to the current user.
     */
    private List<Task> filterTasksByVisibility(List<Task> tasks, int currentUserId) {
        return tasks.stream()
                .filter(task -> {
                    // Task creator can always see their tasks
                    if (task.getCreator() != null && task.getCreator().getId() == currentUserId) {
                        return true;
                    }
                    // Task assigned to user can always see their tasks
                    if (task.getAssignedTo() != null && task.getAssignedTo().getId() == currentUserId) {
                        return true;
                    }

                    switch (task.getVisibility()) {
                        case PUBLIC:
                            return true; // Public tasks are visible to everyone
                        case RESTRICTED:
                            // Restricted tasks are visible if current user shares a team with creator
                            return userManagerService.areUsersInSameTeam(currentUserId, task.getCreator().getId());
                        case PRIVATE:
                            // Private tasks are only visible to creator (already handled above)
                            return false;
                        default:
                            return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing task's details.
     *
     * @param taskId The ID of the task to update.
     * @param title The new title.
     * @param description The new description.
     * @param dueDate The new due date.
     * @param priority The new priority.
     * @param status The new status.
     * @param assignedToUserId The ID of the new assigned user (0 or negative for unassigned).
     * @param projectId The ID of the new project (0 or negative for no project).
     * @param visibility The new visibility.
     * @param currentUserId The ID of the user performing the update (for permission checks).
     * @return true if the task was updated successfully, false otherwise.
     */
    public boolean updateTask(int taskId, String title, String description, LocalDateTime dueDate,
                              Priority priority, Status status, int assignedToUserId, int projectId,
                              Visibility visibility, int currentUserId) {
        Optional<Task> existingTaskOptional = taskDAO.getTaskById(taskId);
        if (existingTaskOptional.isEmpty()) {
            System.err.println("Task update failed: Task with ID " + taskId + " not found.");
            return false;
        }

        Task taskToUpdate = existingTaskOptional.get();

        // Permission check: Only creator or a team owner (if task is team-related) can update
        // For simplicity, let's say only the creator can update for now.
        // More complex rules (e.g., team owners, project managers) would go here.
        if (taskToUpdate.getCreator().getId() != currentUserId) {
            System.err.println("Task update failed: User " + currentUserId + " is not the creator of task " + taskId + ".");
            return false;
        }

        // Update fields
        taskToUpdate.setTitle(title);
        taskToUpdate.setDescription(description);
        taskToUpdate.setDueDate(dueDate);
        taskToUpdate.setPriority(priority);
        taskToUpdate.setStatus(status);
        taskToUpdate.setVisibility(visibility);

        // Handle assigned user update
        User oldAssignedTo = taskToUpdate.getAssignedTo();
        User newAssignedTo = null;
        if (assignedToUserId > 0) {
            newAssignedTo = userDAO.getUserById(assignedToUserId).orElse(null);
            if (newAssignedTo == null) {
                System.err.println("Task update failed: New assigned user with ID " + assignedToUserId + " not found.");
                return false;
            }
        }
        taskToUpdate.setAssignedTo(newAssignedTo);

        // Handle project update
        Project newProject = null;
        if (projectId > 0) {
            newProject = projectDAO.getProjectById(projectId).orElse(null);
            if (newProject == null) {
                System.err.println("Task update failed: New project with ID " + projectId + " not found.");
                return false;
            }
        }
        taskToUpdate.setProject(newProject);

        boolean success = taskDAO.updateTask(taskToUpdate);
        if (success) {
            System.out.println("Task ID " + taskId + " updated successfully.");

            // Send notification if assigned to a different user
            if (newAssignedTo != null && (oldAssignedTo == null || !oldAssignedTo.equals(newAssignedTo))) {
                String message = String.format("You have been assigned to task: '%s' by %s.",
                        taskToUpdate.getTitle(), userDAO.getUserById(currentUserId).get().getUsername());
                notificationDAO.createNotification(new Notification(newAssignedTo, message, LocalDateTime.now(), taskToUpdate.getId(), Notification.NotificationType.TASK_ASSIGNMENT));
            }
            // TODO: Add notifications for status changes, due date reminders etc.
        } else {
            System.err.println("Task update failed: Database operation failed.");
        }
        return success;
    }

    /**
     * Deletes a task from the system.
     * Only the creator of the task can delete it.
     *
     * @param taskId The ID of the task to delete.
     * @param currentUserId The ID of the user attempting to delete the task.
     * @return true if the task was deleted successfully, false otherwise.
     */
    public boolean deleteTask(int taskId, int currentUserId) {
        Optional<Task> taskOptional = taskDAO.getTaskById(taskId);
        if (taskOptional.isEmpty()) {
            System.err.println("Task deletion failed: Task with ID " + taskId + " not found.");
            return false;
        }

        Task taskToDelete = taskOptional.get();

        // Permission check: Only the creator can delete the task
        if (taskToDelete.getCreator().getId() != currentUserId) {
            System.err.println("Task deletion failed: User " + currentUserId + " is not the creator of task " + taskId + ".");
            return false;
        }

        boolean success = taskDAO.deleteTask(taskId);
        if (success) {
            System.out.println("Task ID " + taskId + " deleted successfully.");
        } else {
            System.err.println("Task deletion failed: Database operation failed.");
        }
        return success;
    }
}
