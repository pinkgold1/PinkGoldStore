package com.pinkgold.repository;

import com.pinkgold.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByStatusTrueOrderByCreatedAtDesc();

    @Query("SELECT p FROM Product p WHERE p.status = true AND (p.name LIKE %:keyword% OR p.description LIKE %:keyword%)")
    List<Product> searchProducts(String keyword);

    long countByCategoryId(Long categoryId);


}