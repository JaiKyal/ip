package com.example.shoppingcart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Checkout, orders and the guarantees around them: stock, prices, payments, double submits, privacy. */
class CheckoutApiTest extends AbstractPostgresIntegrationTest {

    // -------------------------------------------------------------- happy paths

    @Test
    void upiCheckoutCreatesAPaidConfirmedOrderAndMovesStock() throws Exception {
        long productId = createProduct("100.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 2);

        checkout(token, key(), "UPI")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber", matchesPattern("ORD-\\d{8}-[A-Z2-9]{6}")))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.subtotal").value(200.00))
                .andExpect(jsonPath("$.totalAmount").value(200.00))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(100.00))
                .andExpect(jsonPath("$.items[0].lineTotal").value(200.00))
                .andExpect(jsonPath("$.shipping.city").value("Bengaluru"))
                .andExpect(jsonPath("$.payment.simulated").value(true))
                .andExpect(jsonPath("$.payment.status").value("PAID"))
                .andExpect(jsonPath("$.payment.reference", startsWith("SIM-")));

        assertThat(stockOf(productId)).isEqualTo(3);
        assertThat(countRows("orders")).isEqualTo(1);
        assertThat(countRows("order_items")).isEqualTo(1);
        assertThat(countRows("payments")).isEqualTo(1);
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void cashOnDeliveryStaysPendingWithNoPaymentRecordYet() throws Exception {
        long productId = createProduct("100.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 1);

        checkout(token, key(), "COD")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.payment").doesNotExist());

        assertThat(stockOf(productId)).isEqualTo(4);
        assertThat(countRows("payments")).isZero();
    }

    @Test
    void anOrderWithSeveralProductsAddsUpAndMovesEachStock() throws Exception {
        long a = createProduct("19.99", 10);
        long b = createProduct("5.05", 10);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, a, 3);
        addToCart(token, b, 2);

        checkout(token, key(), "CARD")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalAmount").value(70.07));   // 3 x 19.99 + 2 x 5.05, exact decimals

        assertThat(stockOf(a)).isEqualTo(7);
        assertThat(stockOf(b)).isEqualTo(8);
    }

    // ---------------------------------------------------------------- bad input

    @Test
    void checkingOutAnEmptyCartIsRejected() throws Exception {
        String token = registerCustomerToken(uniqueEmail());
        checkout(token, key(), "UPI")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Your cart is empty"));
        assertThat(countRows("orders")).isZero();
    }

    @Test
    void theIdempotencyKeyIsRequiredAndMustLookRight() throws Exception {
        String token = registerCustomerToken(uniqueEmail());
        mockMvc.perform(post("/api/checkout").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body("UPI")))
                .andExpect(status().isBadRequest());
        checkout(token, "short", "UPI").andExpect(status().isBadRequest());
        checkout(token, "has spaces and ! symbols", "UPI").andExpect(status().isBadRequest());
    }

    @Test
    void shippingAndPaymentMethodAreValidated() throws Exception {
        long productId = createProduct("10.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 1);

        Map<String, Object> badShipping = new HashMap<>(shipping());
        badShipping.put("pincode", "12");
        badShipping.put("phone", "abc");
        badShipping.put("name", "");
        checkoutRaw(token, key(), Map.of("shipping", badShipping, "paymentMethod", "UPI"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("shipping.pincode")))
                .andExpect(jsonPath("$.message", containsString("shipping.phone")))
                .andExpect(jsonPath("$.message", containsString("shipping.name")));

        checkoutRaw(token, key(), Map.of("paymentMethod", "UPI")).andExpect(status().isBadRequest());
        checkoutRaw(token, key(), Map.of("shipping", shipping())).andExpect(status().isBadRequest());
        checkoutRaw(token, key(), Map.of("shipping", shipping(), "paymentMethod", "BITCOIN")).andExpect(status().isBadRequest());
        assertThat(countRows("orders")).isZero();
        assertThat(stockOf(productId)).isEqualTo(5);
    }

    @Test
    void pricesAndTotalsSentByTheBrowserAreIgnored() throws Exception {
        long productId = createProduct("100.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 2);

        Map<String, Object> tampered = new HashMap<>();
        tampered.put("shipping", shipping());
        tampered.put("paymentMethod", "UPI");
        tampered.put("totalAmount", 0.01);
        tampered.put("subtotal", 0.01);
        tampered.put("items", List.of(Map.of("productId", productId, "quantity", 99, "unitPrice", 0.01)));
        checkoutRaw(token, key(), tampered)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(200.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(100.00));
        assertThat(stockOf(productId)).isEqualTo(3);
    }

    @Test
    void checkoutNeedsALogin() throws Exception {
        mockMvc.perform(post("/api/checkout").header("Idempotency-Key", key()).contentType(MediaType.APPLICATION_JSON).content(body("UPI")))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------- stock and consistency

    @Test
    void ifStockDropsAfterAddingToTheCartCheckoutFailsAndChangesNothing() throws Exception {
        long productId = createProduct("10.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 3);
        jdbc.update("UPDATE products SET stock_quantity = 2 WHERE id = ?", productId);

        checkout(token, key(), "UPI")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only 2 left in stock")));

        assertThat(stockOf(productId)).isEqualTo(2);
        assertThat(countRows("orders")).isZero();
        assertThat(countRows("payments")).isZero();
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void aProductHiddenAfterAddingToTheCartBlocksCheckout() throws Exception {
        long productId = createProduct("10.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 1);
        jdbc.update("UPDATE products SET active = FALSE WHERE id = ?", productId);

        checkout(token, key(), "UPI")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("no longer available")));
        assertThat(countRows("orders")).isZero();
    }

    @Test
    void aDeclinedPaymentRollsBackEverythingAndTheSameKeyCanBeRetried() throws Exception {
        long productId = createProduct("100.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 2);
        String key = key();

        paymentGateway.setDecline(true);
        checkout(token, key, "CARD")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message", containsString("declined")));

        assertThat(stockOf(productId)).isEqualTo(5);          // stock untouched
        assertThat(countRows("orders")).isZero();              // no half-made order
        assertThat(countRows("order_items")).isZero();
        assertThat(countRows("payments")).isZero();
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items[0].quantity").value(2));   // cart kept

        paymentGateway.setDecline(false);
        checkout(token, key, "CARD").andExpect(status().isCreated());   // the failed key was not "used up"
        assertThat(stockOf(productId)).isEqualTo(3);
    }

    @Test
    void sendingTheSameKeyTwiceGivesTheSameOrderAndOnlyOneSale() throws Exception {
        long productId = createProduct("100.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 2);
        String key = key();

        String first = checkout(token, key, "UPI").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = checkout(token, key, "UPI").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(second).get("id")).isEqualTo(objectMapper.readTree(first).get("id"));
        assertThat(objectMapper.readTree(second).get("orderNumber")).isEqualTo(objectMapper.readTree(first).get("orderNumber"));
        assertThat(countRows("orders")).isEqualTo(1);
        assertThat(countRows("payments")).isEqualTo(1);
        assertThat(stockOf(productId)).isEqualTo(3);           // taken once, not twice
    }

    @Test
    void aNewKeyAfterASuccessfulCheckoutFindsAnEmptyCart() throws Exception {
        long productId = createProduct("100.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 1);
        checkout(token, key(), "UPI").andExpect(status().isCreated());
        checkout(token, key(), "UPI").andExpect(status().isBadRequest());
        assertThat(countRows("orders")).isEqualTo(1);
    }

    @Test
    void anOldOrderKeepsItsNameAndPriceWhenTheProductChangesLater() throws Exception {
        long productId = createProduct("100.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId, 2);
        String body = checkout(token, key(), "UPI").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(body).get("id").asLong();
        String originalName = objectMapper.readTree(body).get("items").get(0).get("name").asText();

        jdbc.update("UPDATE products SET price = 999.00, name = 'TEST-renamed' WHERE id = ?", productId);

        mockMvc.perform(get("/api/orders/{id}", orderId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(100.00))
                .andExpect(jsonPath("$.items[0].lineTotal").value(200.00))
                .andExpect(jsonPath("$.items[0].name").value(originalName))
                .andExpect(jsonPath("$.totalAmount").value(200.00));
    }

    // ------------------------------------------------------------------ orders

    @Test
    void customersSeeOnlyTheirOwnOrders() throws Exception {
        long productId = createProduct("10.00", 10);
        String alice = registerCustomerToken(uniqueEmail());
        String bob = registerCustomerToken(uniqueEmail());
        addToCart(alice, productId, 2);
        String body = checkout(alice, key(), "UPI").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/orders/{id}", orderId).header("Authorization", bearer(alice)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/{id}", orderId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders/{id}", 999999).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders/my-orders").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/orders/my-orders").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(orderId))
                .andExpect(jsonPath("$.items[0].itemCount").value(2));
        mockMvc.perform(get("/api/orders/{id}", orderId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/orders/my-orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void orderHistoryIsNewestFirstAndPaged() throws Exception {
        long productId = createProduct("10.00", 10);
        String token = registerCustomerToken(uniqueEmail());
        for (int i = 0; i < 3; i++) {
            addToCart(token, productId, 1);
            checkout(token, key(), "COD").andExpect(status().isCreated());
        }
        mockMvc.perform(get("/api/orders/my-orders").param("size", "2").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        String list = mockMvc.perform(get("/api/orders/my-orders").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        long first = objectMapper.readTree(list).get("items").get(0).get("id").asLong();
        long last = objectMapper.readTree(list).get("items").get(2).get("id").asLong();
        assertThat(first).isGreaterThan(last);
        mockMvc.perform(get("/api/orders/my-orders").param("size", "1000").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- helpers

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private static Map<String, Object> shipping() {
        return Map.of("name", "Asha Rao", "phone", "9876543210", "address", "12 MG Road, Indiranagar",
                "city", "Bengaluru", "pincode", "560038");
    }

    private String body(String method) throws Exception {
        return objectMapper.writeValueAsString(Map.of("shipping", shipping(), "paymentMethod", method));
    }

    private void addToCart(String token, long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", quantity))))
                .andExpect(status().isCreated());
    }

    private ResultActions checkout(String token, String idempotencyKey, String method) throws Exception {
        return mockMvc.perform(post("/api/checkout").header("Authorization", bearer(token)).header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON).content(body(method)));
    }

    private ResultActions checkoutRaw(String token, String idempotencyKey, Map<String, Object> payload) throws Exception {
        return mockMvc.perform(post("/api/checkout").header("Authorization", bearer(token)).header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)));
    }
}
