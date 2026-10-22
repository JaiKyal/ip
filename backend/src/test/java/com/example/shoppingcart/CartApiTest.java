package com.example.shoppingcart;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** The cart rules, checked through the real HTTP layer and database. */
class CartApiTest extends AbstractPostgresIntegrationTest {

    @Test
    void aNewCustomerHasAnEmptyCart() throws Exception {
        String token = registerCustomerToken(uniqueEmail());
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalQuantity").value(0))
                .andExpect(jsonPath("$.subtotal").value(0.0))
                .andExpect(jsonPath("$.hasUnavailableItems").value(false));
    }

    @Test
    void addingAProductUsesThePriceFromTheDatabase() throws Exception {
        long productId = createProduct("250.50", 10);
        String token = registerCustomerToken(uniqueEmail());
        add(token, productId, 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].unitPrice").value(250.50))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(501.00))
                .andExpect(jsonPath("$.items[0].available").value(true))
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(501.00));
    }

    @Test
    void aClientCannotSetItsOwnPrice() throws Exception {
        long productId = createProduct("250.50", 10);
        String token = registerCustomerToken(uniqueEmail());
        mockMvc.perform(post("/api/cart/items").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1,\"price\":0.01,\"unitPrice\":0.01}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].unitPrice").value(250.50))
                .andExpect(jsonPath("$.subtotal").value(250.50));
    }

    @Test
    void addingTheSameProductAgainMergesIntoOneLine() throws Exception {
        long productId = createProduct("10.00", 10);
        String token = registerCustomerToken(uniqueEmail());
        add(token, productId, 2).andExpect(status().isCreated());
        add(token, productId, 3).andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.subtotal").value(50.00));
    }

    @Test
    void quantityCanNeverExceedStockEvenAcrossSeveralAdds() throws Exception {
        long productId = createProduct("10.00", 3);
        String token = registerCustomerToken(uniqueEmail());
        add(token, productId, 4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only 3 left in stock")));
        add(token, productId, 2).andExpect(status().isCreated());
        add(token, productId, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only 3 left in stock")));
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void outOfStockProductsCannotBeAdded() throws Exception {
        long productId = createProduct("10.00", 0);
        add(registerCustomerToken(uniqueEmail()), productId, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("out of stock")));
    }

    @Test
    void hiddenAndUnknownProductsAreNotFound() throws Exception {
        long productId = createProduct("10.00", 5);
        jdbc.update("UPDATE products SET active = FALSE WHERE id = ?", productId);
        String token = registerCustomerToken(uniqueEmail());
        add(token, productId, 1).andExpect(status().isNotFound());
        add(token, 999999L, 1).andExpect(status().isNotFound());
    }

    @Test
    void invalidQuantitiesAndMissingFieldsAreRejected() throws Exception {
        long productId = createProduct("10.00", 500);
        String token = registerCustomerToken(uniqueEmail());
        add(token, productId, 0).andExpect(status().isBadRequest());
        add(token, productId, -1).andExpect(status().isBadRequest());
        add(token, productId, 100).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/cart/items").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quantityCanBeChangedWithinStock() throws Exception {
        long productId = createProduct("10.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        long itemId = addAndGetItemId(token, productId, 1);
        update(token, itemId, 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.subtotal").value(40.00));
        update(token, itemId, 6).andExpect(status().isConflict());
        update(token, itemId, 0).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items[0].quantity").value(4));
    }

    @Test
    void itemsCanBeRemoved() throws Exception {
        long productId = createProduct("10.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        long itemId = addAndGetItemId(token, productId, 2);
        mockMvc.perform(delete("/api/cart/items/{id}", itemId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.subtotal").value(0.0));
        mockMvc.perform(delete("/api/cart/items/{id}", itemId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void nobodyCanTouchAnotherCustomersCartItems() throws Exception {
        long productId = createProduct("10.00", 5);
        String alice = registerCustomerToken(uniqueEmail());
        String bob = registerCustomerToken(uniqueEmail());
        long aliceItem = addAndGetItemId(alice, productId, 2);

        update(bob, aliceItem, 1).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/cart/items/{id}", aliceItem).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void lowerStockMarksTheCartLineUnavailable() throws Exception {
        long productId = createProduct("10.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        add(token, productId, 3).andExpect(status().isCreated());
        jdbc.update("UPDATE products SET stock_quantity = 1 WHERE id = ?", productId);
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items[0].available").value(false))
                .andExpect(jsonPath("$.items[0].stockQuantity").value(1))
                .andExpect(jsonPath("$.hasUnavailableItems").value(true));
    }

    @Test
    void theCartNeedsALogin() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- helpers

    private ResultActions add(String token, long productId, int quantity) throws Exception {
        return mockMvc.perform(post("/api/cart/items").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", quantity))));
    }

    private ResultActions update(String token, long itemId, int quantity) throws Exception {
        return mockMvc.perform(patch("/api/cart/items/{id}", itemId).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":" + quantity + "}"));
    }

    private long addAndGetItemId(String token, long productId, int quantity) throws Exception {
        String body = add(token, productId, quantity).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("items").get(0).get("id").asLong();
    }
}
