package com.pinkgold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinkgold.entity.*;
import com.pinkgold.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository       orderRepository;
    private final UserRepository        userRepository;
    private final ProductRepository     productRepository;
    private final OrderItemRepository   orderItemRepository;
    private final PaymentRepository     paymentRepository;

    // ─────────────────────────────────────────────────────────
    // GET ALL  (Admin)
    // ─────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllOrders() {
        return ResponseEntity.ok(
                orderRepository.findAll().stream().map(this::toMap).toList()
        );
    }

    // ─────────────────────────────────────────────────────────
    // GET my orders  (dùng userId từ query param — tạm thời)
    // ─────────────────────────────────────────────────────────
    @GetMapping("/my-orders")
    public ResponseEntity<List<Map<String, Object>>> getMyOrders(
            @RequestParam(defaultValue = "2") Long userId) {
        return ResponseEntity.ok(
                orderRepository.findByUserId(userId).stream().map(this::toMap).toList()
        );
    }

    // ─────────────────────────────────────────────────────────
    // GET by orderCode
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{orderCode}")
    public ResponseEntity<?> getOrderByCode(@PathVariable String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode);
        return order != null
                ? ResponseEntity.ok(toMap(order))
                : ResponseEntity.notFound().build();
    }

    // ─────────────────────────────────────────────────────────
    // CREATE ORDER  ← phần quan trọng nhất, đã sửa đầy đủ
    // POST /api/orders/create
    // Body JSON:
    // {
    //   "userId": 2,
    //   "shippingAddress": "...",
    //   "note": "...",
    //   "paymentMethod": "COD" | "VNPAY",
    //   "totalAmount": 5000000,
    //   "discountAmount": 500000,
    //   "finalAmount": 4500000,
    //   "items": [
    //     { "productId": 1, "quantity": 2, "price": 2190000 },
    //     { "productId": 3, "quantity": 1, "price": 2890000 }
    //   ]
    // }
    // ─────────────────────────────────────────────────────────
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest req) {

        // 1. Kiểm tra user
        Long userId = req.userId != null ? req.userId : 2L;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Người dùng không tồn tại!");
        }

        // 2. Kiểm tra items
        if (req.items == null || req.items.isEmpty()) {
            return ResponseEntity.badRequest().body("Đơn hàng không có sản phẩm!");
        }

        // 3. Tính toán lại tổng tiền từ items (bảo vệ dữ liệu)
        BigDecimal calcTotal = req.items.stream()
                .map(i -> BigDecimal.valueOf(i.price).multiply(BigDecimal.valueOf(i.quantity)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount   = req.totalAmount   != null ? req.totalAmount   : calcTotal;
        BigDecimal discountAmt   = req.discountAmount != null ? req.discountAmount : BigDecimal.ZERO;
        BigDecimal finalAmount   = req.finalAmount   != null ? req.finalAmount   : totalAmount.subtract(discountAmt);

        // 4. Tạo đơn hàng
        Order order = new Order();
        order.setUser(user);
        order.setOrderCode("ORDER" + System.currentTimeMillis());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmt);
        order.setFinalAmount(finalAmount);
        order.setShippingAddress(req.shippingAddress != null ? req.shippingAddress : user.getAddress());
        order.setNote(req.note);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        // 5. Tạo order_items  ← trước đây bị thiếu
        for (CreateOrderRequest.OrderItemReq item : req.items) {
            Product product = productRepository.findById(item.productId).orElse(null);
            if (product == null) continue;

            OrderItem oi = new OrderItem();
            oi.setOrder(savedOrder);
            oi.setProduct(product);
            oi.setQuantity(item.quantity);
            oi.setPrice(BigDecimal.valueOf(item.price));
            orderItemRepository.save(oi);

            // Giảm tồn kho (tuỳ chọn)
            if (product.getStock() != null && product.getStock() >= item.quantity) {
                product.setStock(product.getStock() - item.quantity);
                productRepository.save(product);
            }
        }

        // 6. Tạo bản ghi payment  ← trước đây bị thiếu
        String payMethod = req.paymentMethod != null ? req.paymentMethod : "COD";
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setPaymentMethod(payMethod);
        payment.setAmount(finalAmount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentTime(LocalDateTime.now());
        paymentRepository.save(payment);

        // 7. Nếu COD → xác nhận ngay
        if ("COD".equalsIgnoreCase(payMethod)) {
            savedOrder.setStatus(OrderStatus.CONFIRMED);
            savedOrder.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(savedOrder);
        }

        // 8. Trả về kết quả
        Map<String, Object> result = toMap(savedOrder);
        result.put("paymentMethod", payMethod);

        // Nếu VNPay, trả thêm URL thanh toán (sẽ được frontend dùng để redirect)
        if ("VNPAY".equalsIgnoreCase(payMethod)) {
            String vnpayUrl = buildVNPayUrl(savedOrder.getOrderCode(), finalAmount);
            result.put("vnpayUrl", vnpayUrl);
        }

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────
    // CANCEL order
    // ─────────────────────────────────────────────────────────
    @PutMapping("/{orderCode}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null) return ResponseEntity.notFound().build();
        if (order.getStatus() != OrderStatus.PENDING)
            return ResponseEntity.badRequest().body("Không thể hủy đơn hàng ở trạng thái này!");

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toMap(orderRepository.save(order)));
    }



    // ─────────────────────────────────────────────────────────
    // DELETE  (Admin)
    // ─────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (!orderRepository.existsById(id)) return ResponseEntity.notFound().build();
        orderRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ─────────────────────────────────────────────────────────
    // VNPay Callback  (sandbox)
    // ─────────────────────────────────────────────────────────
    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(
            @RequestParam(name = "vnp_ResponseCode") String responseCode,
            @RequestParam(name = "vnp_TxnRef")       String orderCode,
            @RequestParam(name = "vnp_Amount",        required = false) String amount,
            @RequestParam(name = "vnp_TransactionNo", required = false) String txnNo) {

        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null) return ResponseEntity.badRequest().body("Đơn hàng không tồn tại");

        if ("00".equals(responseCode)) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Cập nhật payment
            if (order.getPayment() != null) {
                Payment p = order.getPayment();
                p.setStatus(PaymentStatus.PAID);
                p.setTransactionCode(txnNo);
                p.setPaymentTime(LocalDateTime.now());
                paymentRepository.save(p);
            }

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("success", true);
            res.put("orderCode", orderCode);
            res.put("message", "Thanh toán thành công!");
            return ResponseEntity.ok(res);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", false);
        res.put("orderCode", orderCode);
        res.put("message", "Thanh toán thất bại. Mã lỗi: " + responseCode);
        return ResponseEntity.ok(res);
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    /**
     * Tạo URL VNPay Sandbox.
     * Thực tế bạn cần thêm chữ ký HMAC-SHA512, nhưng đây là khung cơ bản.
     */
    private String buildVNPayUrl(String orderCode, BigDecimal amount) {
        // Số tiền VNPay tính theo đơn vị VNĐ * 100
        long vnpAmount = amount.longValue() * 100L;
        String vnpTmnCode  = "DEMOTMNCODE";   // ← thay bằng TMN code thật
        String vnpHashSecret = "DEMOSECRET";  // ← thay bằng hash secret thật
        String returnUrl = "http://localhost:8080/api/orders/vnpay-return";

        // Sandbox URL cơ bản (chưa có chữ ký — cần bổ sung HMAC-SHA512 cho production)
        return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
                + "?vnp_Version=2.1.0"
                + "&vnp_Command=pay"
                + "&vnp_TmnCode=" + vnpTmnCode
                + "&vnp_Amount=" + vnpAmount
                + "&vnp_CurrCode=VND"
                + "&vnp_TxnRef=" + orderCode
                + "&vnp_OrderInfo=Thanh+toan+don+hang+" + orderCode
                + "&vnp_OrderType=other"
                + "&vnp_Locale=vn"
                + "&vnp_ReturnUrl=" + returnUrl
                + "&vnp_IpAddr=127.0.0.1"
                + "&vnp_CreateDate=20250601120000";
    }

    /** Chuyển Order entity → Map (tránh vòng lặp JSON vô hạn) */
    private Map<String, Object> toMap(Order order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              order.getId());
        m.put("orderCode",       order.getOrderCode());
        m.put("totalAmount",     order.getTotalAmount());
        m.put("discountAmount",  order.getDiscountAmount());
        m.put("finalAmount",     order.getFinalAmount());
        m.put("status",          order.getStatus()        != null ? order.getStatus().name()        : null);
        m.put("paymentStatus",   order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        m.put("shippingAddress", order.getShippingAddress());
        m.put("note",            order.getNote());
        m.put("createdAt",       order.getCreatedAt());
        m.put("updatedAt",       order.getUpdatedAt());

        // User (gọn)
        if (order.getUser() != null) {
            User u = order.getUser();
            Map<String, Object> um = new LinkedHashMap<>();
            um.put("id",       u.getId());
            um.put("username", u.getUsername());
            um.put("fullName", u.getFullName());
            um.put("email",    u.getEmail());
            um.put("phone",    u.getPhone());
            m.put("user", um);
        }

        // OrderItems (gọn)
        if (order.getOrderItems() != null) {
            m.put("orderItems", order.getOrderItems().stream().map(oi -> {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("id",       oi.getId());
                im.put("quantity", oi.getQuantity());
                im.put("price",    oi.getPrice());
                if (oi.getProduct() != null) {
                    Product p = oi.getProduct();
                    Map<String, Object> pm = new LinkedHashMap<>();
                    pm.put("id",            p.getId());
                    pm.put("name",          p.getName());
                    pm.put("imageUrl",      p.getImageUrl());
                    pm.put("price",         p.getPrice());
                    pm.put("discountPrice", p.getDiscountPrice());
                    im.put("product", pm);
                }
                return im;
            }).toList());
        }

        // Payment (gọn)
        if (order.getPayment() != null) {
            Payment pay = order.getPayment();
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id",              pay.getId());
            pm.put("paymentMethod",   pay.getPaymentMethod());
            pm.put("transactionCode", pay.getTransactionCode());
            pm.put("amount",          pay.getAmount());
            pm.put("status",          pay.getStatus() != null ? pay.getStatus().name() : null);
            pm.put("paymentTime",     pay.getPaymentTime());
            m.put("payment", pm);
        }

        return m;
    }

    // ─────────────────────────────────────────────────────────
    // REQUEST DTO  (inner records)
    // ─────────────────────────────────────────────────────────
    public static class CreateOrderRequest {
        public Long       userId;
        public String     shippingAddress;
        public String     note;
        public String     paymentMethod;      // "COD" hoặc "VNPAY"
        public BigDecimal totalAmount;
        public BigDecimal discountAmount;
        public BigDecimal finalAmount;
        public List<OrderItemReq> items;

        public static class OrderItemReq {
            public Long   productId;
            public int    quantity;
            public double price;
        }
    }

    // === CHỈ PHẦN updateStatus VÀ THÊM MỚI markRefunded — chèn vào OrderController.java hiện có ===

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestParam OrderStatus status) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(status);

            // Nếu chuyển sang ĐÃ GIAO -> tự động đánh dấu ĐÃ THANH TOÁN
            if (status == OrderStatus.DELIVERED) {
                order.setPaymentStatus(PaymentStatus.PAID);
                if (order.getPayment() != null) {
                    order.getPayment().setStatus(PaymentStatus.PAID);
                    if (order.getPayment().getPaymentTime() == null) {
                        order.getPayment().setPaymentTime(LocalDateTime.now());
                    }
                    paymentRepository.save(order.getPayment());
                }
            }

            order.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(toMap(orderRepository.save(order)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Đánh dấu đã hoàn tiền cho đơn hàng đã hủy (đã thanh toán trước đó)
    @PutMapping("/{id}/refund")
    public ResponseEntity<?> markRefunded(@PathVariable Long id) {
        return orderRepository.findById(id).map(order -> {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            if (order.getPayment() != null) {
                order.getPayment().setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(order.getPayment());
            }
            order.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(toMap(orderRepository.save(order)));
        }).orElse(ResponseEntity.notFound().build());
    }
}