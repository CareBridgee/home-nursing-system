package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.entity.NurseOffer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface NurseOfferMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "serviceRequest", ignore = true)
    @Mapping(target = "nurse", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NurseOffer toEntity(NurseOfferRequest request);

    @Mapping(target = "serviceRequestId", source = "serviceRequest.id")
    @Mapping(target = "nurseId", source = "nurse.id")
    NurseOfferResponse toResponse(NurseOffer offer);
}
