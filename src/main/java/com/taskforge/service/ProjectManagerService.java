package com.taskforge.service;

import com.taskforge.dao.ProjectDAO;
import com.taskforge.dao.TeamDAO;
import com.taskforge.model.Project;
import com.taskforge.model.Team;

import java.util.List;
import java.util.Optional;

/**
 * Service class responsible for managing project-related business logic.
 * This class orchestrates operations between ProjectDAO and TeamDAO
 * to provide a higher-level API for project creation, retrieval, and updates.
 */
public class ProjectManagerService {

    private final ProjectDAO projectDAO;
    private final TeamDAO teamDAO;

    /**
     * Constructs a ProjectManagerService with necessary DAO dependencies.
     * This allows for dependency injection, making the class more testable and modular.
     *
     * @param projectDAO The Data Access Object for Project entities.
     * @param teamDAO The Data Access Object for Team entities.
     */
    public ProjectManagerService(ProjectDAO projectDAO, TeamDAO teamDAO) {
        this.projectDAO = projectDAO;
        this.teamDAO = teamDAO;
    }

    /**
     * Creates a new project in the system.
     * Associates the project with a team if a valid team ID is provided.
     *
     * @param projectName The name of the project.
     * @param teamId The ID of the team to associate with the project (optional, can be 0 or negative if no team).
     * @return An Optional containing the created Project object if successful, or an empty Optional if creation fails.
     */
    public Optional<Project> createProject(String projectName, int teamId) {
        // Basic input validation
        if (projectName == null || projectName.trim().isEmpty()) {
            System.err.println("Project creation failed: Project name cannot be empty.");
            return Optional.empty();
        }

        Team team = null;
        if (teamId > 0) {
            team = teamDAO.getTeamById(teamId).orElse(null);
            if (team == null) {
                System.err.println("Project creation failed: Team with ID " + teamId + " not found.");
                return Optional.empty();
            }
        }

        Project newProject = new Project(projectName, team);
        Project createdProject = projectDAO.createProject(newProject);

        if (createdProject != null) {
            System.out.println("Project created successfully: " + createdProject.getName());
            return Optional.of(createdProject);
        } else {
            System.err.println("Project creation failed: Database operation failed.");
            return Optional.empty();
        }
    }

    /**
     * Retrieves a project by its ID.
     *
     * @param projectId The ID of the project to retrieve.
     * @return An Optional containing the Project object if found, or an empty Optional.
     */
    public Optional<Project> getProjectById(int projectId) {
        return projectDAO.getProjectById(projectId);
    }

    /**
     * Retrieves all projects in the system.
     *
     * @return A list of all projects.
     */
    public List<Project> getAllProjects() {
        return projectDAO.getAllProjects();
    }

    /**
     * Retrieves projects associated with a specific team.
     *
     * @param teamId The ID of the team whose projects are to be retrieved.
     * @return A list of projects associated with the specified team.
     */
    public List<Project> getProjectsByTeam(int teamId) {
        return projectDAO.getProjectsByTeamId(teamId);
    }

    /**
     * Updates an existing project's details.
     *
     * @param projectId The ID of the project to update.
     * @param newProjectName The new name for the project.
     * @param newTeamId The ID of the new team to associate (0 or negative for no team).
     * @return true if the project was updated successfully, false otherwise.
     */
    public boolean updateProject(int projectId, String newProjectName, int newTeamId) {
        Optional<Project> existingProjectOptional = projectDAO.getProjectById(projectId);
        if (existingProjectOptional.isEmpty()) {
            System.err.println("Project update failed: Project with ID " + projectId + " not found.");
            return false;
        }

        Project projectToUpdate = existingProjectOptional.get();

        // Update name
        projectToUpdate.setName(newProjectName);

        // Update team association
        Team newTeam = null;
        if (newTeamId > 0) {
            newTeam = teamDAO.getTeamById(newTeamId).orElse(null);
            if (newTeam == null) {
                System.err.println("Project update failed: New team with ID " + newTeamId + " not found.");
                return false;
            }
        }
        projectToUpdate.setTeam(newTeam);

        boolean success = projectDAO.updateProject(projectToUpdate);
        if (success) {
            System.out.println("Project ID " + projectId + " updated successfully.");
        } else {
            System.err.println("Project update failed: Database operation failed.");
        }
        return success;
    }

    /**
     * Deletes a project from the system.
     *
     * @param projectId The ID of the project to delete.
     * @return true if the project was deleted successfully, false otherwise.
     */
    public boolean deleteProject(int projectId) {
        boolean success = projectDAO.deleteProject(projectId);
        if (success) {
            System.out.println("Project ID " + projectId + " deleted successfully.");
        } else {
            System.err.println("Project deletion failed: Project with ID " + projectId + " not found or database error.");
        }
        return success;
    }
}
