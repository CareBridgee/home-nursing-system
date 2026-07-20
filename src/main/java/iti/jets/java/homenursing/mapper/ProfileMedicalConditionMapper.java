package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.ProfileMedicalConditionRequest;
import iti.jets.java.homenursing.dto.ProfileMedicalConditionResponse;
import iti.jets.java.homenursing.entity.ProfileMedicalCondition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ProfileMedicalConditionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "medicalCondition", ignore = true)
    ProfileMedicalCondition toEntity(ProfileMedicalConditionRequest request);

    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(target = "medicalConditionId", source = "medicalCondition.id")
    @Mapping(target = "conditionName", source = "medicalCondition.name")
    ProfileMedicalConditionResponse toResponse(ProfileMedicalCondition entity);
}
