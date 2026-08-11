package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurse.NurseRejectionRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceResponse;
import iti.jets.java.homenursing.entity.FailedStep;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseRejectionDetail;
import iti.jets.java.homenursing.entity.NurseService;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseMapper;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.service.impl.AdminNurseServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AdminNurseServiceImplTest {

    @Mock
    private NurseRepository nurseRepository;
    @Mock
    private NurseServiceRepository nurseServiceRepository;
    @Mock
    private NurseMapper nurseMapper;

    @InjectMocks
    private AdminNurseServiceImpl adminNurseService;

    private static final UUID NURSE_ID = UUID.randomUUID();

    private static Nurse nurse(String nationalId, VerificationStatus status) {
        return Nurse.builder()
                .id(NURSE_ID)
                .nationalId(nationalId)
                .verificationStatus(status)
                .build();
    }

    private static NurseServiceResponse serviceResponse(String name) {
        return NurseServiceResponse.builder()
                .id(UUID.randomUUID())
                .serviceTypeId(UUID.randomUUID())
                .serviceName(name)
                .basePrice(new BigDecimal("100.00"))
                .isActive(true)
                .build();
    }

    private static NurseService nurseService() {
        return NurseService.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void listByStatusMapsNursesWithServicesSortedByName() {
        Nurse first = nurse("N1", VerificationStatus.UNDER_REVIEW);
        Nurse second = nurse("N2", VerificationStatus.UNDER_REVIEW);
        when(nurseRepository.findByVerificationStatus(VerificationStatus.UNDER_REVIEW))
                .thenReturn(List.of(first, second));

        NurseService linkA = nurseService();
        NurseService linkB = nurseService();
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of(linkA, linkB));
        when(nurseMapper.toServiceResponse(linkA)).thenReturn(serviceResponse("Physio"));
        when(nurseMapper.toServiceResponse(linkB)).thenReturn(serviceResponse("Nursing"));
        when(nurseMapper.toResponse(eq(first), any()))
                .thenReturn(NurseResponse.builder().id(NURSE_ID).services(List.of(
                        serviceResponse("Nursing"), serviceResponse("Physio"))).build());
        when(nurseMapper.toResponse(eq(second), any())).thenReturn(NurseResponse.builder().id(UUID.randomUUID()).build());

        List<NurseResponse> responses = adminNurseService.listByStatus(VerificationStatus.UNDER_REVIEW);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getServices()).extracting(NurseServiceResponse::getServiceName)
                .containsExactly("Nursing", "Physio");
    }

    @Test
    void approveSetsApprovedStatusAndClearsRejection() {
        Nurse nurse = nurse("N1", VerificationStatus.UNDER_REVIEW);
        nurse.setRejectionReason("old reason");
        nurse.setRejectionDetail(NurseRejectionDetail.builder().build());
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(nurse));
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of());
        when(nurseRepository.save(nurse)).thenReturn(nurse);
        when(nurseMapper.toResponse(eq(nurse), any())).thenReturn(
                NurseResponse.builder().id(NURSE_ID).verificationStatus(VerificationStatus.APPROVED).build());

        NurseResponse result = adminNurseService.approve(NURSE_ID);

        assertThat(nurse.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(nurse.getRejectionReason()).isNull();
        assertThat(nurse.getRejectionDetail()).isNull();
        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        verify(nurseRepository).save(nurse);
    }

    @Test
    void approveWithoutNationalIdThrows() {
        Nurse nurse = nurse(null, VerificationStatus.UNDER_REVIEW);
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(nurse));

        assertThatThrownBy(() -> adminNurseService.approve(NURSE_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("hasn't completed registration");

        verify(nurseRepository, never()).save(any(Nurse.class));
    }

    @Test
    void approveWhenMissingThrows() {
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNurseService.approve(NURSE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse not found");
    }

    @Test
    void rejectSetsStatusAndBuildsRejectionDetail() {
        Nurse nurse = nurse("N1", VerificationStatus.UNDER_REVIEW);
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(nurse));
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of());
        when(nurseRepository.save(nurse)).thenReturn(nurse);
        when(nurseMapper.toResponse(eq(nurse), any())).thenReturn(
                NurseResponse.builder().id(NURSE_ID).verificationStatus(VerificationStatus.REJECTED).build());

        NurseRejectionRequest request = NurseRejectionRequest.builder()
                .overallReason("Documents invalid")
                .failedSteps(List.of(new FailedStep("national-id", "blurry")))
                .build();

        NurseResponse result = adminNurseService.reject(NURSE_ID, request);

        assertThat(nurse.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(nurse.getRejectionReason()).isEqualTo("Documents invalid");
        verify(nurseRepository).save(nurse);
        assertThat(nurse.getRejectionDetail()).isNotNull();
        assertThat(nurse.getRejectionDetail().getNurse()).isSameAs(nurse);
        assertThat(nurse.getRejectionDetail().getOverallReason()).isEqualTo("Documents invalid");
        assertThat(nurse.getRejectionDetail().getFailedSteps())
                .containsExactly(new FailedStep("national-id", "blurry"));
        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    void rejectWhenMissingThrows() {
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNurseService.reject(NURSE_ID, NurseRejectionRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse not found");
    }
}
