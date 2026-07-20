package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.ReviewRatingRequest;
import iti.jets.java.homenursing.dto.ReviewRatingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewRatingService {

    Page<ReviewRatingResponse> listByNurse(UUID nurseId, Pageable pageable);

    ReviewRatingResponse getById(UUID id);

    ReviewRatingResponse create(UUID userId, ReviewRatingRequest request);

    ReviewRatingResponse update(UUID id, UUID userId, ReviewRatingRequest request);

    void delete(UUID id, UUID userId);
}
