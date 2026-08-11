package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurse.NurseRegistrationRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceBatchResult;
import iti.jets.java.homenursing.dto.nurse.NurseServiceRequest;
import iti.jets.java.homenursing.dto.nurse.NurseServiceResponse;
import iti.jets.java.homenursing.dto.nurse.NurseUpdateRequest;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseService;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseMapper;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.impl.NurseServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

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
class NurseServiceImplTest {

    @Mock
    private NurseRepository nurseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ServiceTypeRepository serviceTypeRepository;
    @Mock
    private NurseServiceRepository nurseServiceRepository;
    @Mock
    private NurseMapper nurseMapper;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private NurseServiceImpl nurseService;

    private static final UUID NURSE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private static User user() {
        return User.builder().id(USER_ID).build();
    }

    private static Nurse nurse(VerificationStatus status) {
        return Nurse.builder()
                .id(NURSE_ID)
                .user(user())
                .nationalId("old-national")
                .verificationStatus(status)
                .build();
    }

    private static NurseRegistrationRequest registrationRequest(String nationalId, MultipartFile[] docs, MultipartFile image) {
        return NurseRegistrationRequest.builder()
                .nationalId(nationalId)
                .licenseNumber("LIC-1")
                .nationalIdFront(docs[0])
                .nationalIdBack(docs[1])
                .licenseImage(docs[2])
                .professionalCertificate(docs[3])
                .specialization("General")
                .yearsOfExperience(5)
                .bio("bio")
                .profileImage(image)
                .build();
    }

    @Test
    void registerNewNurseUploadsDocumentsAndImage() {
        User savedUser = user(); when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        MultipartFile[] docs = {
                new MockMultipartFile("front", "a.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("back", "b.jpg", "image/jpeg", new byte[]{2}),
                new MockMultipartFile("lic", "c.jpg", "image/jpeg", new byte[]{3}),
                new MockMultipartFile("cert", "d.jpg", "image/jpeg", new byte[]{4}),
        };
        MultipartFile image = new MockMultipartFile("img", "e.jpg", "image/jpeg", new byte[]{5});
        NurseRegistrationRequest request = registrationRequest("national-1", docs, image);
        Nurse newNurse = Nurse.builder().id(NURSE_ID).build();
        when(nurseRepository.existsByNationalId("national-1")).thenReturn(false);
        when(nurseMapper.toEntity(request, savedUser)).thenReturn(newNurse);
        when(cloudinaryService.upload(any(MultipartFile.class))).thenReturn("cloud-url");
        when(nurseRepository.save(newNurse)).thenReturn(newNurse);
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of());
        when(nurseMapper.toResponse(eq(newNurse), any())).thenReturn(NurseResponse.builder().id(NURSE_ID).build());

        NurseResponse result = nurseService.register(USER_ID, request);

        assertThat(result.getId()).isEqualTo(NURSE_ID);
        verify(cloudinaryService).upload(docs[0]);
        verify(cloudinaryService).upload(docs[1]);
        verify(cloudinaryService).upload(docs[2]);
        verify(cloudinaryService).upload(docs[3]);
        verify(cloudinaryService).upload(image);
        verify(userRepository).save(savedUser);
    }

    @Test
    void registerNewNurseWithNullNationalIdSkipsDuplicateCheck() {
        User savedUser = user(); when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        NurseRegistrationRequest request = registrationRequest(null, new MultipartFile[4], null);
        Nurse newNurse = Nurse.builder().id(NURSE_ID).build();
        when(nurseMapper.toEntity(request, savedUser)).thenReturn(newNurse);
        when(nurseRepository.save(newNurse)).thenReturn(newNurse);
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of());
        when(nurseMapper.toResponse(eq(newNurse), any())).thenReturn(NurseResponse.builder().id(NURSE_ID).build());

        nurseService.register(USER_ID, request);

