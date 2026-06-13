package com.pinkgold.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderCode;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String status;           // PENDING, CONFIRMED, ...
    private String paymentStatus;    // PAID, UNPAID,...
    private String shippingAddress;
    private String note;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> orderItems;
}