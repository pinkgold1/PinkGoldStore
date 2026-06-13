package com.pinkgold.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {

    private Long id;
    private String code;
    private Integer discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal minOrderAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
    private Integer usedCount;
    private Boolean status;
}