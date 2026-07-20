package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.AddressRequest;
import iti.jets.java.homenursing.dto.AddressResponse;
import iti.jets.java.homenursing.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface AddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "profile", ignore = true)
    Address toEntity(AddressRequest request);

    @Mapping(target = "profileId", source = "profile.id")
    AddressResponse toResponse(Address address);
}
