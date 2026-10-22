package com.example.shoppingcart.service;

import com.example.shoppingcart.exception.BadRequestException;
import java.util.Arrays;
import org.springframework.data.domain.Sort;

/** The sort orders the storefront may ask for. Anything else is rejected, so clients cannot sort by arbitrary columns. */
public enum ProductSort {
    FEATURED("featured", Sort.by(Sort.Order.asc("id"))),
    NEWEST("newest", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    NAME("name", Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))),
    PRICE_ASC("price_asc", Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"))),
    PRICE_DESC("price_desc", Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id")));

    private final String key;
    private final Sort sort;

    ProductSort(String key, Sort sort) {
        this.key = key;
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }

    public static ProductSort fromKey(String key) {
        if (key == null || key.isBlank()) {
            return FEATURED;
        }
        return Arrays.stream(values())
                .filter(s -> s.key.equalsIgnoreCase(key.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Unknown sort '" + key + "'. Use one of: featured, newest, name, price_asc, price_desc"));
    }
}
