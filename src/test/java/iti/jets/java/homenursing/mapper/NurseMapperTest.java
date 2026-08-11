package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.nurse.NurseRejectionDetailsResponse;
import iti.jets.java.homenursing.dto.nurse.NurseRegistrationRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceResponse;
import iti.jets.java.homenursing.dto.nurse.NurseUpdateRequest;
import iti.jets.java.homenursing.entity.FailedStep;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseRejectionDetail;
import iti.jets.java.homenursing.entity.NurseService;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class NurseMapperTest {

    private final NurseMapper mapper = Mappers.getMapper(NurseMapper.class);

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("Mai")
                .lastName("Elsayed")
                .phoneNumber("+201002003004")
                .profileImageUrl("https://img.example/mai.png")
                .build();
    }

    private Nurse nurseWithUser() {
        return Nurse.builder()
                .id(UUID.randomUUID())
                .user(user())
                .nationalId("29901011234567")
                .licenseNumber("LIC-001")
                .nationalIdFrontUrl("https://img.example/front.png")
                .nationalIdBackUrl("https://img.example/back.png")
                .licenseImageUrl("https://img.example/license.png")
                .professionalCertificateUrl("https://img.example/cert.png")
                .specialization("Elderly Care")
                .yearsOfExperience(5)
                .bio("Five years of home care")
                .ratingAvg(new BigDecimal("4.70"))
                .totalReviews(20)
                .verificationStatus(VerificationStatus.APPROVED)
                .rejectionReason("Approved")
                .build();
    }

    @Test
    void toEntity_mapsAllRequestFieldsAndUser() {
        NurseRegistrationRequest request = NurseRegistrationRequest.builder()
                .nationalId("29901011234567")
                .licenseNumber("LIC-001")
                .specialization("Elderly Care")
                .yearsOfExperience(5)
                .bio("Five years of home care")
                .build();
        User user = user();

        Nurse entity = mapper.toEntity(request, user);

        assertThat(entity).isNotNull();
        assertThat(entity.getNationalId()).isEqualTo("29901011234567");
        assertThat(entity.getLicenseNumber()).isEqualTo("LIC-001");
        assertThat(entity.getSpecialization()).isEqualTo("Elderly Care");
        assertThat(entity.getYearsOfExperience()).isEqualTo(5);
        assertThat(entity.getBio()).isEqualTo("Five years of home care");
        assertThat(entity.getUser()).isSameAs(user);
        assertThat(entity.getId()).isNull();
        assertThat(entity.getNationalIdFrontUrl()).isNull();
        assertThat(entity.getNationalIdBackUrl()).isNull();
        assertThat(entity.getLicenseImageUrl()).isNull();
        assertThat(entity.getProfessionalCertificateUrl()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullRequestAndUser_returnsNull() {
        assertThat(mapper.toEntity(null, null)).isNull();
    }

    @Test
    void toEntity_nullRequest_stillSetsUser() {
        User user = user();

        Nurse entity = mapper.toEntity(null, user);

        assertThat(entity).isNotNull();
        assertThat(entity.getUser()).isSameAs(user);
        assertThat(entity.getNationalId()).isNull();
        assertThat(entity.getSpecialization()).isNull();
        assertThat(entity.getBio()).isNull();
    }

    @Test
    void updateEntity_nullRequest_isNoOp() {
        Nurse nurse = nurseWithUser();

        mapper.updateEntity(null, nurse);

        assertThat(nurse.getNationalId()).isEqualTo("29901011234567");
        assertThat(nurse.getLicenseNumber()).isEqualTo("LIC-001");
        assertThat(nurse.getSpecialization()).isEqualTo("Elderly Care");
        assertThat(nurse.getYearsOfExperience()).isEqualTo(5);
        assertThat(nurse.getBio()).isEqualTo("Five years of home care");
    }

    @Test
    void updateEntity_allFieldsSet_overwritesTarget() {
        Nurse nurse = nurseWithUser();
        NurseUpdateRequest request = NurseUpdateRequest.builder()
                .nationalId("29902022234567")
                .licenseNumber("LIC-999")
                .specialization("Wound Care")
                .yearsOfExperience(8)
                .bio("Updated bio")
                .build();

        mapper.updateEntity(request, nurse);

        assertThat(nurse.getNationalId()).isEqualTo("29902022234567");
        assertThat(nurse.getLicenseNumber()).isEqualTo("LIC-999");
        assertThat(nurse.getSpecialization()).isEqualTo("Wound Care");
        assertThat(nurse.getYearsOfExperience()).isEqualTo(8);
        assertThat(nurse.getBio()).isEqualTo("Updated bio");
    }

    @Test
    void updateEntity_nullFields_keepsExistingValues() {
        Nurse nurse = nurseWithUser();
        NurseUpdateRequest request = NurseUpdateRequest.builder().build();

        mapper.updateEntity(request, nurse);

        assertThat(nurse.getNationalId()).isEqualTo("29901011234567");
        assertThat(nurse.getLicenseNumber()).isEqualTo("LIC-001");
        assertThat(nurse.getSpecialization()).isEqualTo("Elderly Care");
        assertThat(nurse.getYearsOfExperience()).isEqualTo(5);
        assertThat(nurse.getBio()).isEqualTo("Five years of home care");
    }

    @Test
    void toResponse_mapsNurseUserAndServices() {
        Nurse nurse = nurseWithUser();
        nurse.setRejectionDetail(NurseRejectionDetail.builder()
                .overallReason("Missing documents")
                .failedSteps(List.of(new FailedStep("LICENSE", "Image unreadable")))
                .build());
        NurseServiceResponse service = NurseServiceResponse.builder()
                .id(UUID.randomUUID())
                .serviceTypeId(UUID.randomUUID())
                .serviceName("General Nursing")
                .build();

        NurseResponse response = mapper.toResponse(nurse, List.of(service));

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(nurse.getId());
        assertThat(response.getUserId()).isEqualTo(nurse.getUser().getId());
        assertThat(response.getFirstName()).isEqualTo("Mai");
        assertThat(response.getLastName()).isEqualTo("Elsayed");
        assertThat(response.getPhoneNumber()).isEqualTo("+201002003004");
        assertThat(response.getProfileImageUrl()).isEqualTo("https://img.example/mai.png");
        assertThat(response.getNationalId()).isEqualTo("29901011234567");
        assertThat(response.getNationalIdFrontUrl()).isEqualTo("https://img.example/front.png");
        assertThat(response.getNationalIdBackUrl()).isEqualTo("https://img.example/back.png");
        assertThat(response.getLicenseImageUrl()).isEqualTo("https://img.example/license.png");
        assertThat(response.getProfessionalCertificateUrl()).isEqualTo("https://img.example/cert.png");
        assertThat(response.getSpecialization()).isEqualTo("Elderly Care");
        assertThat(response.getYearsOfExperience()).isEqualTo(5);
        assertThat(response.getBio()).isEqualTo("Five years of home care");
        assertThat(response.getRatingAvg()).isEqualByComparingTo("4.70");
        assertThat(response.getTotalReviews()).isEqualTo(20);
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(response.getRejectionReason()).isEqualTo("Approved");
        assertThat(response.getRejectionDetails()).isNotNull();
        assertThat(response.getRejectionDetails().overallReason()).isEqualTo("Missing documents");
        assertThat(response.getRejectionDetails().failedSteps())
                .containsExactly(new FailedStep("LICENSE", "Image unreadable"));
        assertThat(response.getServices()).isNotSameAs(List.of(service))
                .containsExactly(service);
    }

    @Test
    void toResponse_nullServices_yieldsNullServices() {
        NurseResponse response = mapper.toResponse(nurseWithUser(), null);

        assertThat(response.getServices()).isNull();
    }

    @Test
    void toResponse_nullNurse_withServices_setsOnlyServices() {
        NurseServiceResponse service = NurseServiceResponse.builder().id(UUID.randomUUID()).build();

        NurseResponse response = mapper.toResponse(null, List.of(service));

        assertThat(response.getServices()).containsExactly(service);
        assertThat(response.getId()).isNull();
        assertThat(response.getUserId()).isNull();
        assertThat(response.getFirstName()).isNull();
    }

    @Test
    void toResponse_nullNurseAndServices_returnsNull() {
        assertThat(mapper.toResponse(null, null)).isNull();
    }

    @Test
    void toResponse_nurseWithoutUserAndRejectionDetails_yieldsNullFields() {
        Nurse nurse = Nurse.builder().id(UUID.randomUUID()).build();

        NurseResponse response = mapper.toResponse(nurse, null);

        assertThat(response.getUserId()).isNull();
        assertThat(response.getFirstName()).isNull();
        assertThat(response.getLastName()).isNull();
        assertThat(response.getPhoneNumber()).isNull();
        assertThat(response.getProfileImageUrl()).isNull();
        assertThat(response.getRejectionDetails()).isNull();
        assertThat(response.getId()).isEqualTo(nurse.getId());
    }

    @Test
    void toSimpleResponse_mapsNurseAndUser() {
        Nurse nurse = nurseWithUser();

        NurseResponse response = mapper.toSimpleResponse(nurse);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(nurse.getId());
        assertThat(response.getUserId()).isEqualTo(nurse.getUser().getId());
        assertThat(response.getFirstName()).isEqualTo("Mai");
        assertThat(response.getLastName()).isEqualTo("Elsayed");
        assertThat(response.getPhoneNumber()).isEqualTo("+201002003004");
        assertThat(response.getProfileImageUrl()).isEqualTo("https://img.example/mai.png");
        assertThat(response.getNationalId()).isEqualTo("29901011234567");
        assertThat(response.getSpecialization()).isEqualTo("Elderly Care");
        assertThat(response.getRatingAvg()).isEqualByComparingTo("4.70");
        assertThat(response.getTotalReviews()).isEqualTo(20);
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(response.getServices()).isNull();
        assertThat(response.getRejectionDetails()).isNull();
    }

    @Test
    void toSimpleResponse_nullUser_yieldsNullUserFields() {
        Nurse nurse = Nurse.builder().id(UUID.randomUUID()).build();

        NurseResponse response = mapper.toSimpleResponse(nurse);

        assertThat(response.getUserId()).isNull();
        assertThat(response.getFirstName()).isNull();
        assertThat(response.getLastName()).isNull();
        assertThat(response.getPhoneNumber()).isNull();
        assertThat(response.getProfileImageUrl()).isNull();
    }

    @Test
    void toSimpleResponse_nullNurse_returnsNull() {
        assertThat(mapper.toSimpleResponse(null)).isNull();
    }

    @Test
    void toRejectionDetailsResponse_mapsAllFields() {
        NurseRejectionDetail detail = NurseRejectionDetail.builder()
                .overallReason("Rejected due to invalid license")
                .failedSteps(List.of(
                        new FailedStep("LICENSE", "Expired"),
                        new FailedStep("ID", "Blurry")
                ))
                .build();

        NurseRejectionDetailsResponse response = mapper.toRejectionDetailsResponse(detail);

        assertThat(response).isNotNull();
        assertThat(response.overallReason()).isEqualTo("Rejected due to invalid license");
        assertThat(response.failedSteps())
                .containsExactly(new FailedStep("LICENSE", "Expired"), new FailedStep("ID", "Blurry"));
    }

    @Test
    void toRejectionDetailsResponse_defaultFailedSteps_yieldsEmptyList() {
        NurseRejectionDetail detail = NurseRejectionDetail.builder().overallReason("X").build();

        assertThat(mapper.toRejectionDetailsResponse(detail).failedSteps()).isNotNull().isEmpty();
    }

    @Test
    void toRejectionDetailsResponse_nullFailedSteps_yieldsNullList() {
        NurseRejectionDetail detail = NurseRejectionDetail.builder().overallReason("X").build();
        detail.setFailedSteps(null);

        assertThat(mapper.toRejectionDetailsResponse(detail).failedSteps()).isNull();
    }

    @Test
    void toRejectionDetailsResponse_nullDetail_returnsNull() {
        assertThat(mapper.toRejectionDetailsResponse(null)).isNull();
    }

    @Test
    void toServiceResponse_mapsAllFields() {
        UUID serviceTypeId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        ServiceType serviceType = ServiceType.builder()
                .id(serviceTypeId)
                .name("General Nursing")
                .description("Basic care")
                .basePrice(new BigDecimal("150.00"))
                .build();
        NurseService link = NurseService.builder()
                .id(id)
                .serviceType(serviceType)
                .isActive(true)
                .build();

        NurseServiceResponse response = mapper.toServiceResponse(link);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getServiceTypeId()).isEqualTo(serviceTypeId);
        assertThat(response.getServiceName()).isEqualTo("General Nursing");
        assertThat(response.getServiceDescription()).isEqualTo("Basic care");
        assertThat(response.getBasePrice()).isEqualByComparingTo("150.00");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void toServiceResponse_nullServiceType_yieldsNullServiceTypeFields() {
        NurseService link = NurseService.builder().id(UUID.randomUUID()).serviceType(null).build();

        NurseServiceResponse response = mapper.toServiceResponse(link);

        assertThat(response.getServiceTypeId()).isNull();
        assertThat(response.getServiceName()).isNull();
        assertThat(response.getServiceDescription()).isNull();
        assertThat(response.getBasePrice()).isNull();
    }

    @Test
    void toServiceResponse_nullLink_returnsNull() {
        assertThat(mapper.toServiceResponse(null)).isNull();
    }
}
