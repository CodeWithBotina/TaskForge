package com.taskforge.model;

/**
 * Represents the visibility level of a task in the TaskForge system.
 * This enum defines who can view a particular task.
 */
public enum Visibility {
    /**
     * Public tasks are visible to all users in the system.
     */
    PUBLIC,
    /**
     * Restricted tasks are visible only to users who share a team with the task creator.
     */
    RESTRICTED,
    /**
     * Private tasks are visible only to the task creator.
     */
    PRIVATE;

    /**
     * Returns a user-friendly string representation of the visibility.
     * This can be used for display purposes in the UI.
     *
     * @return The capitalized name of the enum constant, with underscores replaced by spaces.
     */
    @Override
    public String toString() {
        String name = this.name();
        name = name.replace("_", " ").toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
