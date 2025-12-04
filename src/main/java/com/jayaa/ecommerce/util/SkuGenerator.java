package com.jayaa.ecommerce.util;

import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class SkuGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    /**
     * Generate SKU format: XXXX-YYYY-ZZZZ
     * Example: PROD-A1B2-C3D4
     */
    public String generateSku(String prefix) {
        StringBuilder sku = new StringBuilder(prefix.toUpperCase());
        sku.append("-");

        // Add 4 random characters
        for (int i = 0; i < 4; i++) {
            sku.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        sku.append("-");

        // Add 4 more random characters
        for (int i = 0; i < 4; i++) {
            sku.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }

        return sku.toString();
    }

    /**
     * Generate order number: ORD-YYYY-NNN
     * Example: ORD-2025-001
     */
    public String generateOrderNumber(int year, long count) {
        return String.format("ORD-%d-%03d", year, count + 1);
    }
}