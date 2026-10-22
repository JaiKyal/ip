package com.example.shoppingcart.controller;

import com.example.shoppingcart.dto.PageResponse;
import com.example.shoppingcart.dto.ProductResponse;
import com.example.shoppingcart.service.ProductService;
import com.example.shoppingcart.service.ProductSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public (no login) product browsing for the storefront. */
@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return productService.search(q, categoryId, ProductSort.fromKey(sort), page, size);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.getActiveProduct(id);
    }
}
