package com.taskforge.model;

/**
 * Represents the priority level of a task in the TaskForge system.
 * This enum defines a set of predefined priority levels.
 */
public enum Priority {
    LOW,    // Indicates a low priority task
    MEDIUM, // Indicates a medium priority task
    HIGH;   // Indicates a high priority task

    /**
     * Returns a user-friendly string representation of the priority.
     * This can be used for display purposes in the UI.
     *
     * @return The capitalized name of the enum constant.
     */
    @Override
    public String toString() {
        return this.name().charAt(0) + this.name().substring(1).toLowerCase();
    }
}
