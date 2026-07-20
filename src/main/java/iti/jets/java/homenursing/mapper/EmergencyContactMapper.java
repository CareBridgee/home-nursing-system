package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.EmergencyContactRequest;
import iti.jets.java.homenursing.dto.EmergencyContactResponse;
import iti.jets.java.homenursing.entity.EmergencyContact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface EmergencyContactMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "profile", ignore = true)
    EmergencyContact toEntity(EmergencyContactRequest request);

    @Mapping(target = "profileId", source = "profile.id")
    EmergencyContactResponse toResponse(EmergencyContact emergencyContact);
}
