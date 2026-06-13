package com.pinkgold.controller;

import com.pinkgold.entity.Category;
import com.pinkgold.entity.Product;
import com.pinkgold.repository.CategoryRepository;
import com.pinkgold.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        List<Map<String, Object>> products = productRepository.findAll()
                .stream()
                .map(this::toProductResponse)
                .toList();

        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(product -> ResponseEntity.ok(toProductResponse(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchProducts(@RequestParam String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();

        List<Map<String, Object>> products = productRepository.findAll()
                .stream()
                .filter(product -> product.getName() != null
                        && product.getName().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .map(this::toProductResponse)
                .toList();

        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Map<String, Object>>> getProductsByCategory(@PathVariable Long categoryId) {
        List<Map<String, Object>> products = productRepository.findAll()
                .stream()
                .filter(product -> product.getCategory() != null
                        && categoryId.equals(product.getCategory().getId()))
                .map(this::toProductResponse)
                .toList();

        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product request) {
        Product product = new Product();
        copyProductFields(product, request);

        ResponseEntity<String> categoryError = attachCategory(product, request);
        if (categoryError != null) {
            return categoryError;
        }

        Product savedProduct = productRepository.save(product);
        return ResponseEntity.ok(toProductResponse(savedProduct));
    }

    /*@PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product request) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        copyProductFields(product, request);

        ResponseEntity<String> categoryError = attachCategory(product, request);
        if (categoryError != null) {
            return categoryError;
        }

        Product savedProduct = productRepository.save(product);
        return ResponseEntity.ok(toProductResponse(savedProduct));
    }*/

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        productRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private void copyProductFields(Product product, Product request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStock(request.getStock());
        product.setMaterial(request.getMaterial());
        product.setWeight(request.getWeight());
        product.setImageUrl(request.getImageUrl());

        product.setStatus(request.getStatus() != null ? request.getStatus() : Boolean.TRUE);
    }

    private ResponseEntity<String> attachCategory(Product product, Product request) {
        if (request.getCategory() == null || request.getCategory().getId() == null) {
            product.setCategory(null);
            return null;
        }

        Category category = categoryRepository.findById(request.getCategory().getId()).orElse(null);
        if (category == null) {
            return ResponseEntity.badRequest().body("Danh mục không tồn tại!");
        }

        product.setCategory(category);
        return null;
    }



    private Map<String, Object> toProductResponse(Product product) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", product.getId());
        response.put("name", product.getName());
        response.put("description", product.getDescription());
        response.put("price", product.getPrice());
        response.put("discountPrice", product.getDiscountPrice());
        response.put("stock", product.getStock());
        response.put("material", product.getMaterial());
        response.put("weight", product.getWeight());
        response.put("imageUrl", product.getImageUrl());
        response.put("status", product.getStatus());
        response.put("createdAt", product.getCreatedAt());
        // ← THÊM 2 DÒNG NÀY
        response.put("availabilityType", product.getAvailabilityType() != null
                ? product.getAvailabilityType().name() : "IN_STOCK");
        response.put("restockDate", product.getRestockDate());
        response.put("category", toCategoryResponse(product.getCategory()));
        return response;
    }



    private Map<String, Object> toCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", category.getId());
        response.put("name", category.getName());
        response.put("description", category.getDescription());
        response.put("imageUrl", category.getImageUrl());
        return response;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setName(product.getName());
                    existing.setDescription(product.getDescription());
                    existing.setPrice(product.getPrice());
                    existing.setDiscountPrice(product.getDiscountPrice());
                    existing.setStock(product.getStock());
                    existing.setMaterial(product.getMaterial());
                    existing.setWeight(product.getWeight());
                    existing.setImageUrl(product.getImageUrl());
                    existing.setStatus(product.getStatus());
                    existing.setAvailabilityType(product.getAvailabilityType());
                    existing.setRestockDate(product.getRestockDate());
                    // Gắn category đúng cách
                    if (product.getCategory() != null && product.getCategory().getId() != null) {
                        categoryRepository.findById(product.getCategory().getId())
                                .ifPresent(existing::setCategory);
                    } else {
                        existing.setCategory(null);
                    }
                    return ResponseEntity.ok(toProductResponse(productRepository.save(existing)));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}