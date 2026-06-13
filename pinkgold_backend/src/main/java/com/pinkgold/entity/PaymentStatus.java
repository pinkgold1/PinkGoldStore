package com.pinkgold.entity;

public enum PaymentStatus {
    UNPAID,     // Chưa thanh toán
    PAID,       // Đã thanh toán
    REFUNDED,   // Đã hoàn tiền

    // Các giá trị cũ trong bảng payments.status.
    // Giữ lại để Hibernate đọc được dữ liệu đã insert trước đó.
    PENDING,
    SUCCESS,
    FAILED
}