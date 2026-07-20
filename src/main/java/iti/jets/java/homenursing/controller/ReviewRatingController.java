package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ReviewRatingRequest;
import iti.jets.java.homenursing.dto.ReviewRatingResponse;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ReviewRatingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReviewRatingController {

    private final ReviewRatingService reviewRatingService;

    public ReviewRatingController(ReviewRatingService reviewRatingService) {
        this.reviewRatingService = reviewRatingService;
    }

    @GetMapping("/nurses/{nurseId}/reviews")
    public ResponseEntity<Page<ReviewRatingResponse>> listByNurse(
            @PathVariable UUID nurseId, Pageable pageable) {
        return ResponseEntity.ok(reviewRatingService.listByNurse(nurseId, pageable));
    }

    @GetMapping("/reviews/{id}")
    public ResponseEntity<ReviewRatingResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewRatingService.getById(id));
    }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewRatingResponse> create(
            @Valid @RequestBody ReviewRatingRequest request) {
        ReviewRatingResponse response = reviewRatingService.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/reviews/" + response.getId()))
                .body(response);
    }

    @PutMapping("/reviews/{id}")
    public ResponseEntity<ReviewRatingResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRatingRequest request) {
        return ResponseEntity.ok(reviewRatingService.update(id, SecurityUtils.currentUserId(), request));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reviewRatingService.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
