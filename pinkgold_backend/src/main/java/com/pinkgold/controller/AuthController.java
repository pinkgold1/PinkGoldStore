package com.pinkgold.controller;

import com.pinkgold.entity.Role;
import com.pinkgold.entity.User;
import com.pinkgold.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    // === ĐĂNG KÝ ===
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Email đã được sử dụng!");
        }

        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().body("Tên đăng nhập đã tồn tại!");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(request.password());        // TODO: Mã hóa sau
        user.setRole(Role.CUSTOMER);                 // ← Đã sửa
        user.setStatus(true);

        userRepository.save(user);

        return ResponseEntity.ok("Đăng ký thành công!");
    }

    // === ĐĂNG NHẬP ===
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        return userRepository.findByEmail(request.email())
                .map(user -> {
                    if (user.getPassword().equals(request.password())) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "Đăng nhập thành công!");
                        response.put("user", Map.of(
                                "id", user.getId(),
                                "username", user.getUsername(),
                                "fullName", user.getFullName(),
                                "email", user.getEmail(),
                                "role", user.getRole()
                        ));
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.badRequest().body("Mật khẩu không đúng!");
                    }
                })
                .orElse(ResponseEntity.badRequest().body("Email không tồn tại!"));
    }

    // DTO
    record RegisterRequest(String username, String fullName, String email, String phone, String password) {}
    record LoginRequest(String email, String password) {}
}