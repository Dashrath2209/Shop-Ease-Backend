package com.jayaa.ecommerce.controller;

import com.jayaa.ecommerce.dto.ReviewRequest;
import com.jayaa.ecommerce.dto.ReviewResponse;
import com.jayaa.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByProduct(
            @PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }

    @PostMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request
    ) {
        ReviewResponse review = reviewService.addReview(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @PutMapping("/api/reviews/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateReview(id, request));
    }

    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}