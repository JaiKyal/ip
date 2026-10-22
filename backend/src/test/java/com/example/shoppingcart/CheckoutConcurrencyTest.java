package com.example.shoppingcart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * The viva centerpiece: many customers pressing "Place order" at the very same moment, with real threads and a
 * real database. The row locks must make sure stock is never oversold.
 */
class CheckoutConcurrencyTest extends AbstractPostgresIntegrationTest {

    @Test
    void twoCustomersRaceForTheLastUnitAndExactlyOneWins() throws Exception {
        long productId = createProduct("100.00", 1);
        String alice = registerCustomerToken(uniqueEmail());
        String bob = registerCustomerToken(uniqueEmail());
        addToCart(alice, productId);
        addToCart(bob, productId);

        List<Integer> statuses = runTogether(
                () -> checkoutStatus(alice, UUID.randomUUID().toString()),
                () -> checkoutStatus(bob, UUID.randomUUID().toString()));

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(stockOf(productId)).isZero();                       // never negative, never oversold
        assertThat(countRows("orders")).isEqualTo(1);
        assertThat(countRows("order_items")).isEqualTo(1);
        assertThat(countRows("payments")).isEqualTo(1);
        // The customer who lost still has the item in their cart and was not charged.
        assertThat(countRows("cart_items")).isEqualTo(1);
    }

    @Test
    void fiveCustomersRaceForThreeUnitsAndExactlyThreeWin() throws Exception {
        long productId = createProduct("50.00", 3);
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String token = registerCustomerToken(uniqueEmail());
            addToCart(token, productId);
            tokens.add(token);
        }

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (String token : tokens) {
            tasks.add(() -> checkoutStatus(token, UUID.randomUUID().toString()));
        }
        List<Integer> statuses = runTogether(tasks.toArray(new Callable[0]));

        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(3);
        assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(2);
        assertThat(stockOf(productId)).isZero();
        assertThat(countRows("orders")).isEqualTo(3);
    }

    @Test
    void aDoubleClickWithTheSameKeyCreatesOneOrderAndSellsOnce() throws Exception {
        long productId = createProduct("100.00", 5);
        String token = registerCustomerToken(uniqueEmail());
        addToCart(token, productId);
        String key = UUID.randomUUID().toString();

        List<Integer> statuses = runTogether(
                () -> checkoutStatus(token, key),
                () -> checkoutStatus(token, key),
                () -> checkoutStatus(token, key));

        assertThat(statuses).containsExactlyInAnyOrder(201, 200, 200);   // one created, two replays
        assertThat(countRows("orders")).isEqualTo(1);
        assertThat(countRows("payments")).isEqualTo(1);
        assertThat(stockOf(productId)).isEqualTo(4);                    // sold exactly once
    }

    @Test
    void customersBuyingOverlappingProductsInOppositeOrderDoNotDeadlock() throws Exception {
        long a = createProduct("10.00", 10);
        long b = createProduct("10.00", 10);
        String alice = registerCustomerToken(uniqueEmail());
        String bob = registerCustomerToken(uniqueEmail());
        addToCart(alice, a);
        addToCart(alice, b);
        addToCart(bob, b);     // bob adds in the opposite order
        addToCart(bob, a);

        List<Integer> statuses = runTogether(
                () -> checkoutStatus(alice, UUID.randomUUID().toString()),
                () -> checkoutStatus(bob, UUID.randomUUID().toString()));

        assertThat(statuses).containsExactly(201, 201);
        assertThat(stockOf(a)).isEqualTo(8);
        assertThat(stockOf(b)).isEqualTo(8);
    }

    // -------------------------------------------------------------- helpers

    @SafeVarargs
    private static List<Integer> runTogether(Callable<Integer>... tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.length);
        CountDownLatch ready = new CountDownLatch(tasks.length);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();                 // every thread waits here, then they all start together
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private void addToCart(String token, long productId) throws Exception {
        mockMvc.perform(post("/api/cart/items").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isCreated());
    }

    private int checkoutStatus(String token, String idempotencyKey) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "shipping", Map.of("name", "Asha Rao", "phone", "9876543210", "address", "12 MG Road, Indiranagar",
                        "city", "Bengaluru", "pincode", "560038"),
                "paymentMethod", "UPI"));
        return mockMvc.perform(post("/api/checkout").header("Authorization", bearer(token)).header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getStatus();
    }
}
