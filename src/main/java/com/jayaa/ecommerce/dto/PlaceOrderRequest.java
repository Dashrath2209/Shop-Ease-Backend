package com.jayaa.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    @NotBlank(message = "Shipping address is required")
    @Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    private String shippingAddress;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // COD, CARD, UPI, etc.
}