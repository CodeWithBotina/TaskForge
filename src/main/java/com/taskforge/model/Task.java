package com.taskforge.model;

import java.time.LocalDateTime; // For handling dates and times
import java.util.Objects;

/**
 * Represents a task within the TaskForge task management system.
 * This class encapsulates task properties such as ID, title, description,
 * due date, priority level, status, assigned user, associated project,
 * its visibility level, and the user who created it.
 */
public class Task {

    private int id;
    private String title;
    private String description;
    private LocalDateTime dueDate; // Using LocalDateTime for date and time
    private Priority priority;
    private Status status;
    private User assignedTo; // The user this task is assigned to (can be null)
    private Project project; // The project this task belongs to (can be null)
    private Visibility visibility; // Visibility level of the task
    private User creator; // New: The user who created this task

    /**
     * Default constructor for Task.
     * Useful for frameworks that require a no-argument constructor.
     */
    public Task() {
        // Default constructor
    }

    /**
     * Constructs a new Task with essential details.
     * The ID is typically generated by the database.
     *
     * @param title The title of the task.
     * @param description A detailed description of the task.
     * @param dueDate The date and time when the task is due.
     * @param priority The priority level of the task.
     * @param status The current status of the task.
     * @param assignedTo The user assigned to this task (can be null).
     * @param project The project this task belongs to (can be null).
     * @param visibility The visibility level of the task.
     * @param creator The user who created this task.
     */
    public Task(String title, String description, LocalDateTime dueDate,
                Priority priority, Status status, User assignedTo, Project project,
                Visibility visibility, User creator) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.project = project;
        this.visibility = visibility;
        this.creator = creator;
    }

    /**
     * Constructs a new Task with an ID and all specified details.
     * This constructor is typically used when retrieving task data from the database.
     *
     * @param id The unique identifier for the task.
     * @param title The title of the task.
     * @param description A detailed description of the task.
     * @param dueDate The date and time when the task is due.
     * @param priority The priority level of the task.
     * @param status The current status of the task.
     * @param assignedTo The user assigned to this task (can be null).
     * @param project The project this task belongs to (can be null).
     * @param visibility The visibility level of the task.
     * @param creator The user who created this task.
     */
    public Task(int id, String title, String description, LocalDateTime dueDate,
                Priority priority, Status status, User assignedTo, Project project,
                Visibility visibility, User creator) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.project = project;
        this.visibility = visibility;
        this.creator = creator;
    }

    // --- Getters and Setters ---

    /**
     * Gets the unique identifier of the task.
     * @return The task's ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the task.
     * This method is typically used by DAOs when persisting or retrieving data.
     * @param id The task's ID.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the title of the task.
     * @return The task's title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the task.
     * @param title The new task title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the description of the task.
     * @return The task's description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the task.
     * @param description The new task description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the due date and time of the task.
     * @return The task's due date.
     */
    public LocalDateTime getDueDate() {
        return dueDate;
    }

    /**
     * Sets the due date and time of the task.
     * @param dueDate The new due date.
     */
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Gets the priority level of the task.
     * @return The task's priority.
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Sets the priority level of the task.
     * @param priority The new priority.
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * Gets the current status of the task.
     * @return The task's status.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the current status of the task.
     * @param status The new status.
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the user assigned to this task.
     * @return The User object assigned to the task, or null if unassigned.
     */
    public User getAssignedTo() {
        return assignedTo;
    }

    /**
     * Sets the user assigned to this task.
     * @param assignedTo The User object to assign, or null to unassign.
     */
    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    /**
     * Gets the project this task belongs to.
     * @return The Project object this task belongs to, or null if not part of a project.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Sets the project this task belongs to.
     * @param project The Project object to associate, or null to disassociate.
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Gets the visibility level of the task.
     * @return The task's visibility.
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Sets the visibility level of the task.
     * @param visibility The new visibility.
     */
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    /**
     * Gets the user who created this task.
     * @return The User object who created the task.
     */
    public User getCreator() {
        return creator;
    }

    /**
     * Sets the user who created this task.
     * @param creator The User object who created the task.
     */
    public void setCreator(User creator) {
        this.creator = creator;
    }

    // --- Object Overrides for Equality and Hashing ---

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two Task objects are considered equal if their IDs are the same.
     *
     * @param o The reference object with which to compare.
     * @return true if this object is the same as the obj argument; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id; // Tasks are equal if their IDs are the same
    }

    /**
     * Returns a hash code value for the object.
     * Consistent with the equals method, the hash code is based on the task's ID.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns a string representation of the Task object.
     * Useful for logging and debugging.
     *
     * @return A string representation of the object.
     */
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", dueDate=" + dueDate +
                ", priority=" + priority +
                ", status=" + status +
                ", assignedTo=" + (assignedTo != null ? assignedTo.getUsername() : "Unassigned") +
                ", project=" + (project != null ? project.getName() : "None") +
                ", visibility=" + visibility +
                ", creator=" + (creator != null ? creator.getUsername() : "Unknown") +
                '}';
    }
}
