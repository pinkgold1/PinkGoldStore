package com.pinkgold.controller;

import com.pinkgold.entity.*;
import com.pinkgold.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

import static tools.jackson.databind.type.LogicalType.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final VoucherRepository voucherRepository;
    private final CategoryRepository categoryRepository;

    // ==================== QUẢN LÝ USER ====================
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(@PathVariable Long id, @RequestParam Boolean status) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setStatus(status);
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== QUẢN LÝ SẢN PHẨM ====================
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProductsAdmin() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    // ==================== QUẢN LÝ ĐƠN HÀNG ====================
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrdersAdmin() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id,
                                                   @RequestParam OrderStatus status) {
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(status);
                    return ResponseEntity.ok(orderRepository.save(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== QUẢN LÝ VOUCHER ====================
    @GetMapping("/vouchers")
    public ResponseEntity<List<Voucher>> getAllVouchersAdmin() {
        return ResponseEntity.ok(voucherRepository.findAll());
    }

    // ==================== THỐNG KÊ CƠ BẢN ====================
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        // Sử dụng HashMap thay vì Map.of() để tránh lỗi
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalProducts", totalProducts);
        stats.put("totalOrders", totalOrders);
        // stats.put("totalRevenue", 0); // có thể tính sau

        return ResponseEntity.ok(stats);
    }
}