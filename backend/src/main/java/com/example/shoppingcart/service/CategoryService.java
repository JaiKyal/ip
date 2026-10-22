package com.example.shoppingcart.service;

import com.example.shoppingcart.dto.CategoryResponse;
import com.example.shoppingcart.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    public List<CategoryResponse> listAll() {
        return categories.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }
}
