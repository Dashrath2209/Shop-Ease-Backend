package com.jayaa.ecommerce.repository;

import com.jayaa.ecommerce.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySku(String sku);

    // Find active products only
    Page<Product> findByIsActive(Boolean isActive, Pageable pageable);

    // ⭐ Search by name or description
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    // ⭐ Filter by price range
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    Page<Product> findByPriceRange(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    // ⭐ Find by category
    @Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id = :categoryId AND p.isActive = true")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    // Check stock availability
    @Query("SELECT CASE WHEN p.stockQuantity >= :quantity THEN true ELSE false END " +
            "FROM Product p WHERE p.id = :productId")
    boolean hasEnoughStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}