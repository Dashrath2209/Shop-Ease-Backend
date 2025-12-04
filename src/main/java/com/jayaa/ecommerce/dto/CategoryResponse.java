package com.jayaa.ecommerce.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private Integer productCount;
    private LocalDateTime createdAt;
}