package com.jayaa.ecommerce.service;

import com.jayaa.ecommerce.dto.*;
import com.jayaa.ecommerce.exception.*;
import com.jayaa.ecommerce.model.*;
import com.jayaa.ecommerce.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    // ⭐ GET CART
    @Transactional(readOnly = true)
    public CartResponse getMyCart() {
        Long userId = getCurrentUserId();
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        CartResponse response = new CartResponse();

        List<CartItemResponse> items = cartItems.stream()
                .map(this::convertToCartItemResponse)
                .collect(Collectors.toList());

        response.setItems(items);
        response.setTotalItems(cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum());

        BigDecimal total = cartItems.stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalAmount(total);

        return response;
    }

    // ⭐ ADD TO CART (with stock check)
    public CartItemResponse addToCart(AddToCartRequest request) {
        Long userId = getCurrentUserId();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if product is active
        if (!product.getIsActive()) {
            throw new BadRequestException("Product is not available");
        }

        // Check stock availability
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: " + product.getStockQuantity()
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if already in cart
        CartItem cartItem = cartItemRepository
                .findByUserIdAndProductId(userId, request.getProductId())
                .orElse(null);

        if (cartItem != null) {
            // Update quantity
            int newQuantity = cartItem.getQuantity() + request.getQuantity();

            // Check stock again for new quantity
            if (product.getStockQuantity() < newQuantity) {
                throw new InsufficientStockException(
                        "Insufficient stock. Available: " + product.getStockQuantity() +
                                ", Already in cart: " + cartItem.getQuantity()
                );
            }

            cartItem.setQuantity(newQuantity);
            CartItem updated = cartItemRepository.save(cartItem);
            return convertToCartItemResponse(updated);
        } else {
            // Add new item
            CartItem newItem = new CartItem();
            newItem.setUser(user);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());

            CartItem saved = cartItemRepository.save(newItem);
            return convertToCartItemResponse(saved);
        }
    }

    // ⭐ UPDATE CART ITEM
    public CartItemResponse updateCartItem(Long productId, UpdateCartItemRequest request) {
        Long userId = getCurrentUserId();

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        Product product = cartItem.getProduct();

        // Check stock for new quantity
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: " + product.getStockQuantity()
            );
        }

        cartItem.setQuantity(request.getQuantity());
        CartItem updated = cartItemRepository.save(cartItem);
        return convertToCartItemResponse(updated);
    }

    // ⭐ REMOVE FROM CART
    public void removeFromCart(Long productId) {
        Long userId = getCurrentUserId();

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        cartItemRepository.delete(cartItem);
    }

    // ⭐ CLEAR CART
    public void clearCart() {
        Long userId = getCurrentUserId();
        cartItemRepository.deleteByUserId(userId);
    }

    // ========== HELPER METHODS ==========

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

    private CartItemResponse convertToCartItemResponse(CartItem cartItem) {
        CartItemResponse response = new CartItemResponse();
        response.setId(cartItem.getId());
        response.setQuantity(cartItem.getQuantity());
        response.setAddedAt(cartItem.getAddedAt());

        // Product info
        Product product = cartItem.getProduct();
        CartItemResponse.ProductInfo productInfo = new CartItemResponse.ProductInfo();
        productInfo.setId(product.getId());
        productInfo.setName(product.getName());
        productInfo.setSlug(product.getSlug());
        productInfo.setPrice(product.getPrice());
        productInfo.setStockQuantity(product.getStockQuantity());
        productInfo.setImageUrl(product.getImageUrl());
        response.setProduct(productInfo);

        // Calculate subtotal
        BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        response.setSubtotal(subtotal);

        return response;
    }
}