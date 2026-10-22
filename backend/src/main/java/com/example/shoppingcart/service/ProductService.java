package com.example.shoppingcart.service;

import com.example.shoppingcart.dto.PageResponse;
import com.example.shoppingcart.dto.ProductResponse;
import com.example.shoppingcart.entity.Product;
import com.example.shoppingcart.exception.ResourceNotFoundException;
import com.example.shoppingcart.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    /** Public product listing: only active products, optionally filtered by name text and category. */
    public PageResponse<ProductResponse> search(String text, Long categoryId, ProductSort sort, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> cb.isTrue(root.get("active"));

        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (text != null && !text.isBlank()) {
            String pattern = "%" + escapeLike(text.trim().toLowerCase()) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\'));
        }

        return PageResponse.from(
                products.findAll(spec, PageRequest.of(page, size, sort.toSort())).map(ProductResponse::from));
    }

    public ProductResponse getActiveProduct(Long id) {
        return products.findByIdAndActiveTrue(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    /** Makes %, _ and \ in user text match literally instead of acting as SQL wildcards. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
