package com.taskforge.service;

import com.taskforge.dao.ProjectDAO;
import com.taskforge.dao.TeamDAO;
import com.taskforge.model.Project;
import com.taskforge.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectManagerServiceTest {

    @Mock
    private ProjectDAO projectDAO;

    @Mock
    private TeamDAO teamDAO;

    private ProjectManagerService projectManagerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        projectManagerService = new ProjectManagerService(projectDAO, teamDAO);
    }

    @Test
    void createProject_Success() {
        // Arrange
        String projectName = "Test Project";
        Team mockTeam = new Team("Test Team");
        mockTeam.setId(1);
        Project mockProject = new Project(projectName, mockTeam);

        when(teamDAO.getTeamById(1)).thenReturn(Optional.of(mockTeam));
        when(projectDAO.createProject(any(Project.class))).thenReturn(mockProject);

        // Act
        Optional<Project> result = projectManagerService.createProject(projectName, 1);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(projectName, result.get().getName());
        verify(projectDAO, times(1)).createProject(any(Project.class));
    }

    @Test
    void getProjectById_Success() {
        // Arrange
        int projectId = 1;
        Project mockProject = new Project("Test Project", null);
        mockProject.setId(projectId);

        when(projectDAO.getProjectById(projectId)).thenReturn(Optional.of(mockProject));

        // Act
        Optional<Project> result = projectManagerService.getProjectById(projectId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(projectId, result.get().getId());
    }

    @Test
    void updateProject_Success() {
        // Arrange
        int projectId = 1;
        String newName = "Updated Project";
        Project existingProject = new Project("Old Name", null);
        existingProject.setId(projectId);

        when(projectDAO.getProjectById(projectId)).thenReturn(Optional.of(existingProject));
        when(projectDAO.updateProject(any(Project.class))).thenReturn(true);

        // Act
        boolean result = projectManagerService.updateProject(projectId, newName, 0);

        // Assert
        assertTrue(result);
        assertEquals(newName, existingProject.getName());
    }

    @Test
    void deleteProject_Success() {
        // Arrange
        int projectId = 1;
        when(projectDAO.deleteProject(projectId)).thenReturn(true);

        // Act
        boolean result = projectManagerService.deleteProject(projectId);

        // Assert
        assertTrue(result);
    }
}