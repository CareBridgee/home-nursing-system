package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.ServiceTypeResponse;
import iti.jets.java.homenursing.entity.ServiceType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.context.i18n.LocaleContextHolder;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public abstract class ServiceTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    public abstract ServiceType toEntity(ServiceTypeRequest request);

    public ServiceTypeResponse toResponse(ServiceType entity) {
        if (entity == null) {
            return null;
        }
        boolean isAr = "ar".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage());
        ServiceTypeResponse.ServiceTypeResponseBuilder builder = ServiceTypeResponse.builder()
                .id(entity.getId())
                .estimatedDurationMinutes(entity.getEstimatedDurationMinutes())
                .basePrice(entity.getBasePrice())
                .name(pick(isAr, entity.getNameAr(), entity.getName()))
                .description(pick(isAr, entity.getDescriptionAr(), entity.getDescription()));

        return builder.build();
    }

    protected String pick(boolean isAr, String ar, String en) {
        if (isAr && ar != null && !ar.isBlank()) {
            return ar;
        }
        return en;
    }
}
