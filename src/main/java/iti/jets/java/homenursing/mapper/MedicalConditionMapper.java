package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.MedicalConditionRequest;
import iti.jets.java.homenursing.dto.MedicalConditionResponse;
import iti.jets.java.homenursing.entity.MedicalCondition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface MedicalConditionMapper {

    @Mapping(target = "id", ignore = true)
    MedicalCondition toEntity(MedicalConditionRequest request);

    MedicalConditionResponse toResponse(MedicalCondition medicalCondition);
}