        verify(nurseRepository, never()).existsByNationalId(any());
    }

    @Test
    void registerNewNurseWithDuplicateNationalIdThrows() {
        User savedUser = user(); when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        NurseRegistrationRequest request = registrationRequest("dup", new MultipartFile[4], null);
        when(nurseRepository.existsByNationalId("dup")).thenReturn(true);

        assertThatThrownBy(() -> nurseService.register(USER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("National ID already exists");
    }

    @Test
    void registerExistingNurseUpdatesFieldsAndSkipsDuplicateCheckForSameNationalId() {
        User savedUser = user(); when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
        Nurse existing = nurse(VerificationStatus.UNDER_REVIEW);
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(existing));
        NurseRegistrationRequest request = registrationRequest("old-national", new MultipartFile[4], null);
        when(nurseRepository.save(existing)).thenReturn(existing);
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of());
        when(nurseMapper.toResponse(eq(existing), any())).thenReturn(NurseResponse.builder().id(NURSE_ID).build());

        nurseService.register(USER_ID, request);

        assertThat(existing.getLicenseNumber()).isEqualTo("LIC-1");
        assertThat(existing.getSpecialization()).isEqualTo("General");
        assertThat(existing.getYearsOfExperience()).isEqualTo(5);
        assertThat(existing.getBio()).isEqualTo("bio");
        verify(nurseRepository, never()).existsByNationalId(any());
        verify(nurseMapper, never()).toEntity(any(), any());
    }

    @Test
    void registerExistingNurseWithDifferentConflictingNationalIdThrows() {
        User savedUser = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(nurse(VerificationStatus.UNDER_REVIEW)));
        NurseRegistrationRequest request = registrationRequest("different", new MultipartFile[4], null);
        when(nurseRepository.existsByNationalId("different")).thenReturn(true);

        assertThatThrownBy(() -> nurseService.register(USER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("National ID already exists");
    }

    @Test
    void registerExistingNurseWithNullNationalIdSkipsDuplicateCheck() {
        User savedUser = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
        Nurse existing = nurse(VerificationStatus.UNDER_REVIEW);
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(existing));
        NurseRegistrationRequest request = registrationRequest(null, new MultipartFile[4], null);
        when(nurseRepository.save(existing)).thenReturn(existing);
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of());
        when(nurseMapper.toResponse(eq(existing), any())).thenReturn(NurseResponse.builder().id(NURSE_ID).build());

        nurseService.register(USER_ID, request);

        verify(nurseRepository, never()).existsByNationalId(any());
    }

    @Test
    void registerExistingNurseWithDifferentAvailableNationalIdUpdates() {
        User savedUser = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
        Nurse existing = nurse(VerificationStatus.UNDER_REVIEW);
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(existing));
        NurseRegistrationRequest request = registrationRequest("new-national", new MultipartFile[4], null);
        when(nurseRepository.existsByNationalId("new-national")).thenReturn(false);
        when(nurseRepository.save(existing)).thenReturn(existing);
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of());
        when(nurseMapper.toResponse(eq(existing), any())).thenReturn(NurseResponse.builder().id(NURSE_ID).build());

        nurseService.register(USER_ID, request);

        assertThat(existing.getNationalId()).isEqualTo("new-national");
    }

    @Test
    void registerWhenUserNotFoundThrows() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nurseService.register(USER_ID, registrationRequest("n", new MultipartFile[4], null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateProfileWhenRejectedResetsToUnderReviewAndClearsRejection() {
        Nurse rejected = nurse(VerificationStatus.REJECTED);
        rejected.setRejectionReason("bad docs");
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(rejected));
        MultipartFile front = new MockMultipartFile("front", "a.jpg", "image/jpeg", new byte[]{1});
        MultipartFile image = new MockMultipartFile("img", "b.jpg", "image/jpeg", new byte[]{2});
        NurseUpdateRequest request = NurseUpdateRequest.builder()
                .nationalId("n2")
                .licenseNumber("LIC-2")
                .nationalIdFront(front)
                .nationalIdBack(null)
                .licenseImage(null)
                .professionalCertificate(null)
                .profileImage(image)
                .build();
        when(cloudinaryService.upload(any(MultipartFile.class))).thenReturn("cloud-url");
        when(nurseRepository.save(rejected)).thenReturn(rejected);
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of());
        when(nurseMapper.toResponse(eq(rejected), any())).thenReturn(NurseResponse.builder().id(NURSE_ID).build());

        nurseService.updateProfile(NURSE_ID, USER_ID, request);

        assertThat(rejected.getVerificationStatus()).isEqualTo(VerificationStatus.UNDER_REVIEW);
        assertThat(rejected.getRejectionReason()).isNull();
        assertThat(rejected.getRejectionDetail()).isNull();
        verify(nurseMapper).updateEntity(request, rejected);
        verify(cloudinaryService).upload(front);
        verify(cloudinaryService).upload(image);
        verify(userRepository).save(rejected.getUser());
    }

    @Test
    void updateProfileWhenNotRejectedKeepsStatus() {
        Nurse approved = nurse(VerificationStatus.APPROVED);
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(approved));
        NurseUpdateRequest request = NurseUpdateRequest.builder()
                .nationalId("old-national")
                .nationalIdBack(new MockMultipartFile("back", new byte[0]))
                .build();
        when(nurseRepository.save(approved)).thenReturn(approved);
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of());
        when(nurseMapper.toResponse(eq(approved), any())).thenReturn(NurseResponse.builder().id(NURSE_ID).build());

        nurseService.updateProfile(NURSE_ID, USER_ID, request);

        assertThat(approved.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        verify(cloudinaryService, never()).upload(any(MultipartFile.class));
    }

    @Test
    void updateProfileWhenNotOwnedThrows() {
        Nurse other = Nurse.builder()
                .id(NURSE_ID)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> nurseService.updateProfile(NURSE_ID, USER_ID, NurseUpdateRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse not found");
    }

    @Test
    void updateProfileWhenNurseMissingThrows() {
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nurseService.updateProfile(NURSE_ID, USER_ID, NurseUpdateRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse not found");
    }

    @Test
    void getProfileReturnsMappedResponseWithSortedServices() {
        Nurse nurse = nurse(VerificationStatus.APPROVED);
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(nurse));
        NurseService nursingLink = NurseService.builder().id(UUID.randomUUID()).build();
        NurseService physioLink = NurseService.builder().id(UUID.randomUUID()).build();
        when(nurseServiceRepository.findAllByNurse_Id(NURSE_ID)).thenReturn(List.of(physioLink, nursingLink));
        NurseServiceResponse nursing = NurseServiceResponse.builder().serviceName("Nursing").build();
        NurseServiceResponse physio = NurseServiceResponse.builder().serviceName("Physio").build();
        when(nurseMapper.toServiceResponse(nursingLink)).thenReturn(nursing);
        when(nurseMapper.toServiceResponse(physioLink)).thenReturn(physio);
        when(nurseMapper.toResponse(eq(nurse), any())).thenAnswer(inv -> NurseResponse.builder()
                .id(NURSE_ID)
                .services(inv.getArgument(1))
                .build());

        NurseResponse result = nurseService.getProfile(NURSE_ID);

        assertThat(result.getId()).isEqualTo(NURSE_ID);
        assertThat(result.getServices()).extracting(NurseServiceResponse::getServiceName)
                .containsExactly("Nursing", "Physio");
    }

    @Test
    void getProfileWhenNurseMissingThrows() {
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nurseService.getProfile(NURSE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse not found");
    }

    @Test
    void listNursesMapsAllToSimpleResponses() {
        Nurse first = nurse(VerificationStatus.APPROVED);
        Nurse second = nurse(VerificationStatus.UNDER_REVIEW);
        when(nurseRepository.findAll()).thenReturn(List.of(first, second));
        when(nurseMapper.toSimpleResponse(first)).thenReturn(NurseResponse.builder().id(NURSE_ID).build());
        when(nurseMapper.toSimpleResponse(second)).thenReturn(NurseResponse.builder().id(NURSE_ID).build());

        List<NurseResponse> result = nurseService.listNurses();

        assertThat(result).hasSize(2);
    }

    @Test
    void addServicesAddsNewLinksAndReportsFailures() {
        Nurse nurse = nurse(VerificationStatus.APPROVED);
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(nurse));

        UUID typeA = UUID.randomUUID();
        UUID typeB = UUID.randomUUID();
        ServiceType serviceTypeA = ServiceType.builder().id(typeA).name("Nursing").build();
        when(serviceTypeRepository.findById(typeA)).thenReturn(Optional.of(serviceTypeA));
        when(serviceTypeRepository.findById(typeB)).thenReturn(Optional.empty());
        NurseService linkA = NurseService.builder().id(UUID.randomUUID()).nurse(nurse).serviceType(serviceTypeA).build();
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, typeA)).thenReturn(Optional.empty());
        when(nurseServiceRepository.save(any(NurseService.class))).thenReturn(linkA);
        when(nurseMapper.toServiceResponse(linkA)).thenReturn(NurseServiceResponse.builder().serviceName("Nursing").build());

        List<NurseServiceRequest> requests = List.of(
                NurseServiceRequest.builder().serviceTypeId(typeA).build(),
                NurseServiceRequest.builder().serviceTypeId(typeA).build(),
                NurseServiceRequest.builder().serviceTypeId(typeB).build(),
                NurseServiceRequest.builder().serviceTypeId(null).build()
        );

        NurseServiceBatchResult result = nurseService.addServices(NURSE_ID, USER_ID, requests);

        assertThat(result.getAdded()).hasSize(1);
        assertThat(result.getFailed()).hasSize(2);
        assertThat(result.getFailed()).anyMatch(f -> f.getReason().equals("Service type not found"));
        assertThat(result.getFailed()).anyMatch(f -> f.getReason().equals("Service type id is required"));
        verify(nurseServiceRepository).save(any(NurseService.class));
    }

    @Test
    void addServicesReactivatesExistingLink() {
        Nurse nurse = nurse(VerificationStatus.APPROVED);
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(nurse));
        UUID typeA = UUID.randomUUID();
        ServiceType serviceTypeA = ServiceType.builder().id(typeA).name("Nursing").build();
        when(serviceTypeRepository.findById(typeA)).thenReturn(Optional.of(serviceTypeA));
        NurseService existing = NurseService.builder().id(UUID.randomUUID()).isActive(false).build();
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, typeA)).thenReturn(Optional.of(existing));
        when(nurseServiceRepository.save(existing)).thenReturn(existing);
        when(nurseMapper.toServiceResponse(existing)).thenReturn(NurseServiceResponse.builder().serviceName("Nursing").build());

        NurseServiceBatchResult result = nurseService.addServices(NURSE_ID, USER_ID,
                List.of(NurseServiceRequest.builder().serviceTypeId(typeA).build()));

        assertThat(result.getAdded()).hasSize(1);
        assertThat(existing.getIsActive()).isTrue();
    }

    @Test
    void addServicesWhenNurseNotOwnedThrows() {
        Nurse other = Nurse.builder()
                .id(NURSE_ID)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> nurseService.addServices(NURSE_ID, USER_ID, List.of()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse not found");
    }

    @Test
    void removeServiceDeactivatesFoundLink() {
        Nurse nurse = nurse(VerificationStatus.APPROVED);
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(nurse));
        UUID typeA = UUID.randomUUID();
        NurseService link = NurseService.builder().id(UUID.randomUUID()).isActive(true).build();
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, typeA)).thenReturn(Optional.of(link));
        when(nurseServiceRepository.save(link)).thenReturn(link);

        nurseService.removeService(NURSE_ID, USER_ID, typeA);

        assertThat(link.getIsActive()).isFalse();
        verify(nurseServiceRepository).save(link);
    }

    @Test
    void removeServiceWhenLinkMissingThrows() {
        Nurse nurse = nurse(VerificationStatus.APPROVED);
        UUID typeA = UUID.randomUUID();
        when(nurseRepository.findById(NURSE_ID)).thenReturn(Optional.of(nurse));
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, typeA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nurseService.removeService(NURSE_ID, USER_ID, typeA))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse service not found");
    }

    @Test
    void findVerifiedNursesByServiceTypeNameFiltersApprovedAndDistinct() {
        Nurse approved = nurse(VerificationStatus.APPROVED);
        Nurse pending = nurse(VerificationStatus.UNDER_REVIEW);
        NurseService approvedLink = NurseService.builder().nurse(approved).build();
        NurseService approvedLinkDup = NurseService.builder().nurse(approved).build();
        NurseService pendingLink = NurseService.builder().nurse(pending).build();
        when(nurseServiceRepository.findByServiceType_NameContainingIgnoreCaseAndIsActiveTrue("Nursing"))
                .thenReturn(List.of(approvedLink, approvedLinkDup, pendingLink));
        when(nurseMapper.toSimpleResponse(approved)).thenReturn(NurseResponse.builder().id(NURSE_ID).build());

        List<NurseResponse> result = nurseService.findVerifiedNursesByServiceTypeName("Nursing");

        assertThat(result).hasSize(1);
        verify(nurseMapper, never()).toSimpleResponse(pending);
    }
}




