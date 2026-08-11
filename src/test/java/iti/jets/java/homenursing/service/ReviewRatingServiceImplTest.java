package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.review.ReviewRatingRequest;
import iti.jets.java.homenursing.dto.review.ReviewRatingResponse;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ReviewRating;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ReviewRatingMapper;
import iti.jets.java.homenursing.repository.ReviewRatingRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.impl.ReviewRatingServiceImpl;
import iti.jets.java.homenursing.util.NurseRatingUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewRatingServiceImplTest {

    @Mock
    private ReviewRatingRepository reviewRatingRepository;
    @Mock
    private ReviewRatingMapper reviewRatingMapper;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NurseRatingUpdater nurseRatingUpdater;

    private ReviewRatingServiceImpl service;

    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID_2 = UUID.randomUUID();
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID NURSE_ID = UUID.randomUUID();
    private static final UUID PATIENT_USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    @BeforeEach
    void setUp() {
        service = new ReviewRatingServiceImpl(
                reviewRatingRepository, reviewRatingMapper, serviceRequestRepository,
                userRepository, nurseRatingUpdater);
    }

    private User user(UUID id) {
        return User.builder().id(id).firstName("Mona").lastName("Ali").build();
    }

    private Profile profile() {
        return Profile.builder().id(PROFILE_ID).user(user(PATIENT_USER_ID)).firstName("Mona").build();
    }

    private Nurse nurse() {
        return Nurse.builder()
                .id(NURSE_ID)
                .user(User.builder().id(UUID.randomUUID()).build())
                .verificationStatus(VerificationStatus.APPROVED)
                .build();
    }

    private ServiceRequest completedRequest() {
        return ServiceRequest.builder()
                .id(REQ_ID)
                .profile(profile())
                .serviceType(ServiceType.builder()
                        .id(UUID.randomUUID()).name("Home Nursing").basePrice(new BigDecimal("500")).build())
                .nurse(nurse())
                .status(ServiceRequestStatus.COMPLETED)
                .isDeleted(false)
                .createdAt(NOW)
                .build();
    }

    private ReviewRating review() {
        return ReviewRating.builder()
                .id(REVIEW_ID)
                .serviceRequest(completedRequest())
                .profile(profile())
                .nurse(nurse())
                .rating(4)
                .reviewText("Good care")
                .isAnonymous(false)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private ReviewRatingRequest request(UUID serviceRequestId, Integer rating, String text, Boolean anonymous) {
        return ReviewRatingRequest.builder()
                .serviceRequestId(serviceRequestId)
                .rating(rating)
                .reviewText(text)
                .isAnonymous(anonymous)
                .build();
    }

    private ReviewRatingResponse response(UUID id) {
        return ReviewRatingResponse.builder()
                .id(id)
                .serviceRequestId(REQ_ID)
                .profileId(PROFILE_ID)
                .nurseId(NURSE_ID)
                .rating(4)
                .reviewText("Good care")
                .isAnonymous(false)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    @Test
    void listByNurse_returnsMappedPage() {
        ReviewRatingResponse first = response(REVIEW_ID);
        ReviewRatingResponse second = response(REVIEW_ID_2);
        when(reviewRatingRepository.findByNurseId(eq(NURSE_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review(), review())));
        when(reviewRatingMapper.toResponse(any(ReviewRating.class))).thenReturn(first, second);

        Page<ReviewRatingResponse> result = service.listByNurse(NURSE_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals(first, result.getContent().get(0));
        assertEquals(second, result.getContent().get(1));
    }

    @Test
    void listByNurse_emptyPage_returnsEmpty() {
        when(reviewRatingRepository.findByNurseId(eq(NURSE_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ReviewRatingResponse> result = service.listByNurse(NURSE_ID, PageRequest.of(0, 10));

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getById_returnsMapped() {
        ReviewRatingResponse response = response(REVIEW_ID);
        when(reviewRatingRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review()));
        when(reviewRatingMapper.toResponse(any(ReviewRating.class))).thenReturn(response);

        ReviewRatingResponse result = service.getById(REVIEW_ID);

        assertEquals(response, result);
    }

    @Test
    void getById_notFound_throws() {
        when(reviewRatingRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                service.getById(REVIEW_ID));

        assertTrue(ex.getMessage().contains("Review not found"));
    }

    @Test
    void create_happy_createsAndUpdatesNurseRating() {
        reviewRatingRepositoryStubs();

        service.create(PATIENT_USER_ID, request(REQ_ID, 4, "Good care", null));

        ArgumentCaptor<ReviewRating> captor = ArgumentCaptor.forClass(ReviewRating.class);
        verify(reviewRatingRepository).save(captor.capture());
        assertEquals(REQ_ID, captor.getValue().getServiceRequest().getId());
        assertEquals(PROFILE_ID, captor.getValue().getProfile().getId());
        assertEquals(NURSE_ID, captor.getValue().getNurse().getId());
        verify(nurseRatingUpdater).onReviewCreated(captor.getValue().getNurse(), 4);
    }

    private void reviewRatingRepositoryStubs() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID))
                .thenReturn(Optional.of(completedRequest()));
        when(reviewRatingRepository.existsByServiceRequestId(REQ_ID)).thenReturn(false);
        when(reviewRatingMapper.toEntity(any(ReviewRatingRequest.class))).thenReturn(review());
        when(reviewRatingRepository.save(any(ReviewRating.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRatingMapper.toResponse(any(ReviewRating.class))).thenReturn(response(REVIEW_ID));
    }

    @Test
    void create_requestNotFound_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                service.create(PATIENT_USER_ID, request(REQ_ID, 4, "x", null)));

        assertTrue(ex.getMessage().contains("Service request not found"));
    }

    @Test
    void create_notOwner_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID))
                .thenReturn(Optional.of(completedRequest()));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.create(OTHER_USER_ID, request(REQ_ID, 4, "x", null)));

        assertEquals("You can only review requests you own", ex.getMessage());
    }

    @Test
    void create_notCompleted_throws() {
        ServiceRequest inProgress = ServiceRequest.builder()
                .id(REQ_ID).profile(profile()).serviceType(completedRequest().getServiceType())
                .nurse(nurse()).status(ServiceRequestStatus.IN_PROGRESS).isDeleted(false).build();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(inProgress));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.create(PATIENT_USER_ID, request(REQ_ID, 4, "x", null)));

        assertEquals("You can only review completed requests", ex.getMessage());
    }

    @Test
    void create_reviewAlreadyExists_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID))
                .thenReturn(Optional.of(completedRequest()));
        when(reviewRatingRepository.existsByServiceRequestId(REQ_ID)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.create(PATIENT_USER_ID, request(REQ_ID, 4, "x", null)));

        assertEquals("A review already exists for this request", ex.getMessage());
        verify(reviewRatingRepository, never()).save(any());
    }

    @Test
    void update_happy_updatesRatingTextAndAnonymous() {
        ReviewRating existing = review();
        when(reviewRatingRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existing));
        when(reviewRatingRepository.save(any(ReviewRating.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRatingMapper.toResponse(any(ReviewRating.class))).thenReturn(response(REVIEW_ID));

        service.update(REVIEW_ID, PATIENT_USER_ID, request(REQ_ID, 5, "Great!", true));

        assertEquals(5, existing.getRating());
        assertEquals("Great!", existing.getReviewText());
        assertEquals(Boolean.TRUE, existing.getIsAnonymous());
        verify(nurseRatingUpdater).onReviewUpdated(existing.getNurse(), 4, 5);
    }

    @Test
    void update_noRatingFields_keepsRatingAndSkipsUpdater() {
        ReviewRating existing = review();
        when(reviewRatingRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existing));
        when(reviewRatingRepository.save(any(ReviewRating.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRatingMapper.toResponse(any(ReviewRating.class))).thenReturn(response(REVIEW_ID));

        service.update(REVIEW_ID, PATIENT_USER_ID, request(REQ_ID, null, null, null));

        assertEquals(4, existing.getRating());
        assertEquals("Good care", existing.getReviewText());
        verify(nurseRatingUpdater, never()).onReviewUpdated(any(), anyInt(), anyInt());
    }

    @Test
    void update_notFound_throws() {
        when(reviewRatingRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.update(REVIEW_ID, PATIENT_USER_ID, request(REQ_ID, 4, "x", null)));
    }

    @Test
    void update_notOwner_throws() {
        ReviewRating otherOwnersReview = ReviewRating.builder()
                .id(REVIEW_ID)
                .serviceRequest(completedRequest())
                .profile(Profile.builder().id(PROFILE_ID).user(user(OTHER_USER_ID)).build())
                .nurse(nurse())
                .rating(4)
                .build();
        when(reviewRatingRepository.findById(REVIEW_ID)).thenReturn(Optional.of(otherOwnersReview));

        assertThrows(ResourceNotFoundException.class, () ->
                service.update(REVIEW_ID, PATIENT_USER_ID, request(REQ_ID, 4, "x", null)));
    }

    @Test
    void delete_happy_deletesAndUpdatesNurseRating() {
        ReviewRating existing = review();
        when(reviewRatingRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existing));

        service.delete(REVIEW_ID, PATIENT_USER_ID);

        verify(reviewRatingRepository).delete(existing);
        verify(nurseRatingUpdater).onReviewDeleted(existing.getNurse(), 4);
    }

    @Test
    void delete_notFound_throws() {
        when(reviewRatingRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(REVIEW_ID, PATIENT_USER_ID));
        verify(reviewRatingRepository, never()).delete(any());
    }
}