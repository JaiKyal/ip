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

        if (sort == ProductSort.FEATURED) {
            // Featured: products that can be bought first, then by id. Spring's Sort cannot express
            // "in stock first", so the order is added to the query itself (not to the count query).
            spec = spec.and((root, query, cb) -> {
                if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                    query.orderBy(
                            cb.asc(cb.selectCase().when(cb.greaterThan(root.get("stockQuantity"), 0), 0).otherwise(1)),
                            cb.asc(root.get("id")));
                }
                return null;
            });
            return PageResponse.from(products.findAll(spec, PageRequest.of(page, size)).map(ProductResponse::from));
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
