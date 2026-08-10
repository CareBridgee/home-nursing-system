package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ReviewRatingRequest;
import iti.jets.java.homenursing.dto.ReviewRatingResponse;
import iti.jets.java.homenursing.entity.ReviewRating;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ReviewRatingMapper;
import iti.jets.java.homenursing.repository.ReviewRatingRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.NurseRatingUpdater;
import iti.jets.java.homenursing.service.ReviewRatingService;
import iti.jets.java.homenursing.util.SortSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewRatingServiceImpl implements ReviewRatingService {

    private final ReviewRatingRepository reviewRatingRepository;
    private final ReviewRatingMapper reviewRatingMapper;
    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final NurseRatingUpdater nurseRatingUpdater;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewRatingResponse> listByNurse(UUID nurseId, Pageable pageable) {
        return reviewRatingRepository.findByNurseId(
                        nurseId, SortSanitizer.sanitize(pageable, SortSanitizer.asSet(SortSanitizer.REVIEW_RATING_SORTABLE)))
                .map(reviewRatingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewRatingResponse getById(UUID id) {
        ReviewRating review = reviewRatingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        return reviewRatingMapper.toResponse(review);
    }

    @Override
    @Transactional
    public ReviewRatingResponse create(UUID userId, ReviewRatingRequest request) {
        ServiceRequest serviceRequest = serviceRequestRepository
                .findByIdAndIsDeletedFalse(request.getServiceRequestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service request not found: " + request.getServiceRequestId()));

        if (!serviceRequest.getProfile().getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only review requests you own");
        }

        if (serviceRequest.getStatus() != ServiceRequestStatus.COMPLETED) {
            throw new BadRequestException("You can only review completed requests");
        }

        if (reviewRatingRepository.existsByServiceRequestId(serviceRequest.getId())) {
            throw new BadRequestException("A review already exists for this request");
        }

        ReviewRating review = reviewRatingMapper.toEntity(request);
        review.setServiceRequest(serviceRequest);
        review.setProfile(serviceRequest.getProfile());
        review.setNurse(serviceRequest.getNurse());

        ReviewRating saved = reviewRatingRepository.save(review);
        nurseRatingUpdater.onReviewCreated(saved.getNurse(), saved.getRating());
        return reviewRatingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReviewRatingResponse update(UUID id, UUID userId, ReviewRatingRequest request) {
        ReviewRating review = loadOwned(id, userId);
        int oldRating = review.getRating();
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getReviewText() != null) review.setReviewText(request.getReviewText());
        if (request.getIsAnonymous() != null) review.setIsAnonymous(request.getIsAnonymous());
        ReviewRating saved = reviewRatingRepository.save(review);
        if (request.getRating() != null) {
            nurseRatingUpdater.onReviewUpdated(saved.getNurse(), oldRating, saved.getRating());
        }
        return reviewRatingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        ReviewRating review = loadOwned(id, userId);
        reviewRatingRepository.delete(review);
        nurseRatingUpdater.onReviewDeleted(review.getNurse(), review.getRating());
    }

    private ReviewRating loadOwned(UUID id, UUID userId) {
        ReviewRating review = reviewRatingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        if (!review.getProfile().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Review not found: " + id);
        }
        return review;
    }
}
