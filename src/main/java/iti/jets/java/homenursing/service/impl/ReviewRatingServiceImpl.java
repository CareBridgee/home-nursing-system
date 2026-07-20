package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ReviewRatingRequest;
import iti.jets.java.homenursing.dto.ReviewRatingResponse;
import iti.jets.java.homenursing.entity.Booking;
import iti.jets.java.homenursing.entity.ReviewRating;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.BookingStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ReviewRatingMapper;
import iti.jets.java.homenursing.repository.BookingRepository;
import iti.jets.java.homenursing.repository.ReviewRatingRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.ReviewRatingService;
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
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewRatingResponse> listByNurse(UUID nurseId, Pageable pageable) {
        return reviewRatingRepository.findByNurseId(nurseId, pageable)
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
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.getBookingId()));

        if (!booking.getProfile().getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only review bookings you own");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("You can only review completed bookings");
        }

        if (reviewRatingRepository.existsByBookingId(booking.getId())) {
            throw new BadRequestException("A review already exists for this booking");
        }

        ReviewRating review = reviewRatingMapper.toEntity(request);
        review.setBooking(booking);
        review.setProfile(booking.getProfile());
        review.setNurse(booking.getNurse());

        ReviewRating saved = reviewRatingRepository.save(review);
        return reviewRatingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReviewRatingResponse update(UUID id, UUID userId, ReviewRatingRequest request) {
        ReviewRating review = loadOwned(id, userId);
        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getReviewText() != null) review.setReviewText(request.getReviewText());
        if (request.getIsAnonymous() != null) review.setIsAnonymous(request.getIsAnonymous());
        ReviewRating saved = reviewRatingRepository.save(review);
        return reviewRatingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        ReviewRating review = loadOwned(id, userId);
        reviewRatingRepository.delete(review);
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
