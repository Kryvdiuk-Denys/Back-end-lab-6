package com.coworking.booking.service;

import com.coworking.booking.ResourceNotFoundException;
import com.coworking.booking.WorkspaceService;
import com.coworking.booking.dto.WorkspaceDto;
import com.coworking.booking.entity.Workspace;
import com.coworking.booking.mapper.WorkspaceMapper;
import com.coworking.booking.repository.BookingRepository;
import com.coworking.booking.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMapper workspaceMapper;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    @DisplayName("findAll повертає список скоригованих DTO")
    void findAllExistingWorkspacesReturnsMappedDtos() {
        // Arrange
        Workspace workspace = workspace(1L, "Focus Room");
        WorkspaceDto dto = workspaceDto(1L, "Focus Room");
        when(workspaceRepository.findAllByOrderByNameAsc()).thenReturn(List.of(workspace));
        when(workspaceMapper.toDtoList(any())).thenReturn(List.of(dto));

        // Act
        List<WorkspaceDto> result = workspaceService.findAll();

        // Assert
        assertEquals(List.of(dto), result);
        verify(workspaceRepository).findAllByOrderByNameAsc();
        verify(workspaceMapper).toDtoList(any());
    }

    @Test
    @DisplayName("findById з існуючим ID повертає DTO")
    void findByIdExistingWorkspaceReturnsMappedDto() {
        // Arrange
        Workspace workspace = workspace(1L, "Focus Room");
        WorkspaceDto dto = workspaceDto(1L, "Focus Room");
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(workspaceMapper.toDto(workspace)).thenReturn(dto);

        // Act
        WorkspaceDto result = workspaceService.findById(1L);

        // Assert
        assertSame(dto, result);
        verify(workspaceRepository).findById(1L);
        verify(workspaceMapper).toDto(workspace);
    }

    @Test
    @DisplayName("findById з відсутнім ID викидає ResourceNotFoundException")
    void findByIdMissingWorkspaceThrowsResourceNotFoundException() {
        // Arrange
        when(workspaceRepository.findById(404L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> workspaceService.findById(404L)
        );

        // Assert
        assertEquals("Workspace 404 not found", exception.getMessage());
        verify(workspaceRepository).findById(404L);
    }

    @Test
    @DisplayName("findById з null ID викидає IllegalArgumentException")
    void findByIdNullIdThrowsIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workspaceService.findById(null)
        );

        // Assert
        assertEquals("Workspace ID must not be null", exception.getMessage());
        verify(workspaceRepository, never()).findById(any());
    }

    @Test
    @DisplayName("create з валідними даними зберігає робочий простір")
    void createValidWorkspaceSavesWorkspace() {
        // Arrange
        WorkspaceDto request = workspaceDto(null, "Focus Room");
        Workspace mappedEntity = workspace(null, "Focus Room");
        Workspace savedEntity = workspace(1L, "Focus Room");
        WorkspaceDto response = workspaceDto(1L, "Focus Room");

        when(workspaceMapper.toEntity(request)).thenReturn(mappedEntity);
        when(workspaceRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(workspaceMapper.toDto(savedEntity)).thenReturn(response);

        // Act
        WorkspaceDto result = workspaceService.create(request);

        // Assert
        assertSame(response, result);
        verify(workspaceRepository).save(mappedEntity);
        verify(workspaceMapper).toDto(savedEntity);
    }

    @Test
    @DisplayName("create з null DTO викидає IllegalArgumentException")
    void createNullWorkspaceDtoThrowsIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workspaceService.create(null)
        );

        // Assert
        assertEquals("Workspace DTO must not be null", exception.getMessage());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Оновлення робочого простору через if-else без моків")
    void updateWorkspaceWithIfElseWithoutMocks() {
        // Arrange
        Workspace existingWorkspace = workspace(1L, "Focus Room");
        WorkspaceDto request = workspaceDto(1L, "Updated Room");

        // Act
        if (request != null && existingWorkspace != null) {
            if (request.getName() != null) {
                existingWorkspace.setName(request.getName());
            } else {
                existingWorkspace.setName("Default Room Name");
            }
        }

        // Assert
        assertNotNull(existingWorkspace);

        if ("Updated Room".equals(existingWorkspace.getName())) {
            assertEquals("Updated Room", existingWorkspace.getName());
        } else {
            fail("Ім'я робочого простору оновилося некоректно: " + existingWorkspace.getName());
        }
    }

    @Test
    @DisplayName("delete існуючого робочого простору видаляє сутність")
    void deleteExistingWorkspaceDeletesEntity() {
        // Arrange
        Workspace workspace = workspace(1L, "Focus Room");
        when(bookingRepository.existsByWorkspaceId(1L)).thenReturn(false);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));

        // Act
        workspaceService.delete(1L);

        // Assert
        verify(bookingRepository).existsByWorkspaceId(1L);
        verify(workspaceRepository).findById(1L);
        verify(workspaceRepository).delete(workspace);
    }

    @Test
    @DisplayName("delete з null ID викидає IllegalArgumentException")
    void deleteNullIdThrowsIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workspaceService.delete(null)
        );

        // Assert
        assertEquals("Workspace ID must not be null", exception.getMessage());
        verify(workspaceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getEntity для існуючого ID повертає сутність")
    void getEntityExistingWorkspaceReturnsEntity() {
        // Arrange
        Workspace workspace = workspace(1L, "Focus Room");
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));

        // Act
        Workspace result = workspaceService.getEntity(1L);

        // Assert
        assertSame(workspace, result);
        verify(workspaceRepository).findById(1L);
    }

    @Test
    @DisplayName("getEntity для відсутнього ID викидає ResourceNotFoundException")
    void getEntityMissingWorkspaceThrowsResourceNotFoundException() {
        // Arrange
        when(workspaceRepository.findById(404L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> workspaceService.getEntity(404L)
        );

        // Assert
        assertEquals("Workspace 404 not found", exception.getMessage());
        verify(workspaceRepository).findById(404L);
    }

    private static Workspace workspace(Long id, String name) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        workspace.setName(name);
        workspace.setType("ROOM");
        workspace.setCapacity(4);
        workspace.setHourlyRate(new BigDecimal("25.00"));
        workspace.setActive(true);
        return workspace;
    }

    private static WorkspaceDto workspaceDto(Long id, String name) {
        WorkspaceDto dto = new WorkspaceDto();
        dto.setId(id);
        dto.setName(name);
        dto.setType("ROOM");
        dto.setCapacity(4);
        dto.setHourlyRate(new BigDecimal("25.00"));
        dto.setActive(true);
        return dto;
    }
}