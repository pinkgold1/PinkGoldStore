CREATE DATABASE pinkgold_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pinkgold_db;

-- 1. Người dùng (Khách hàng + Admin)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(15),
    address TEXT,
    date_of_birth DATE,
    role ENUM('CUSTOMER', 'ADMIN') DEFAULT 'CUSTOMER',
    status BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. Danh mục sản phẩm
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 3. Sản phẩm
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(15,2) NOT NULL,
    discount_price DECIMAL(15,2),
    stock INT NOT NULL DEFAULT 0,
    material VARCHAR(100),           -- Vàng, Bạc, Kim cương...
    weight DECIMAL(10,2),            -- Trọng lượng
    image_url VARCHAR(255),
    status BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 4. Ảnh sản phẩm (nhiều ảnh)
CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT,
    image_url VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 5. Giỏ hàng
CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    product_id BIGINT,
    quantity INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 6. Đơn hàng
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    order_code VARCHAR(50) UNIQUE NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    final_amount DECIMAL(15,2) NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
    payment_status ENUM('UNPAID', 'PAID', 'REFUNDED') DEFAULT 'UNPAID',
    shipping_address TEXT,
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 7. Chi tiết đơn hàng
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    product_id BIGINT,
    quantity INT NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 8. Thanh toán (VNPay)
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    payment_method VARCHAR(50),
    transaction_code VARCHAR(100),
    amount DECIMAL(15,2),
    status ENUM('SUCCESS', 'FAILED', 'PENDING') DEFAULT 'PENDING',
    payment_time DATETIME,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- 9. Mã giảm giá (Voucher)
CREATE TABLE vouchers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_percent INT,
    discount_amount DECIMAL(15,2),
    min_order_amount DECIMAL(15,2),
    start_date DATE,
    end_date DATE,
    quantity INT DEFAULT 1,
    used_count INT DEFAULT 0,
    status BOOLEAN DEFAULT TRUE
);

-- 10. Đánh giá sản phẩm
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    product_id BIGINT,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

ALTER TABLE payments 
MODIFY COLUMN status ENUM('SUCCESS', 'FAILED', 'PENDING', 'UNPAID', 'PAID', 'REFUNDED') DEFAULT 'PENDING';

ALTER TABLE products
ADD COLUMN availability_type ENUM('IN_STOCK','RESTOCKING','MADE_TO_ORDER') DEFAULT 'IN_STOCK',
ADD COLUMN restock_date DATE NULL;

INSERT INTO users (username, password, full_name, email, phone, address, role, status) VALUES
('admin', '123456', 'Quản Trị Viên PinkGold', 'admin@pinkgold.com', '0987654321', '123 Đường ABC, Quận 1, TP.HCM', 'ADMIN', TRUE),

('customer1', '1234566', 'Nguyễn Thị Hồng Nhung', 'nhung.nguyen@gmail.com', '0912345678', '456 Nguyễn Văn Linh, Quận 7, TP.HCM', 'CUSTOMER', TRUE),
('customer2', '123456', 'Trần Minh Quân', 'quan.tran@gmail.com', '0933456789', '789 Lê Văn Sỹ, Quận 3, TP.HCM', 'CUSTOMER', TRUE),
('customer3', '123456', 'Lê Thị Ngọc Anh', 'anh.le@gmail.com', '0978123456', '101 Pasteur, Quận 1, TP.HCM', 'CUSTOMER', TRUE),
('customer4', '123456', 'Phạm Hoàng Nam', 'nam.pham@gmail.com', '0908765432', '202 Võ Văn Kiệt, Quận 5, TP.HCM', 'CUSTOMER', TRUE);

INSERT INTO categories (name, description) VALUES
('Nhẫn', 'Các mẫu nhẫn vàng, kim cương, bạc cao cấp'),
('Dây Chuyền', 'Dây chuyền vàng, bạc, ngọc trai sang trọng'),
('Lắc Tay & Vòng Tay', 'Lắc tay, vòng tay thời trang và cao cấp'),
('Bông Tai', 'Bông tai vàng, bạc, đá quý đa dạng'),
('Mặt Dây Chuyền', 'Mặt dây chuyền nam nữ, Phật bản mệnh'),
('Trang Sức Cưới', 'Bộ trang sức cưới cao cấp'),
('Trang Sức Bạc', 'Trang sức bạc 925 thời trang hiện đại');

