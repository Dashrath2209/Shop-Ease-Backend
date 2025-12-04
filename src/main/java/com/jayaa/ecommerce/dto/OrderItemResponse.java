package com.jayaa.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemResponse {

    private Long id;
    private ProductInfo product;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal subtotal;

    @Data
    public static class ProductInfo {
        private Long id;
        private String name;
        private String slug;
        private String imageUrl;
    }
}