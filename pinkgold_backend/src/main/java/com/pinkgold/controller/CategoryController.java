package com.pinkgold.controller;

import com.pinkgold.entity.Category;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllCategories() {
        List<Map<String, Object>> categories = categoryRepository.findAll()
                .stream()
                .map(this::toCategoryResponse)
                .toList();

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCategoryById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(category -> ResponseEntity.ok(toCategoryResponse(category)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        category.setId(null);
        Category savedCategory = categoryRepository.save(category);
        return ResponseEntity.ok(savedCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category request) {
        return categoryRepository.findById(id)
                .map(category -> {
                    category.setName(request.getName());
                    category.setDescription(request.getDescription());
                    category.setImageUrl(request.getImageUrl());
                    return ResponseEntity.ok(categoryRepository.save(category));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> toCategoryResponse(Category category) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", category.getId());
        response.put("name", category.getName());
        response.put("description", category.getDescription());
        response.put("imageUrl", category.getImageUrl());
        response.put("createdAt", category.getCreatedAt());
        response.put("productCount",
                category.getProducts() != null ? category.getProducts().size() : 0);
        return response;
    }
}