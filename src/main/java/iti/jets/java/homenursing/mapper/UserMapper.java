package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.UserRequest;
import iti.jets.java.homenursing.dto.UserResponse;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.service.ProfileCompletionChecker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ProfileCompletionChecker.class,
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(UserRequest request);

    @Mapping(target = "defaultProfileId",
             expression = "java(user.getProfiles() == null ? null : "
                     + "user.getProfiles().stream()"
                     + ".filter(p -> Boolean.TRUE.equals(p.getIsPrimary()))"
                     + ".map(iti.jets.java.homenursing.entity.Profile::getId)"
                     + ".findFirst().orElse(null))")
    @Mapping(target = "profileCompleted", source = "user", qualifiedByName = "isUserProfileCompleted")
    UserResponse toResponse(User user);
}
