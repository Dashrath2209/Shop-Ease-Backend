package com.jayaa.ecommerce.service;

import com.jayaa.ecommerce.dto.*;
import com.jayaa.ecommerce.exception.*;
import com.jayaa.ecommerce.model.*;
import com.jayaa.ecommerce.repository.*;
import com.jayaa.ecommerce.util.SlugUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired(required = false)
    private ReviewRepository reviewRepository;

    @Autowired
    private SlugUtil slugUtil;

    // ========== PUBLIC METHODS (Browsing) ==========

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllActiveProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByIsActive(true, pageable);

        List<ProductResponse> responses = products.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, products.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return convertToResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return convertToResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(query, pageable);

        List<ProductResponse> responses = products.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, products.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<Product> products = productRepository.findByCategoryId(categoryId, pageable);

        List<ProductResponse> responses = products.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, products.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        Page<Product> products = productRepository.findByPriceRange(minPrice, maxPrice, pageable);

        List<ProductResponse> responses = products.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, products.getTotalElements());
    }

    // ========== ADMIN METHODS (Product Management) ==========

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // Check SKU uniqueness
        if (productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("SKU already exists: " + request.getSku());
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setSlug(generateUniqueSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setIsActive(request.getIsActive());

        // ⭐ Handle Many-to-Many: Categories
        Set<Category> categories = new HashSet<>();
        for (Long categoryId : request.getCategoryIds()) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
            categories.add(category);
        }
        product.setCategories(categories);

        Product saved = productRepository.save(product);
        productRepository.flush();

        return convertToResponseSimple(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setIsActive(request.getIsActive());

        // Update categories
        product.getCategories().clear();
        Set<Category> categories = new HashSet<>();
        for (Long categoryId : request.getCategoryIds()) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
            categories.add(category);
        }
        product.setCategories(categories);

        Product updated = productRepository.save(product);
        productRepository.flush();

        return convertToResponseSimple(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found");
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductResponse updateProductImage(Long id, String imageUrl) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setImageUrl(imageUrl);
        Product updated = productRepository.save(product);
        return convertToResponseSimple(updated);
    }

    // ========== HELPER METHODS ==========

    private String generateUniqueSlug(String name) {
        String baseSlug = slugUtil.generateSlug(name);
        String slug = baseSlug;
        int attempt = 0;

        while (productRepository.findBySlug(slug).isPresent()) {
            attempt++;
            slug = baseSlug + "-" + attempt;
        }

        return slug;
    }

    // ✅ Simple conversion - used after create/update to avoid lazy loading
    private ProductResponse convertToResponseSimple(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setSlug(product.getSlug());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setSku(product.getSku());
        response.setImageUrl(product.getImageUrl());
        response.setIsActive(product.getIsActive());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());

        // Convert categories WITHOUT triggering lazy loads
        Set<ProductResponse.CategoryInfo> categoryInfos = new HashSet<>();
        if (product.getCategories() != null) {
            for (Category category : product.getCategories()) {
                ProductResponse.CategoryInfo info = new ProductResponse.CategoryInfo();
                info.setId(category.getId());
                info.setName(category.getName());
                info.setSlug(category.getSlug());
                categoryInfos.add(info);
            }
        }
        response.setCategories(categoryInfos);

        // Default review stats
        response.setAverageRating(0.0);
        response.setReviewCount(0L);

        return response;
    }

    // ⭐ Full conversion with review statistics
    private ProductResponse convertToResponse(Product product) {
        ProductResponse response = convertToResponseSimple(product);

        // Get review statistics
        if (reviewRepository != null) {
            try {
                Double avgRating = reviewRepository.getAverageRating(product.getId());
                Long reviewCount = reviewRepository.getReviewCount(product.getId());
                response.setAverageRating(avgRating != null ? avgRating : 0.0);
                response.setReviewCount(reviewCount != null ? reviewCount : 0L);
            } catch (Exception e) {
                System.err.println("Warning: Could not load review stats: " + e.getMessage());
            }
        }

        return response;
    }
}
////package com.jayaa.ecommerce.service;
////
////import com.jayaa.ecommerce.dto.*;
////import com.jayaa.ecommerce.exception.*;
////import com.jayaa.ecommerce.model.*;
////import com.jayaa.ecommerce.repository.*;
////import com.jayaa.ecommerce.util.SlugUtil;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.data.domain.Page;
////import org.springframework.data.domain.Pageable;
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////import java.math.BigDecimal;
////import java.util.HashSet;
////import java.util.Set;
////import java.util.stream.Collectors;
////
////@Service
////@Transactional
////public class ProductService {
////
////    @Autowired
////    private ProductRepository productRepository;
////
////    @Autowired
////    private CategoryRepository categoryRepository;
////
////    @Autowired
////    private ReviewRepository reviewRepository;
////
////    @Autowired
////    private SlugUtil slugUtil;
////
////    // ========== PUBLIC METHODS (Browsing) ==========
////
////    @Transactional(readOnly = true)
////    public Page<ProductResponse> getAllActiveProducts(Pageable pageable) {
////        Page<Product> products = productRepository.findByIsActive(true, pageable);
////        return products.map(this::convertToResponse);
////    }
////
////    @Transactional(readOnly = true)
////    public ProductResponse getProductById(Long id) {
////        Product product = productRepository.findById(id)
////                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
////        return convertToResponse(product);
////    }
////
////    @Transactional(readOnly = true)
////    public ProductResponse getProductBySlug(String slug) {
////        Product product = productRepository.findBySlug(slug)
////                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
////        return convertToResponse(product);
////    }
////
////    @Transactional(readOnly = true)
////    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
////        Page<Product> products = productRepository.searchProducts(query, pageable);
////        return products.map(this::convertToResponse);
////    }
////
////    @Transactional(readOnly = true)
////    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
////        Page<Product> products = productRepository.findByCategoryId(categoryId, pageable);
////        return products.map(this::convertToResponse);
////    }
////
////    @Transactional(readOnly = true)
////    public Page<ProductResponse> getProductsByPriceRange(
////            BigDecimal minPrice,
////            BigDecimal maxPrice,
////            Pageable pageable
////    ) {
////        Page<Product> products = productRepository.findByPriceRange(minPrice, maxPrice, pageable);
////        return products.map(this::convertToResponse);
////    }
////
////    // ========== ADMIN METHODS (Product Management) ==========
////
////    public ProductResponse createProduct(ProductRequest request) {
////        // Check SKU uniqueness
////        if (productRepository.existsBySku(request.getSku())) {
////            throw new BadRequestException("SKU already exists");
////        }
////
////        Product product = new Product();
////        product.setName(request.getName());
////        product.setSlug(generateUniqueSlug(request.getName()));
////        product.setDescription(request.getDescription());
////        product.setPrice(request.getPrice());
////        product.setStockQuantity(request.getStockQuantity());
////        product.setSku(request.getSku());
////        product.setIsActive(request.getIsActive());
////
////        // ⭐ Handle Many-to-Many: Categories
////        Set<Category> categories = new HashSet<>();
////        for (Long categoryId : request.getCategoryIds()) {
////            Category category = categoryRepository.findById(categoryId)
////                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
////            categories.add(category);
////        }
////        product.setCategories(categories);
////
////        Product saved = productRepository.save(product);
////        return convertToResponse(saved);
////    }
////
////    public ProductResponse updateProduct(Long id, ProductRequest request) {
////        Product product = productRepository.findById(id)
////                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
////
////        product.setName(request.getName());
////        product.setDescription(request.getDescription());
////        product.setPrice(request.getPrice());
////        product.setStockQuantity(request.getStockQuantity());
////        product.setIsActive(request.getIsActive());
////
////        // Update categories
////        product.getCategories().clear();
////        Set<Category> categories = new HashSet<>();
////        for (Long categoryId : request.getCategoryIds()) {
////            Category category = categoryRepository.findById(categoryId)
////                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
////            categories.add(category);
////        }
////        product.setCategories(categories);
////
////        Product updated = productRepository.save(product);
////        return convertToResponse(updated);
////    }
////
////    public void deleteProduct(Long id) {
////        if (!productRepository.existsById(id)) {
////            throw new ResourceNotFoundException("Product not found");
////        }
////        productRepository.deleteById(id);
////    }
////
////    public ProductResponse updateProductImage(Long id, String imageUrl) {
////        Product product = productRepository.findById(id)
////                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
////
////        product.setImageUrl(imageUrl);
////        Product updated = productRepository.save(product);
////        return convertToResponse(updated);
////    }
////
////    // ========== HELPER METHODS ==========
////
////    private String generateUniqueSlug(String name) {
////        String baseSlug = slugUtil.generateSlug(name);
////        String slug = baseSlug;
////        int attempt = 0;
////
////        while (productRepository.findBySlug(slug).isPresent()) {
////            attempt++;
////            slug = baseSlug + "-" + attempt;
////        }
////
////        return slug;
////    }
////
////    // ⭐ COMPLEX DTO CONVERSION with relationships and aggregations
////    private ProductResponse convertToResponse(Product product) {
////        ProductResponse response = new ProductResponse();
////        response.setId(product.getId());
////        response.setName(product.getName());
////        response.setSlug(product.getSlug());
////        response.setDescription(product.getDescription());
////        response.setPrice(product.getPrice());
////        response.setStockQuantity(product.getStockQuantity());
////        response.setSku(product.getSku());
////        response.setImageUrl(product.getImageUrl());
////        response.setIsActive(product.getIsActive());
////        response.setCreatedAt(product.getCreatedAt());
////        response.setUpdatedAt(product.getUpdatedAt());
////
////        // Convert categories
////        Set<ProductResponse.CategoryInfo> categoryInfos = product.getCategories().stream()
////                .map(category -> {
////                    ProductResponse.CategoryInfo info = new ProductResponse.CategoryInfo();
////                    info.setId(category.getId());
////                    info.setName(category.getName());
////                    info.setSlug(category.getSlug());
////                    return info;
////                })
////                .collect(Collectors.toSet());
////        response.setCategories(categoryInfos);
////
////        // ⭐ Get review statistics
////        Double avgRating = reviewRepository.getAverageRating(product.getId());
////        Long reviewCount = reviewRepository.getReviewCount(product.getId());
////        response.setAverageRating(avgRating != null ? avgRating : 0.0);
////        response.setReviewCount(reviewCount != null ? reviewCount : 0L);
////
////        return response;
////    }
////}
//
//package com.jayaa.ecommerce.service;
//
//import com.jayaa.ecommerce.dto.*;
//import com.jayaa.ecommerce.exception.*;
//import com.jayaa.ecommerce.model.*;
//import com.jayaa.ecommerce.repository.*;
//import com.jayaa.ecommerce.util.SlugUtil;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.math.BigDecimal;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Service
//@Transactional
//public class ProductService {
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private CategoryRepository categoryRepository;
//
//    @Autowired(required = false)
//    private ReviewRepository reviewRepository;
//
//    @Autowired
//    private SlugUtil slugUtil;
//
//    // ========== PUBLIC METHODS (Browsing) ==========
//
//    @Transactional(readOnly = true)
//    public Page<ProductResponse> getAllActiveProducts(Pageable pageable) {
//        Page<Product> products = productRepository.findByIsActive(true, pageable);
//        return products.map(this::convertToResponse);
//    }
//
//    @Transactional(readOnly = true)
//    public ProductResponse getProductById(Long id) {
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
//        return convertToResponse(product);
//    }
//
//    @Transactional(readOnly = true)
//    public ProductResponse getProductBySlug(String slug) {
//        Product product = productRepository.findBySlug(slug)
//                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
//        return convertToResponse(product);
//    }
//
//    @Transactional(readOnly = true)
//    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
//        Page<Product> products = productRepository.searchProducts(query, pageable);
//        return products.map(this::convertToResponse);
//    }
//
//    @Transactional(readOnly = true)
//    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
//        Page<Product> products = productRepository.findByCategoryId(categoryId, pageable);
//        return products.map(this::convertToResponse);
//    }
//
//    @Transactional(readOnly = true)
//    public Page<ProductResponse> getProductsByPriceRange(
//            BigDecimal minPrice,
//            BigDecimal maxPrice,
//            Pageable pageable
//    ) {
//        Page<Product> products = productRepository.findByPriceRange(minPrice, maxPrice, pageable);
//        return products.map(this::convertToResponse);
//    }
//
//    // ========== ADMIN METHODS (Product Management) ==========
//
//    public ProductResponse createProduct(ProductRequest request) {
//        // Check SKU uniqueness
//        if (productRepository.existsBySku(request.getSku())) {
//            throw new BadRequestException("SKU already exists");
//        }
//
//        Product product = new Product();
//        product.setName(request.getName());
//        product.setSlug(generateUniqueSlug(request.getName()));
//        product.setDescription(request.getDescription());
//        product.setPrice(request.getPrice());
//        product.setStockQuantity(request.getStockQuantity());
//        product.setSku(request.getSku());
//        product.setIsActive(request.getIsActive());
//
//        // ⭐ Handle Many-to-Many: Categories
//        Set<Category> categories = new HashSet<>();
//        for (Long categoryId : request.getCategoryIds()) {
//            Category category = categoryRepository.findById(categoryId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
//            categories.add(category);
//        }
//        product.setCategories(categories);
//
//        Product saved = productRepository.save(product);
//        return convertToResponse(saved);
//    }
//
//    public ProductResponse updateProduct(Long id, ProductRequest request) {
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
//
//        product.setName(request.getName());
//        product.setDescription(request.getDescription());
//        product.setPrice(request.getPrice());
//        product.setStockQuantity(request.getStockQuantity());
//        product.setIsActive(request.getIsActive());
//
//        // Update categories
//        product.getCategories().clear();
//        Set<Category> categories = new HashSet<>();
//        for (Long categoryId : request.getCategoryIds()) {
//            Category category = categoryRepository.findById(categoryId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
//            categories.add(category);
//        }
//        product.setCategories(categories);
//
//        Product updated = productRepository.save(product);
//        return convertToResponse(updated);
//    }
//
//    public void deleteProduct(Long id) {
//        if (!productRepository.existsById(id)) {
//            throw new ResourceNotFoundException("Product not found");
//        }
//        productRepository.deleteById(id);
//    }
//
//    public ProductResponse updateProductImage(Long id, String imageUrl) {
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
//
//        product.setImageUrl(imageUrl);
//        Product updated = productRepository.save(product);
//        return convertToResponse(updated);
//    }
//
//    // ========== HELPER METHODS ==========
//
//    private String generateUniqueSlug(String name) {
//        String baseSlug = slugUtil.generateSlug(name);
//        String slug = baseSlug;
//        int attempt = 0;
//
//        while (productRepository.findBySlug(slug).isPresent()) {
//            attempt++;
//            slug = baseSlug + "-" + attempt;
//        }
//
//        return slug;
//    }
//
//    // ⭐ COMPLEX DTO CONVERSION with relationships and aggregations
//    private ProductResponse convertToResponse(Product product) {
//        ProductResponse response = new ProductResponse();
//        response.setId(product.getId());
//        response.setName(product.getName());
//        response.setSlug(product.getSlug());
//        response.setDescription(product.getDescription());
//        response.setPrice(product.getPrice());
//        response.setStockQuantity(product.getStockQuantity());
//        response.setSku(product.getSku());
//        response.setImageUrl(product.getImageUrl());
//        response.setIsActive(product.getIsActive());
//        response.setCreatedAt(product.getCreatedAt());
//        response.setUpdatedAt(product.getUpdatedAt());
//
//        // Convert categories - safely handle null or empty
//        Set<ProductResponse.CategoryInfo> categoryInfos = new HashSet<>();
//        try {
//            if (product.getCategories() != null && !product.getCategories().isEmpty()) {
//                categoryInfos = product.getCategories().stream()
//                        .filter(category -> category != null)
//                        .map(category -> {
//                            ProductResponse.CategoryInfo info = new ProductResponse.CategoryInfo();
//                            info.setId(category.getId());
//                            info.setName(category.getName());
//                            info.setSlug(category.getSlug());
//                            return info;
//                        })
//                        .collect(Collectors.toSet());
//            }
//        } catch (Exception e) {
//            System.err.println("Warning: Could not load categories for product " + product.getId() + ": " + e.getMessage());
//        }
//        response.setCategories(categoryInfos);
//
//        // ⭐ Get review statistics - safely handle if ReviewRepository not available
//        try {
//            if (reviewRepository != null) {
//                Double avgRating = reviewRepository.getAverageRating(product.getId());
//                Long reviewCount = reviewRepository.getReviewCount(product.getId());
//                response.setAverageRating(avgRating != null ? avgRating : 0.0);
//                response.setReviewCount(reviewCount != null ? reviewCount : 0L);
//            } else {
//                response.setAverageRating(0.0);
//                response.setReviewCount(0L);
//            }
//        } catch (Exception e) {
//            System.err.println("Warning: Could not load review stats for product " + product.getId() + ": " + e.getMessage());
//            response.setAverageRating(0.0);
//            response.setReviewCount(0L);
//        }
//
//        return response;
//    }
//}