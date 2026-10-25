package com.coworking.booking;

import com.coworking.booking.dto.WorkspaceDto;
import com.coworking.booking.entity.Workspace;
import com.coworking.booking.mapper.WorkspaceMapper;
import com.coworking.booking.repository.BookingRepository;
import com.coworking.booking.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMapper workspaceMapper;
    private final BookingRepository bookingRepository;

    public List<WorkspaceDto> findAll() {
        return workspaceMapper.toDtoList(workspaceRepository.findAllByOrderByNameAsc());
    }

    public WorkspaceDto findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Workspace ID must not be null");
        }
        return workspaceMapper.toDto(getEntity(id));
    }

    @Transactional
    public WorkspaceDto create(WorkspaceDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Workspace DTO must not be null");
        }
        Workspace entity = workspaceMapper.toEntity(dto);
        Workspace saved = workspaceRepository.save(entity);
        return workspaceMapper.toDto(saved);
    }

    @Transactional
    public WorkspaceDto update(Long id, WorkspaceDto dto) {
        if (id == null) {
            throw new IllegalArgumentException("Workspace ID must not be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Workspace DTO must not be null");
        }
        Workspace workspace = getEntity(id);
        workspaceMapper.updateEntityFromDto(dto, workspace);
        return workspaceMapper.toDto(workspace);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Workspace ID must not be null");
        }
        if (bookingRepository.existsByWorkspaceId(id)) {
            throw new IllegalStateException("Cannot delete workspace with existing bookings");
        }
        Workspace workspace = getEntity(id);
        workspaceRepository.delete(workspace);
    }

    public Workspace getEntity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Workspace ID must not be null");
        }
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace " + id + " not found"));
    }
}