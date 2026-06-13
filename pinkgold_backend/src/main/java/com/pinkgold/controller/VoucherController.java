package com.pinkgold.controller;

import com.pinkgold.entity.Voucher;
import com.pinkgold.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VoucherController {

    private final VoucherRepository voucherRepository;

    // GET ALL (Admin)
    @GetMapping
    public ResponseEntity<List<Voucher>> getAllVouchers() {
        return ResponseEntity.ok(voucherRepository.findAll());
    }

    // CHECK VOUCHER (Customer)
    @GetMapping("/check/{code}")
    public ResponseEntity<?> checkVoucher(@PathVariable String code) {
        Optional<Voucher> voucherOpt = voucherRepository.findByCode(code);

        if (voucherOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Mã giảm giá không tồn tại!");
        }

        Voucher voucher = voucherOpt.get();
        if (!voucher.getStatus() || voucher.getUsedCount() >= voucher.getQuantity()) {
            return ResponseEntity.badRequest().body("Mã giảm giá đã hết lượt sử dụng hoặc không còn hiệu lực!");
        }

        return ResponseEntity.ok(voucher);
    }

    // CREATE (Admin)
    @PostMapping
    public ResponseEntity<Voucher> createVoucher(@RequestBody Voucher voucher) {
        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.ok(saved);
    }

    // UPDATE (Admin)
    @PutMapping("/{id}")
    public ResponseEntity<Voucher> updateVoucher(@PathVariable Long id, @RequestBody Voucher voucher) {
        return voucherRepository.findById(id)
                .map(existing -> {
                    existing.setCode(voucher.getCode());
                    existing.setDiscountPercent(voucher.getDiscountPercent());
                    existing.setDiscountAmount(voucher.getDiscountAmount());
                    existing.setMinOrderAmount(voucher.getMinOrderAmount());
                    existing.setStartDate(voucher.getStartDate());
                    existing.setEndDate(voucher.getEndDate());
                    existing.setQuantity(voucher.getQuantity());
                    existing.setStatus(voucher.getStatus());
                    return ResponseEntity.ok(voucherRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE (Admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        if (voucherRepository.existsById(id)) {
            voucherRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}