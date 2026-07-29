package com.example.Stage.Controller;

import com.example.Stage.Model.Product;
import com.example.Stage.Repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        // Retourne tous les produits créés par ton DataLoader
        return productRepository.findAll();
    }
}