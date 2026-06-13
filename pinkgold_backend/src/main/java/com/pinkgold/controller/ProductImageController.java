package com.pinkgold.controller;

import com.pinkgold.entity.Product;
import com.pinkgold.entity.ProductImage;
import com.pinkgold.repository.ProductImageRepository;
import com.pinkgold.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-images")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductImageController {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    // GET: lấy tất cả ảnh của một sản phẩm
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Map<String, Object>>> getImagesByProduct(@PathVariable Long productId) {
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        return ResponseEntity.ok(images.stream().map(this::toMap).toList());
    }

    // POST: thêm ảnh mới cho sản phẩm
    // Gọi: POST /api/product-images?productId=1&imageUrl=images/abc.jpg&isPrimary=false
    @PostMapping
    public ResponseEntity<?> addProductImage(@RequestParam Long productId,
                                             @RequestParam String imageUrl,
                                             @RequestParam(defaultValue = "false") Boolean isPrimary) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body("Sản phẩm không tồn tại!");
        }

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(imageUrl);
        image.setIsPrimary(isPrimary);

        ProductImage saved = productImageRepository.save(image);
        return ResponseEntity.ok(toMap(saved));
    }

    // PUT: cập nhật ảnh (đổi URL hoặc đặt làm ảnh chính)
    // Gọi: PUT /api/product-images/5?imageUrl=images/abc.jpg&isPrimary=true
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProductImage(@PathVariable Long id,
                                                @RequestParam String imageUrl,
                                                @RequestParam Boolean isPrimary) {
        return productImageRepository.findById(id)
                .map(image -> {
                    image.setImageUrl(imageUrl);
                    image.setIsPrimary(isPrimary);
                    return ResponseEntity.ok(toMap(productImageRepository.save(image)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE: xóa ảnh
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductImage(@PathVariable Long id) {
        if (productImageRepository.existsById(id)) {
            productImageRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Chuyển entity → Map gọn, không có vòng lặp JSON
    private Map<String, Object> toMap(ProductImage img) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",        img.getId());
        m.put("imageUrl",  img.getImageUrl());
        m.put("isPrimary", img.getIsPrimary());
        // Chỉ lấy id của product, không serialize toàn bộ object
        if (img.getProduct() != null) {
            m.put("productId", img.getProduct().getId());
        }
        return m;
    }
}