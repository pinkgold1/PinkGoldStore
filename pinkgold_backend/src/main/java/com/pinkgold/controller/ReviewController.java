package com.pinkgold.controller;

import com.pinkgold.entity.Product;
import com.pinkgold.entity.Review;
import com.pinkgold.entity.User;
import com.pinkgold.repository.ProductRepository;
import com.pinkgold.repository.ReviewRepository;
import com.pinkgold.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewRepository  reviewRepository;
    private final UserRepository    userRepository;
    private final ProductRepository productRepository;

    // ── Chuyển Review entity → Map gọn (tránh vòng lặp JSON) ──
    private Map<String, Object> toMap(Review r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",        r.getId());
        m.put("rating",    r.getRating());
        m.put("comment",   r.getComment());
        m.put("createdAt", r.getCreatedAt());

        // User (chỉ lấy các field cần thiết)
        if (r.getUser() != null) {
            User u = r.getUser();
            Map<String, Object> um = new LinkedHashMap<>();
            um.put("id",       u.getId());
            um.put("username", u.getUsername());
            um.put("fullName", u.getFullName());
            um.put("email",    u.getEmail());
            m.put("user", um);
        }

        // Product (chỉ id và tên)
        if (r.getProduct() != null) {
            Product p = r.getProduct();
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id",   p.getId());
            pm.put("name", p.getName());
            m.put("product", pm);
        }

        return m;
    }

    // ── GET tất cả đánh giá của một sản phẩm ──
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Map<String, Object>>> getReviewsByProduct(@PathVariable Long productId) {
        List<Map<String, Object>> result = reviewRepository.findByProductId(productId)
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ── GET đánh giá của tôi ──
    @GetMapping("/my-reviews")
    public ResponseEntity<List<Map<String, Object>>> getMyReviews(
            @RequestParam(defaultValue = "2") Long userId) {
        List<Map<String, Object>> result = reviewRepository.findByUserId(userId)
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ── POST thêm đánh giá mới ──
    @PostMapping
    public ResponseEntity<?> addReview(@RequestParam Long productId,
                                       @RequestParam int rating,
                                       @RequestParam String comment,
                                       @RequestParam(defaultValue = "2") Long userId) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Người dùng không tồn tại!");

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return ResponseEntity.badRequest().body("Sản phẩm không tồn tại!");

        if (rating < 1 || rating > 5)
            return ResponseEntity.badRequest().body("Đánh giá phải từ 1 đến 5 sao!");

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);
        return ResponseEntity.ok(toMap(saved));
    }

    // ── PUT sửa đánh giá (chỉ chủ sở hữu) ──
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {

        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) return ResponseEntity.notFound().build();

        Object ratingObj  = body.get("rating");
        Object commentObj = body.get("comment");

        if (ratingObj != null) {
            int rating = Integer.parseInt(String.valueOf(ratingObj));
            if (rating < 1 || rating > 5)
                return ResponseEntity.badRequest().body("Đánh giá phải từ 1 đến 5 sao!");
            review.setRating(rating);
        }

        if (commentObj != null) {
            String comment = String.valueOf(commentObj).trim();
            if (comment.isEmpty())
                return ResponseEntity.badRequest().body("Nội dung đánh giá không được để trống!");
            review.setComment(comment);
        }

        Review saved = reviewRepository.save(review);
        return ResponseEntity.ok(toMap(saved));
    }

    // ── DELETE xóa đánh giá ──
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        if (!reviewRepository.existsById(id)) return ResponseEntity.notFound().build();
        reviewRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}