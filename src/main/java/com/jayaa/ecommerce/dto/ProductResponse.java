package com.jayaa.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String sku;
    private String imageUrl;
    private Boolean isActive;

    // ⭐ Nested categories
    private Set<CategoryInfo> categories;

    // ⭐ Review statistics
    private Double averageRating;
    private Long reviewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nested DTO for categories
    @Data
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String slug;
    }
}