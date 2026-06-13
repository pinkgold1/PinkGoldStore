package com.pinkgold.controller;

import com.pinkgold.entity.Order;
import com.pinkgold.entity.OrderStatus;
import com.pinkgold.entity.Payment;
import com.pinkgold.entity.PaymentStatus;
import com.pinkgold.repository.OrderRepository;
import com.pinkgold.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    // Lấy tất cả thanh toán (Admin)
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentRepository.findAll());
    }

    // Lấy thanh toán theo order
    @GetMapping("/order/{orderCode}")
    public ResponseEntity<?> getPaymentByOrder(@PathVariable String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null) {
            return ResponseEntity.badRequest().body("Đơn hàng không tồn tại!");
        }
        if (order.getPayment() == null) {
            return ResponseEntity.badRequest().body("Đơn hàng này chưa có thanh toán!");
        }
        return ResponseEntity.ok(order.getPayment());
    }

    // Tạo thanh toán (Customer click "Thanh toán")
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestParam String orderCode,
                                           @RequestParam String paymentMethod) {
        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null) {
            return ResponseEntity.badRequest().body("Đơn hàng không tồn tại!");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod); // VNPAY hoặc COD
        payment.setAmount(order.getFinalAmount());
        payment.setStatus(PaymentStatus.UNPAID);
        payment.setPaymentTime(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        return ResponseEntity.ok(saved);
    }

    // Callback từ VNPay sau khi thanh toán
    @PostMapping("/vnpay-callback")
    public ResponseEntity<?> vnpayCallback(@RequestBody Map<String, String> params) {
        String orderCode     = params.get("orderCode");
        String responseCode  = params.get("vnp_ResponseCode");
        String transactionId = params.get("vnp_TransactionNo");

        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null) {
            return ResponseEntity.badRequest().body("Đơn hàng không tồn tại!");
        }

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            // Cập nhật bản ghi Payment
            Payment payment = order.getPayment();
            if (payment != null) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setTransactionCode(transactionId);
                payment.setPaymentTime(LocalDateTime.now());
                paymentRepository.save(payment);
            }

            return ResponseEntity.ok("Thanh toán thành công!");
        }

        // Thanh toán thất bại
        if (order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.UNPAID);
            paymentRepository.save(order.getPayment());
        }
        return ResponseEntity.badRequest().body("Thanh toán thất bại! Mã lỗi: " + responseCode);
    }

    // Xác nhận thanh toán COD (Admin)
    @PutMapping("/{id}/confirm-cod")
    public ResponseEntity<?> confirmCodPayment(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(payment -> {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaymentTime(LocalDateTime.now());
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    order.setPaymentStatus(PaymentStatus.PAID);
                    orderRepository.save(order);

                    return ResponseEntity.ok("Xác nhận thanh toán COD thành công!");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Hoàn tiền (Admin)
    @PutMapping("/{id}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(payment -> {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    order.setPaymentStatus(PaymentStatus.REFUNDED);
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    return ResponseEntity.ok("Hoàn tiền thành công!");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Xóa thanh toán (Admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        if (paymentRepository.existsById(id)) {
            paymentRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}