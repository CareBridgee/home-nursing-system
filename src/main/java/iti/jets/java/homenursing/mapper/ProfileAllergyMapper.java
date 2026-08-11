package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.profile.ProfileAllergyResponse;
import iti.jets.java.homenursing.entity.ProfileAllergy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ProfileAllergyMapper {

    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(target = "allergyId", source = "allergy.id")
    @Mapping(target = "allergyName", source = "allergy.name")
    @Mapping(target = "allergyType", source = "allergy.type")
    ProfileAllergyResponse toResponse(ProfileAllergy entity);
}
