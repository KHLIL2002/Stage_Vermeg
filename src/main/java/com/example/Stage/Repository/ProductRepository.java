package com.example.Stage.Repository;

import com.example.Stage.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByProductCode(String productCode);
}