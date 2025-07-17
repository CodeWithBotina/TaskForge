package com.taskforge.model;

/**
 * Represents the current status or state of a task in the TaskForge system.
 * This enum defines a set of predefined task statuses.
 */
public enum Status {
    PENDING,    // Task has been created but not yet started
    IN_PROGRESS, // Task is currently being worked on
    COMPLETED,  // Task has been finished
    BLOCKED;    // Task is blocked due to dependencies or other issues

    /**
     * Returns a user-friendly string representation of the status.
     * This can be used for display purposes in the UI.
     *
     * @return The capitalized name of the enum constant, with underscores replaced by spaces.
     */
    @Override
    public String toString() {
        // Convert PENDING -> Pending, IN_PROGRESS -> In Progress, etc.
        String name = this.name();
        name = name.replace("_", " ").toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
