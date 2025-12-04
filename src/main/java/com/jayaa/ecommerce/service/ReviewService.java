package com.jayaa.ecommerce.service;

import com.jayaa.ecommerce.dto.*;
import com.jayaa.ecommerce.exception.*;
import com.jayaa.ecommerce.model.*;
import com.jayaa.ecommerce.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    // ⭐ GET REVIEWS FOR PRODUCT (Public)
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }

        Page<Review> reviews = reviewRepository.findByProductId(productId, pageable);
        return reviews.map(this::convertToResponse);
    }

    // ⭐ ADD REVIEW
    public ReviewResponse addReview(Long productId, ReviewRequest request) {
        Long userId = getCurrentUserId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user already reviewed this product
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new BadRequestException("You have already reviewed this product");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);
        return convertToResponse(saved);
    }

    // ⭐ UPDATE OWN REVIEW
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check ownership
        Long userId = getCurrentUserId();
        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only update your own reviews");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updated = reviewRepository.save(review);
        return convertToResponse(updated);
    }

    // ⭐ DELETE OWN REVIEW
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check ownership
        Long userId = getCurrentUserId();
        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    // ========== HELPER METHODS ==========

    private Long getCurrentUserId() {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Not authenticated");
        }
        return auth.getName();
    }

    private ReviewResponse convertToResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        // User info
        ReviewResponse.UserInfo userInfo = new ReviewResponse.UserInfo();
        userInfo.setId(review.getUser().getId());
        userInfo.setUsername(review.getUser().getUsername());
        userInfo.setFullName(review.getUser().getFullName());
        response.setUser(userInfo);

        return response;
    }

}


