package iti.jets.java.carenest.mapper;

import iti.jets.java.carenest.dto.UserRequest;
import iti.jets.java.carenest.dto.UserResponse;
import iti.jets.java.carenest.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(UserRequest request);

    UserResponse toResponse(User entity);
}
