package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.AllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import org.mapstruct.Mapper;
import org.springframework.context.i18n.LocaleContextHolder;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public abstract class AllergyMapper {

    public AllergyResponse toResponse(Allergy entity) {
        if (entity == null) {
            return null;
        }
        boolean isAr = "ar".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage());
        return AllergyResponse.builder()
                .id(entity.getId())
                .name(pick(isAr, entity.getNameAr(), entity.getName()))
                .build();
    }

    protected String pick(boolean isAr, String ar, String en) {
        if (isAr && ar != null && !ar.isBlank()) {
            return ar;
        }
        return en;
    }
}
