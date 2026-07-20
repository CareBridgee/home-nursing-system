package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.nurse.NurseRegistrationRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceResponse;
import iti.jets.java.homenursing.dto.nurse.NurseUpdateRequest;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseService;
import iti.jets.java.homenursing.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NurseMapper {

    Nurse toEntity(NurseRegistrationRequest request, User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(NurseUpdateRequest request, @MappingTarget Nurse nurse);

    @Mapping(target = "userId", source = "nurse.user.id")
    @Mapping(target = "firstName", source = "nurse.user.firstName")
    @Mapping(target = "lastName", source = "nurse.user.lastName")
    @Mapping(target = "phoneNumber", source = "nurse.user.phoneNumber")
    NurseResponse toResponse(Nurse nurse, List<NurseServiceResponse> services);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "services", ignore = true)
    NurseResponse toSimpleResponse(Nurse nurse);

    @Mapping(target = "serviceTypeId", source = "serviceType.id")
    @Mapping(target = "serviceName", source = "serviceType.name")
    @Mapping(target = "serviceDescription", source = "serviceType.description")
    @Mapping(target = "basePrice", source = "serviceType.basePrice")
    NurseServiceResponse toServiceResponse(NurseService nurseServiceLink);
}