INSERT INTO products (category_id, name, description, price, discount_price, stock, material, weight, image_url, status) VALUES
(1, 'Nhẫn Vàng 18K Đá CZ Tròn', 'Nhẫn vàng 18K đính đá CZ cao cấp', 2450000, 2190000, 45, 'Vàng 18K', 2.45, 'assets/img/products/nhan-vang-cz.jpg', TRUE),
(1, 'Nhẫn Kim Cương 4.5ly Thiên Nhiên', 'Nhẫn kim cương thiên nhiên 4.5ly', 18500000, 17200000, 8, 'Vàng trắng 18K + Kim cương', 3.8, 'assets/img/products/nhan-kimcuong.jpg', TRUE),
(2, 'Dây Chuyền Vàng 18K Trái Tim', 'Dây chuyền vàng mặt trái tim sang trọng', 3250000, 2890000, 32, 'Vàng 18K', 5.2, 'assets/img/products/day-chuyen-traitim.jpg', TRUE),
(2, 'Dây Chuyền Bạc 925 Ngôi Sao', 'Dây chuyền bạc mặt ngôi sao', 890000, 690000, 78, 'Bạc 925', 4.1, 'assets/img/products/day-chuyen-ngoisao.jpg', TRUE),
(3, 'Lắc Tay Vàng Charm Bear', 'Lắc tay vàng charm gấu dễ thương', 2850000, 2590000, 25, 'Vàng 18K', 6.8, 'assets/img/products/lac-tay-charm.jpg', TRUE),
(3, 'Vòng Tay Ngọc Bích Thiên Nhiên', 'Vòng tay ngọc bích A+ cao cấp', 4500000, NULL, 12, 'Ngọc Bích', 12.5, 'assets/img/products/vong-ngoc-bich.jpg', TRUE),
(4, 'Bông Tai Vàng Hoa Hồng', 'Bông tai vàng 18K hoa hồng', 1650000, 1490000, 40, 'Vàng 18K', 1.8, 'assets/img/products/bong-tai-hoahong.jpg', TRUE),
(4, 'Bông Tai Kim Cương 3ly', 'Bông tai kim cương thiên nhiên', 9800000, 9200000, 6, 'Vàng trắng + Kim cương', 2.1, 'assets/img/products/bong-tai-kimcuong.jpg', TRUE),
(5, 'Mặt Dây Chuyền Phật Bản Mệnh', 'Mặt dây chuyền Phật bản mệnh', 1350000, 1190000, 55, 'Vàng 18K', 3.2, 'assets/img/products/mat-phat.jpg', TRUE),
(6, 'Nhẫn Cưới Vàng Trơn Đôi', 'Nhẫn cưới vàng 18K trơn cao cấp', 4850000, 4590000, 20, 'Vàng 18K', 4.5, 'assets/img/products/nhan-cuoi.jpg', TRUE),
(7, 'Lắc Tay Bạc 925 Charm Moon', 'Lắc tay bạc charm mặt trăng', 750000, 650000, 68, 'Bạc 925', 8.5, 'assets/img/products/lac-bac-moon.jpg', TRUE),
(1, 'Nhẫn Nam Vàng 18K Đơn Giản', 'Nhẫn nam vàng 18K sang trọng', 3250000, 2990000, 18, 'Vàng 18K', 5.8, 'assets/img/products/nhan-nam.jpg', TRUE),
(2, 'Dây Chuyền Vàng Nam', 'Dây chuyền vàng nam bản to', 4250000, 3890000, 15, 'Vàng 18K', 12.5, 'assets/img/products/day-chuyen-nam.jpg', TRUE),
(4, 'Bông Tai Bạc 925', 'Bông tai bạc 925 kiểu Hàn', 450000, 390000, 85, 'Bạc 925', 1.5, 'assets/img/products/bong-tai-bac.jpg', TRUE),
(3, 'Vòng Tay Vàng 18K', 'Vòng tay vàng 18K nam nữ', 3650000, 3290000, 22, 'Vàng 18K', 9.2, 'assets/img/products/vong-tay-vang.jpg', TRUE);

