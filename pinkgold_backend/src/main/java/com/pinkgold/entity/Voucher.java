package com.pinkgold.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private Integer discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal minOrderAmount;

    private LocalDate startDate;
    private LocalDate endDate;

    private Integer quantity = 1;
    private Integer usedCount = 0;

    private Boolean status = true;
}
