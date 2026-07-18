package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.AllergyRequest;
import iti.jets.java.homenursing.dto.AllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface AllergyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Allergy toEntity(AllergyRequest request);

    AllergyResponse toResponse(Allergy allergy);
}
