-- ========================================
-- E-COMMERCE SYSTEM - INITIAL SCHEMA --
-- ========================================

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- Categories table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_slug (slug)
) ENGINE=InnoDB;

-- Products table
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    sku VARCHAR(50) NOT NULL UNIQUE,
    image_url VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_slug (slug),
    INDEX idx_sku (sku),
    INDEX idx_price (price),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB;

-- Product_Categories join table (Many-to-Many)
CREATE TABLE product_categories (
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, category_id),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    INDEX idx_product (product_id),
    INDEX idx_category (category_id)
) ENGINE=InnoDB;

-- Cart_Items table
CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_product (user_id, product_id),
    INDEX idx_user (user_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB;

-- Orders table
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    shipping_address TEXT,
    payment_method VARCHAR(50),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_date TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_order_number (order_number),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date)
) ENGINE=InnoDB;

-- Order_Items table
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price_at_purchase DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    INDEX idx_order (order_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB;

-- Reviews table
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_product_user (product_id, user_id),
    INDEX idx_product (product_id),
    INDEX idx_user (user_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB;

-- ========================================
-- SEED DATA
-- ========================================

-- Insert admin user (password: admin123)
INSERT INTO users (username, email, password, full_name, phone, address, role, enabled)
VALUES
('admin', 'admin@ecommerce.com', '$2a$12$2MVZgVqa9NrptU2Vyfq7bOFXYJxowBu6Vp1tYvPzENIjCsa99HDqW', 'Admin User', '1234567890', '123 Admin St', 'ADMIN', TRUE),
('customer1', 'customer@example.com', '$2a$12$2MVZgVqa9NrptU2Vyfq7bOFXYJxowBu6Vp1tYvPzENIjCsa99HDqW', 'John Customer', '9876543210', '456 Customer Ave', 'CUSTOMER', TRUE);

-- Insert categories
INSERT INTO categories (name, slug, description) VALUES
('Electronics', 'electronics', 'Electronic devices and gadgets'),
('Clothing', 'clothing', 'Fashion and apparel'),
('Books', 'books', 'Books and publications'),
('Home & Kitchen', 'home-kitchen', 'Home and kitchen appliances'),
('Sports', 'sports', 'Sports equipment and accessories'),
('Toys', 'toys', 'Toys and games for kids');

-- Insert sample products
INSERT INTO products (name, slug, description, price, stock_quantity, sku, is_active) VALUES
('Laptop Dell XPS 15', 'laptop-dell-xps-15', 'High-performance laptop with 16GB RAM and 512GB SSD', 1299.99, 10, 'ELEC-LAP-001', TRUE),
('iPhone 15 Pro', 'iphone-15-pro', 'Latest iPhone with A17 chip and advanced camera', 999.99, 15, 'ELEC-PHO-001', TRUE),
('Wireless Mouse', 'wireless-mouse', 'Ergonomic wireless mouse with USB receiver', 29.99, 50, 'ELEC-ACC-001', TRUE),
('Gaming Keyboard', 'gaming-keyboard', 'Mechanical gaming keyboard with RGB lighting', 89.99, 30, 'ELEC-ACC-002', TRUE),
('Cotton T-Shirt', 'cotton-t-shirt', 'Comfortable cotton t-shirt in various colors', 19.99, 100, 'CLOT-TSH-001', TRUE),
('Jeans Blue', 'jeans-blue', 'Classic blue denim jeans', 49.99, 60, 'CLOT-PAN-001', TRUE),
('Spring Boot Guide', 'spring-boot-guide', 'Complete guide to Spring Boot development', 39.99, 25, 'BOOK-TEC-001', TRUE),
('Fiction Novel', 'fiction-novel', 'Bestselling fiction novel', 14.99, 40, 'BOOK-FIC-001', TRUE),
('Blender 1000W', 'blender-1000w', 'Powerful blender for smoothies and more', 79.99, 20, 'HOME-APP-001', TRUE),
('Coffee Maker', 'coffee-maker', 'Automatic drip coffee maker', 59.99, 35, 'HOME-APP-002', TRUE);

-- Link products to categories
INSERT INTO product_categories (product_id, category_id) VALUES
(1, 1), -- Laptop -> Electronics
(2, 1), -- iPhone -> Electronics
(3, 1), -- Mouse -> Electronics
(4, 1), -- Keyboard -> Electronics
(5, 2), -- T-Shirt -> Clothing
(6, 2), -- Jeans -> Clothing
(7, 3), -- Spring Boot Guide -> Books
(8, 3), -- Fiction Novel -> Books
(9, 4), -- Blender -> Home & Kitchen
(10, 4); -- Coffee Maker -> Home & Kitchen

-- Insert sample reviews
INSERT INTO reviews (product_id, user_id, rating, comment, created_at) VALUES
(1, 2, 5, 'Excellent laptop! Very fast and reliable.', NOW()),
(2, 2, 4, 'Great phone but a bit expensive.', NOW()),
(5, 2, 5, 'Very comfortable t-shirt, highly recommend!', NOW());
