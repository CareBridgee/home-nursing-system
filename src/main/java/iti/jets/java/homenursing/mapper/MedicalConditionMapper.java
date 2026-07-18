package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.MedicalConditionResponse;
import iti.jets.java.homenursing.entity.MedicalCondition;
import org.mapstruct.Mapper;
import org.springframework.context.i18n.LocaleContextHolder;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public abstract class MedicalConditionMapper {

    public MedicalConditionResponse toResponse(MedicalCondition entity) {
        if (entity == null) {
            return null;
        }
        boolean isAr = "ar".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage());
        MedicalConditionResponse.MedicalConditionResponseBuilder builder = MedicalConditionResponse.builder()
                .id(entity.getId())
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
