package com.jayaa.ecommerce.repository;

import com.jayaa.ecommerce.model.Order;
import com.jayaa.ecommerce.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // ⭐ Count orders created today (for order number generation)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :startOfDay")
    Long countOrdersCreatedToday(@Param("startOfDay") LocalDateTime startOfDay);

    // ⭐ Get total sales
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status != 'CANCELLED'")
    BigDecimal getTotalSales();
}