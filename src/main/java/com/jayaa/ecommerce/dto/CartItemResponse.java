package com.jayaa.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemResponse {

    private Long id;
    private ProductInfo product;
    private Integer quantity;
    private BigDecimal subtotal; // quantity * product price
    private LocalDateTime addedAt;

    @Data
    public static class ProductInfo {
        private Long id;
        private String name;
        private String slug;
        private BigDecimal price;
        private Integer stockQuantity;
        private String imageUrl;
    }
}