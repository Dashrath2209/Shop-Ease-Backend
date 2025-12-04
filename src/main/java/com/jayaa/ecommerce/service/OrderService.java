package com.jayaa.ecommerce.service;

import com.jayaa.ecommerce.dto.*;
import com.jayaa.ecommerce.exception.*;
import com.jayaa.ecommerce.model.*;
import com.jayaa.ecommerce.repository.*;
import com.jayaa.ecommerce.util.SkuGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkuGenerator skuGenerator;

    // ⭐ PLACE ORDER (Most Complex Transaction!)
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get cart items
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // ⭐ STEP 1: Validate stock for ALL items BEFORE creating order
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (!product.getIsActive()) {
                throw new BadRequestException("Product is not available: " + product.getName());
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for: " + product.getName() +
                                ". Available: " + product.getStockQuantity() +
                                ", Requested: " + cartItem.getQuantity()
                );
            }
        }

        // ⭐ STEP 2: Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setOrderDate(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // ⭐ STEP 3: Create order items AND reduce stock
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            orderItem.setSubtotal(subtotal);

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(subtotal);

            // ⭐ REDUCE STOCK (Critical operation!)
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        // ⭐ STEP 4: Save order (cascades to order items)
        Order savedOrder = orderRepository.save(order);

        // ⭐ STEP 5: Clear cart
        cartItemRepository.deleteByUserId(userId);

        return convertToOrderResponse(savedOrder);
    }

    // ⭐ GET MY ORDERS
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(this::convertToOrderResponse);
    }

    // ⭐ GET ORDER BY ID
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check ownership
        Long userId = getCurrentUserId();
        if (!order.getUser().getId().equals(userId) && !isAdmin()) {
            throw new ForbiddenException("Access denied");
        }

        return convertToOrderResponse(order);
    }

    // ⭐ CANCEL ORDER (with stock restoration)
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check ownership
        Long userId = getCurrentUserId();
        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        // Can only cancel PENDING orders
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(
                    "Can only cancel pending orders. Current status: " + order.getStatus()
            );
        }

        // ⭐ RESTORE STOCK
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        return convertToOrderResponse(updated);
    }

    // ========== ADMIN METHODS ==========

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(this::convertToOrderResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatus(status, pageable);
        return orders.map(this::convertToOrderResponse);
    }

    // ⭐ UPDATE ORDER STATUS (ADMIN)
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        order.setStatus(newStatus);

        // Set delivered date when status changes to DELIVERED
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredDate(LocalDateTime.now());
        }

        Order updated = orderRepository.save(order);
        return convertToOrderResponse(updated);
    }

    // ========== HELPER METHODS ==========

    private String generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        Long count = orderRepository.countOrdersCreatedToday(startOfDay);
        return skuGenerator.generateOrderNumber(now.getYear(), count);
    }

    private Long getCurrentUserId() {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Not authenticated");
        }
        return auth.getName();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    // ⭐ COMPLEX DTO CONVERSION
    private OrderResponse convertToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setOrderDate(order.getOrderDate());
        response.setDeliveredDate(order.getDeliveredDate());

        // User info
        OrderResponse.UserInfo userInfo = new OrderResponse.UserInfo();
        userInfo.setId(order.getUser().getId());
        userInfo.setUsername(order.getUser().getUsername());
        userInfo.setEmail(order.getUser().getEmail());
        userInfo.setFullName(order.getUser().getFullName());
        response.setUser(userInfo);

        // Order items
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::convertToOrderItemResponse)
                .collect(Collectors.toList());
        response.setItems(items);

        return response;
    }

    private OrderItemResponse convertToOrderItemResponse(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        response.setQuantity(orderItem.getQuantity());
        response.setPriceAtPurchase(orderItem.getPriceAtPurchase());
        response.setSubtotal(orderItem.getSubtotal());

        // Product info
        Product product = orderItem.getProduct();
        OrderItemResponse.ProductInfo productInfo = new OrderItemResponse.ProductInfo();
        productInfo.setId(product.getId());
        productInfo.setName(product.getName());
        productInfo.setSlug(product.getSlug());
        productInfo.setImageUrl(product.getImageUrl());
        response.setProduct(productInfo);

        return response;
    }
}