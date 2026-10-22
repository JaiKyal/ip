package com.example.shoppingcart;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** Exercises the public product API end to end against the seeded demo catalog (migration V2). */
class ProductApiTest extends AbstractPostgresIntegrationTest {

    @Test
    void firstPageHasTwelveOfFifteenProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(12)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void secondPageHasTheRemainingThree() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    @Test
    void productJsonHasPriceStockAndCategoryAndNoInternalFields() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "Wireless Earbuds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Wireless Earbuds"))
                .andExpect(jsonPath("$.items[0].price").value(1499.00))
                .andExpect(jsonPath("$.items[0].stockQuantity").value(40))
                .andExpect(jsonPath("$.items[0].inStock").value(true))
                .andExpect(jsonPath("$.items[0].category.name").value("Electronics"))
                .andExpect(jsonPath("$.items[0].imageUrl", notNullValue()))
                .andExpect(jsonPath("$.items[0].active").doesNotExist())
                .andExpect(jsonPath("$.items[0].createdAt").doesNotExist());
    }

    @Test
    void filtersByCategory() throws Exception {
        long fashionId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Fashion'", Long.class);
        mockMvc.perform(get("/api/products").param("categoryId", String.valueOf(fashionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.items[*].category.name", org.hamcrest.Matchers.everyItem(is("Fashion"))));
    }

    @Test
    void searchIsCaseInsensitiveAndMatchesPartsOfTheName() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "BOTTLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Stainless Steel Water Bottle"));
    }

    @Test
    void searchTreatsPercentSignAsPlainText() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchWithSqlInjectionAttemptIsHarmless() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "'; DROP TABLE products; --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/products"))
                .andExpect(jsonPath("$.totalElements").value(15));
    }

    @Test
    void sortsByPrice() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "price_asc"))
                .andExpect(jsonPath("$.items[0].name").value("Men Cotton T-Shirt"))
                .andExpect(jsonPath("$.items[0].price").value(499.00));
        mockMvc.perform(get("/api/products").param("sort", "price_desc"))
                .andExpect(jsonPath("$.items[0].name").value("Smart Watch"))
                .andExpect(jsonPath("$.items[0].price").value(2999.00));
    }

    @Test
    void outOfStockProductsAreListedAndClearlyMarked() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "USB-C"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].stockQuantity").value(0))
                .andExpect(jsonPath("$.items[0].inStock").value(false));
    }

    @Test
    @Transactional
    void inactiveProductsAreHiddenFromListAndDetail() throws Exception {
        long id = jdbc.queryForObject("SELECT id FROM products WHERE name = 'Smart Watch'", Long.class);
        jdbc.update("UPDATE products SET active = FALSE WHERE id = ?", id);

        mockMvc.perform(get("/api/products").param("q", "Smart Watch"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailReturnsOneProduct() throws Exception {
        long id = jdbc.queryForObject("SELECT id FROM products WHERE name = 'LED Desk Lamp'", Long.class);
        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("LED Desk Lamp"))
                .andExpect(jsonPath("$.stockQuantity").value(3))
                .andExpect(jsonPath("$.category.name").value("Home & Kitchen"));
    }

    @Test
    void unknownProductIsA404WithJsonError() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", not(org.hamcrest.Matchers.containsString("Exception"))))
                .andExpect(jsonPath("$.path").value("/api/products/999999"));
    }

    @Test
    void invalidInputIsA400NotA500() throws Exception {
        mockMvc.perform(get("/api/products/{id}", "abc")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("size", "1000")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("categoryId", "abc")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("sort", "password_hash"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Unknown sort")));
        mockMvc.perform(get("/api/products").param("q", "x".repeat(101))).andExpect(status().isBadRequest());
    }

    @Test
    void productsCannotBeChangedWithoutLoggingIn() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/products/1"))
                .andExpect(status().isUnauthorized());
    }
}
