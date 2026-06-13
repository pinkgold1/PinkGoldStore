package com.pinkgold.controller;

import com.pinkgold.entity.User;
import com.pinkgold.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(toUserResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        Long userId = 2L;
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(toUserResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User request) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? request.getStatus() : Boolean.TRUE);

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(toUserResponse(savedUser));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody User request) {
        Long userId = 2L;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setDateOfBirth(request.getDateOfBirth());

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(toUserResponse(savedUser));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setStatus(!Boolean.TRUE.equals(user.getStatus()));
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(toUserResponse(savedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/change-password")
    public ResponseEntity<?> changePassword(
            @RequestParam(defaultValue = "2") Long userId,
            @RequestBody ChangePasswordRequest request) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (!user.getPassword().equals(request.oldPassword)) {
            return ResponseEntity.badRequest().body("Mật khẩu hiện tại không đúng!");
        }

        user.setPassword(request.newPassword);
        userRepository.save(user);
        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }

    public static class ChangePasswordRequest {
        public String oldPassword;
        public String newPassword;
    }

    private Map<String, Object> toUserResponse(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("address", user.getAddress());
        response.put("dateOfBirth", user.getDateOfBirth());
        response.put("role", user.getRole() == null ? null : user.getRole().name());
        response.put("status", user.getStatus());
        response.put("createdAt", user.getCreatedAt());
        response.put("updatedAt", user.getUpdatedAt());
        return response;
    }


}