INSERT INTO product_images (product_id, image_url, is_primary) VALUES
(1, 'assets/img/products/nhan-vang-cz-1.jpg', TRUE),
(1, 'assets/img/products/nhan-vang-cz-2.jpg', FALSE),
(2, 'assets/img/products/nhan-kimcuong-1.jpg', TRUE),
(3, 'assets/img/products/day-chuyen-traitim-1.jpg', TRUE),
(4, 'assets/img/products/day-chuyen-ngoisao-1.jpg', TRUE),
(5, 'assets/img/products/lac-tay-charm-1.jpg', TRUE),
(6, 'assets/img/products/vong-ngoc-bich-1.jpg', TRUE),
(7, 'assets/img/products/bong-tai-hoahong-1.jpg', TRUE);

INSERT INTO vouchers (code, discount_percent, discount_amount, min_order_amount, start_date, end_date, quantity, used_count, status) VALUES
('PINKGOLD10', 10, NULL, 2000000, '2025-06-01', '2025-12-31', 100, 15, TRUE),
('GOLD20', 20, NULL, 5000000, '2025-06-01', '2025-08-31', 50, 8, TRUE),
('FREESHIP', NULL, 50000, 3000000, '2025-06-01', '2025-07-31', 200, 67, TRUE),
('NEWUSER15', 15, NULL, 1500000, '2025-06-01', '2025-12-31', 300, 89, TRUE),
('VIP30', 30, NULL, 10000000, '2025-06-01', '2025-09-30', 20, 3, TRUE);

INSERT INTO orders (user_id, order_code, total_amount, discount_amount, final_amount, status, payment_status, shipping_address, note) VALUES
(2, 'ORDER20250604001', 12450000, 1245000, 11205000, 'DELIVERED', 'PAID', '456 Nguyễn Văn Linh, Quận 7, TP.HCM', 'Gói quà cẩn thận'),
(3, 'ORDER20250604002', 8950000, 0, 8950000, 'SHIPPING', 'PAID', '101 Pasteur, Quận 1, TP.HCM', NULL),
(4, 'ORDER20250604003', 3250000, 325000, 2925000, 'PENDING', 'UNPAID', '202 Võ Văn Kiệt, Quận 5, TP.HCM', 'Giao giờ hành chính');

INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(1, 1, 1, 2190000),
(1, 3, 1, 2890000),
(1, 7, 2, 1490000),

(2, 2, 1, 17200000),
(2, 4, 1, 690000),

(3, 5, 1, 2590000);

INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, payment_time) VALUES
(1, 'VNPAY', 'VNP123456789', 11205000, 'SUCCESS', '2025-06-04 10:25:00'),
(2, 'VNPAY', 'VNP987654321', 8950000, 'SUCCESS', '2025-06-04 14:15:00'),
(3, 'COD', NULL, 2925000, 'PENDING', NULL);

INSERT INTO reviews (user_id, product_id, rating, comment) VALUES
(2, 1, 5, 'Nhẫn rất đẹp, chất lượng vàng tốt, giao hàng nhanh'),
(2, 3, 4, 'Dây chuyền đẹp nhưng hơi mỏng'),
(3, 2, 5, 'Kim cương sáng lấp lánh, rất hài lòng'),
(4, 7, 5, 'Bông tai xinh xắn, đáng mua'),
(3, 5, 4, 'Mặt dây chuyền Phật rất ý nghĩa');

INSERT INTO cart_items (user_id, product_id, quantity) VALUES
(2, 4, 1),
(2, 8, 2),
(3, 6, 1),
(4, 10, 1);