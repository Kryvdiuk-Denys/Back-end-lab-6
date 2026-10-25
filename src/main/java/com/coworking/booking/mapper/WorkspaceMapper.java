package com.coworking.booking.mapper;

import com.coworking.booking.dto.WorkspaceDto;
import com.coworking.booking.entity.Workspace;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {

    WorkspaceDto toDto(Workspace workspace);

    List<WorkspaceDto> toDtoList(List<Workspace> workspaces);

    @Mapping(target = "id", ignore = true)
    Workspace toEntity(WorkspaceDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(WorkspaceDto dto, @MappingTarget Workspace workspace);
}
