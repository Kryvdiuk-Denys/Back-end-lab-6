package com.coworking.booking.mapper;

import com.coworking.booking.dto.BookingDto;
import com.coworking.booking.entity.Booking;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "workspaceId", source = "workspace.id")
    @Mapping(target = "workspaceName", source = "workspace.name")
    BookingDto toDto(Booking booking);

    List<BookingDto> toDtoList(List<Booking> bookings);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "reminderSentAt", ignore = true)
    Booking toEntity(BookingDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "reminderSentAt", ignore = true)
    void updateEntityFromDto(BookingDto dto, @MappingTarget Booking booking);
}
