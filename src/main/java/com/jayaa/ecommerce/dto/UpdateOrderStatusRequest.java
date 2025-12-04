package com.jayaa.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status is required")
    private String status; // CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}