package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.MedicalHistoryRequest;
import iti.jets.java.homenursing.dto.MedicalHistoryResponse;
import iti.jets.java.homenursing.entity.MedicalHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface MedicalHistoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "profile", ignore = true)
    MedicalHistory toEntity(MedicalHistoryRequest request);

    @Mapping(target = "profileId", source = "profile.id")
    MedicalHistoryResponse toResponse(MedicalHistory medicalHistory);
}
