package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.catalog.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse;
import iti.jets.java.homenursing.entity.ServiceType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ServiceTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ServiceType toEntity(ServiceTypeRequest request);

    ServiceTypeResponse toResponse(ServiceType serviceType);
}
