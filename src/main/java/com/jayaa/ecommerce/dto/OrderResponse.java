package com.jayaa.ecommerce.dto;

import com.jayaa.ecommerce.model.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private UserInfo user;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String shippingAddress;
    private String paymentMethod;
    private LocalDateTime orderDate;
    private LocalDateTime deliveredDate;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String fullName;
    }
}