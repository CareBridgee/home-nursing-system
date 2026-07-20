package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.ProfileMedicationRequest;
import iti.jets.java.homenursing.dto.ProfileMedicationResponse;
import iti.jets.java.homenursing.entity.ProfileMedication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ProfileMedicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "medication", ignore = true)
    ProfileMedication toEntity(ProfileMedicationRequest request);

    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(target = "medicationId", source = "medication.id")
    @Mapping(target = "medicationName", source = "medication.name")
    ProfileMedicationResponse toResponse(ProfileMedication entity);
}
