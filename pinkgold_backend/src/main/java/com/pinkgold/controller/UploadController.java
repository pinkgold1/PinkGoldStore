package com.pinkgold.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    // Thư mục lưu ảnh — mặc định là "images/" trong thư mục chạy backend
    // Bạn có thể override trong application.properties:
    //   upload.dir=C:/your-project/frontend/images
    @Value("${upload.dir:images}")
    private String uploadDir;

    /**
     * POST /api/upload
     * Body: multipart/form-data, field "file"
     * Trả về: { "url": "images/ten-file.jpg" }
     *
     * Tên file được tạo tự động để tránh trùng: UUID + đuôi gốc
     * VD: upload ảnh "giay-kiem-dinh.jpg" → lưu thành "images/a3f2c1d0-giay-kiem-dinh.jpg"
     */
    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống!"));
        }

        // Lấy đuôi file gốc (.jpg, .png, ...)
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // Chỉ cho phép ảnh
        if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file ảnh: jpg, jpeg, png, gif, webp"));
        }

        // Tạo tên file duy nhất
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "-"
                + sanitizeFilename(originalFilename != null ? originalFilename : "image" + extension);

        try {
            // Tạo thư mục nếu chưa có
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);

            // Lưu file
            Path filePath = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Trả về đường dẫn tương đối (frontend dùng để <img src="...">)
            String url = uploadDir.replace("\\", "/") + "/" + fileName;

            return ResponseEntity.ok(Map.of("url", url));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Lỗi khi lưu file: " + e.getMessage())
            );
        }
    }

    /**
     * DELETE /api/upload?url=images/ten-file.jpg
     * Xóa file khỏi ổ cứng
     */
    @DeleteMapping
    public ResponseEntity<?> deleteFile(@RequestParam("url") String url) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL không hợp lệ"));
        }

        // Bảo vệ: không cho xóa file ngoài thư mục upload
        String sanitized = url.replace("\\", "/").replaceAll("\\.\\./", "");
        Path filePath = Paths.get(sanitized).normalize();

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Đã xóa file: " + sanitized));
            } else {
                return ResponseEntity.ok(Map.of("message", "File không tồn tại (bỏ qua): " + sanitized));
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Lỗi khi xóa file: " + e.getMessage())
            );
        }
    }

    // Làm sạch tên file: bỏ ký tự đặc biệt, giữ chữ, số, gạch ngang, dấu chấm
    private String sanitizeFilename(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9.\\-_]", "-")
                .replaceAll("-{2,}", "-");
    }
}