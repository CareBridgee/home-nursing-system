package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.ProfileRequest;
import iti.jets.java.homenursing.dto.ProfileResponse;
import iti.jets.java.homenursing.entity.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "isPrimary", ignore = true)
    Profile toEntity(ProfileRequest request);

    @Mapping(target = "userId", source = "user.id")
    ProfileResponse toResponse(Profile profile);
}
