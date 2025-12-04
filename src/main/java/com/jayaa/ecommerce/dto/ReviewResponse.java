package com.jayaa.ecommerce.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {

    private Long id;
    private UserInfo user;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String fullName;
    }
}