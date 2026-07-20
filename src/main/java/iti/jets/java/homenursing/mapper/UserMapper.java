package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.UserRequest;
import iti.jets.java.homenursing.dto.UserResponse;
import iti.jets.java.homenursing.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(UserRequest request);

    @Mapping(target = "defaultProfileId",
             expression = "java(user.getProfiles() == null ? null : "
                     + "user.getProfiles().stream()"
                     + ".filter(p -> Boolean.TRUE.equals(p.getIsPrimary()))"
                     + ".map(iti.jets.java.homenursing.entity.Profile::getId)"
                     + ".findFirst().orElse(null))")
    UserResponse toResponse(User user);
}
