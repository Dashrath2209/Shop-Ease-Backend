package com.jayaa.ecommerce.controller;

import com.jayaa.ecommerce.dto.ProductRequest;
import com.jayaa.ecommerce.dto.ProductResponse;
import com.jayaa.ecommerce.service.ProductService;
import com.jayaa.ecommerce.util.FileStorageUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")

public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    // ========== PUBLIC ENDPOINTS ==========

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @PageableDefault(size = 12, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getAllActiveProducts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String q,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        return ResponseEntity.ok(productService.searchProducts(q, pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId, pageable));
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponse>> filterByPrice(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice, pageable));
    }

    // ========== ADMIN ENDPOINTS ==========

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ProductResponse> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        String filename = fileStorageUtil.storeFile(file);
        ProductResponse updated = productService.updateProductImage(id, filename);
        return ResponseEntity.ok(updated);
    }
}
