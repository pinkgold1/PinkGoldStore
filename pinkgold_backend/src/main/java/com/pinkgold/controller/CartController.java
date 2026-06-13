package com.pinkgold.controller;

import com.pinkgold.entity.CartItem;
import com.pinkgold.entity.Product;
import com.pinkgold.entity.User;
import com.pinkgold.repository.CartItemRepository;
import com.pinkgold.repository.ProductRepository;
import com.pinkgold.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // GET giỏ hàng theo userId
    @GetMapping
    public ResponseEntity<?> getCart(@RequestParam Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return ResponseEntity.ok(items);
    }

    // THÊM vào giỏ hàng
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam Long userId,
                                       @RequestParam Long productId,
                                       @RequestParam(defaultValue = "1") int quantity) {

        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Người dùng không tồn tại!");

        Product product = productRepository.findById(productId)
                .orElse(null);
        if (product == null) return ResponseEntity.badRequest().body("Sản phẩm không tồn tại!");

        // Kiểm tra nếu sản phẩm đã có trong giỏ → tăng số lượng
        List<CartItem> existing = cartItemRepository.findByUserId(userId);
        Optional<CartItem> existingItem = existing.stream()
                .filter(c -> c.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
            return ResponseEntity.ok("Đã cập nhật số lượng trong giỏ hàng");
        }

        CartItem cartItem = new CartItem();
        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        return ResponseEntity.ok("Đã thêm vào giỏ hàng");
    }

    // CẬP NHẬT số lượng
    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long cartItemId,
                                            @RequestParam int quantity) {
        return cartItemRepository.findById(cartItemId)
                .map(item -> {
                    if (quantity <= 0) {
                        cartItemRepository.delete(item);
                        return ResponseEntity.ok("Đã xóa sản phẩm khỏi giỏ hàng");
                    }
                    item.setQuantity(quantity);
                    cartItemRepository.save(item);
                    return ResponseEntity.ok("Đã cập nhật số lượng");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // XÓA một sản phẩm
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long cartItemId) {
        if (cartItemRepository.existsById(cartItemId)) {
            cartItemRepository.deleteById(cartItemId);
            return ResponseEntity.ok("Đã xóa khỏi giỏ hàng");
        }
        return ResponseEntity.notFound().build();
    }

    // XÓA toàn bộ giỏ của user
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(@RequestParam Long userId) {
        cartItemRepository.deleteByUserId(userId);
        return ResponseEntity.ok("Đã xóa toàn bộ giỏ hàng");
    }
}