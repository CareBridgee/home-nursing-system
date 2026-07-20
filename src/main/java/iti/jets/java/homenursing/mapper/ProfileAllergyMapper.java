package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.ProfileAllergyRequest;
import iti.jets.java.homenursing.dto.ProfileAllergyResponse;
import iti.jets.java.homenursing.entity.ProfileAllergy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ProfileAllergyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "allergy", ignore = true)
    ProfileAllergy toEntity(ProfileAllergyRequest request);

    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(target = "allergyId", source = "allergy.id")
    @Mapping(target = "allergyName", source = "allergy.name")
    @Mapping(target = "allergyType", source = "allergy.type")
    ProfileAllergyResponse toResponse(ProfileAllergy entity);
}
