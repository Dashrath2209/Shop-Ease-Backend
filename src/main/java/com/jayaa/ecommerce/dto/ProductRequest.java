package com.jayaa.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Set;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 255)
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 50)
    private String sku;

    @NotNull(message = "At least one category is required")
    @Size(min = 1, message = "At least one category is required")
    private Set<Long> categoryIds;

    private Boolean isActive = true;
}