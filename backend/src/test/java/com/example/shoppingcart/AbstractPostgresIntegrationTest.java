package com.example.shoppingcart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shoppingcart.support.ControllablePaymentGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Base for integration tests: boots the whole application against a real PostgreSQL started inside
 * the test JVM. Flyway runs the real migrations, Hibernate validates the entities against them, and the
 * first admin is seeded from the test credentials below. One database is shared by every test class in the run.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractPostgresIntegrationTest {

    protected static final String TEST_ADMIN_EMAIL = "admin@test.example";
    protected static final String TEST_ADMIN_PASSWORD = "AdminPass-12345";
    protected static final String TEST_JWT_SECRET = "test-only-secret-that-is-at-least-32-characters-long";
    protected static final String CUSTOMER_PASSWORD = "CustomerPass-123";

    private static final EmbeddedPostgres POSTGRES = start();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper objectMapper;

    private static EmbeddedPostgres start() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException ignored) {
                    // JVM is exiting
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
        }
    }

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.admin.email", () -> TEST_ADMIN_EMAIL);
        registry.add("app.admin.password", () -> TEST_ADMIN_PASSWORD);
    }

    @Autowired
    protected ControllablePaymentGateway paymentGateway;

    /**
     * Removes everything tests created (orders, carts, customers, "TEST-" products) so tests cannot affect each
     * other. The seeded catalog and the seeded admin stay.
     */
    @AfterEach
    void cleanUpTestData() {
        paymentGateway.setDecline(false);
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM cart_items");
        jdbc.update("DELETE FROM carts");
        jdbc.update("DELETE FROM products WHERE name LIKE 'TEST-%'");
        jdbc.update("DELETE FROM users WHERE role = 'CUSTOMER'");
    }

    /** Inserts an active product named TEST-... in the first category and returns its id. */
    protected long createProduct(String price, int stock) {
        return jdbc.queryForObject(
                "INSERT INTO products (name, description, price, stock_quantity, image_url, category_id) "
                        + "VALUES (?, 'test product', ?::numeric, ?, 'https://placehold.co/600x400', (SELECT min(id) FROM categories)) RETURNING id",
                Long.class, "TEST-" + UUID.randomUUID().toString().substring(0, 8), price, stock);
    }

    protected int stockOf(long productId) {
        return jdbc.queryForObject("SELECT stock_quantity FROM products WHERE id = ?", Integer.class, productId);
    }

    protected long countRows(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.example";
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Registers a brand-new customer through the real API and returns their login token. */
    protected String registerCustomerToken(String email) throws Exception {
        return tokenFrom(post("/api/auth/register"), Map.of("fullName", "Test Customer", "email", email, "password", CUSTOMER_PASSWORD));
    }

    protected String adminToken() throws Exception {
        return tokenFrom(post("/api/auth/login"), Map.of("email", TEST_ADMIN_EMAIL, "password", TEST_ADMIN_PASSWORD));
    }

    private String tokenFrom(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, Map<String, String> body)
            throws Exception {
        MvcResult result = mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
