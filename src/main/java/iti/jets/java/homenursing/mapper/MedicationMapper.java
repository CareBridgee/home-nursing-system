package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.MedicationRequest;
import iti.jets.java.homenursing.dto.MedicationResponse;
import iti.jets.java.homenursing.entity.Medication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.context.i18n.LocaleContextHolder;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public abstract class MedicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    public abstract Medication toEntity(MedicationRequest request);

    public MedicationResponse toResponse(Medication entity) {
        if (entity == null) {
            return null;
        }
        boolean isAr = "ar".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage());
        return MedicationResponse.builder()
                .id(entity.getId())
                .name(pick(isAr, entity.getNameAr(), entity.getName()))
                .description(pick(isAr, entity.getDescriptionAr(), entity.getDescription()))
                .build();
    }

    protected String pick(boolean isAr, String ar, String en) {
        if (isAr && ar != null && !ar.isBlank()) {
            return ar;
        }
        return en;
    }
}
