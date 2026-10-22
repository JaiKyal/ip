package com.example.shoppingcart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runs the real Flyway migrations against a real PostgreSQL and checks that the
 * database itself rejects bad data. Each test runs in a transaction that is rolled back.
 */
class MigrationSchemaTest {

    private static final String CHECK_VIOLATION = "23514";
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private static EmbeddedPostgres postgres;
    private static DataSource dataSource;

    private Connection connection;

    @BeforeAll
    static void migrate() throws IOException {
        postgres = EmbeddedPostgres.builder().start();
        dataSource = postgres.getPostgresDatabase();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }

    @AfterAll
    static void shutDown() throws IOException {
        postgres.close();
    }

    @BeforeEach
    void open() throws SQLException {
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
    }

    @AfterEach
    void rollbackAndClose() throws SQLException {
        connection.rollback();
        connection.close();
    }

    // ------------------------------------------------------------ seed data

    @Test
    void seedCreatesThreeCategoriesAndFifteenProducts() throws SQLException {
        assertThat(count("SELECT count(*) FROM categories")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM products")).isEqualTo(15);
        assertThat(count("SELECT count(*) FROM products WHERE stock_quantity = 0")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM products WHERE stock_quantity BETWEEN 1 AND 5")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM users")).isZero();
    }

    @Test
    void everyProductHasACategory() throws SQLException {
        assertThat(count("SELECT count(*) FROM products WHERE category_id IS NULL")).isZero();
    }

    // ---------------------------------------------------------- constraints

    @Test
    void negativeStockIsRejected() {
        assertSqlState(CHECK_VIOLATION,
                () -> execute("UPDATE products SET stock_quantity = -1 WHERE id = (SELECT min(id) FROM products)"));
    }

    @Test
    void negativePriceIsRejected() {
        assertSqlState(CHECK_VIOLATION,
                () -> execute("UPDATE products SET price = -0.01 WHERE id = (SELECT min(id) FROM products)"));
    }

    @Test
    void emailIsUniqueIgnoringCase() throws SQLException {
        insertUser("Student@Example.com");
        assertSqlState(UNIQUE_VIOLATION, () -> insertUser("student@example.COM"));
    }

    @Test
    void unknownRoleIsRejected() {
        assertSqlState(CHECK_VIOLATION, () -> execute(
                "INSERT INTO users (full_name, email, password_hash, role) VALUES ('X', 'x@example.com', 'h', 'SUPERUSER')"));
    }

    @Test
    void sameProductCannotAppearTwiceInOneCart() throws SQLException {
        long userId = insertUser("cart@example.com");
        long cartId = insertReturningId("INSERT INTO carts (user_id) VALUES (" + userId + ")");
        long productId = firstProductId();
        execute("INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (" + cartId + ", " + productId + ", 1)");
        assertSqlState(UNIQUE_VIOLATION, () -> execute(
                "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (" + cartId + ", " + productId + ", 2)"));
    }

    @Test
    void cartQuantityMustBePositive() throws SQLException {
        long userId = insertUser("qty@example.com");
        long cartId = insertReturningId("INSERT INTO carts (user_id) VALUES (" + userId + ")");
        assertSqlState(CHECK_VIOLATION, () -> execute(
                "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (" + cartId + ", " + firstProductId() + ", 0)"));
    }

    @Test
    void unknownOrderStatusIsRejected() throws SQLException {
        long userId = insertUser("status@example.com");
        assertSqlState(CHECK_VIOLATION, () -> execute(
                "INSERT INTO orders (order_number, user_id, status, subtotal, total_amount, payment_method) "
                        + "VALUES ('ORD-BAD', " + userId + ", 'LOST_IN_SPACE', 10, 10, 'SIMULATED')"));
    }

    @Test
    void repeatedIdempotencyKeyForSameUserIsRejectedButMissingKeysAreFine() throws SQLException {
        long userId = insertUser("idem@example.com");
        insertOrder(userId, "ORD-1", "key-1");
        insertOrder(userId, "ORD-2", null);
        insertOrder(userId, "ORD-3", null);
        assertSqlState(UNIQUE_VIOLATION, () -> insertOrder(userId, "ORD-4", "key-1"));
    }

    @Test
    void orderLineTotalMustEqualUnitPriceTimesQuantity() throws SQLException {
        long userId = insertUser("line@example.com");
        long orderId = insertOrder(userId, "ORD-LINE", null);
        long productId = firstProductId();
        assertSqlState(CHECK_VIOLATION, () -> execute(
                "INSERT INTO order_items (order_id, product_id, product_name_snapshot, quantity, unit_price, line_total) "
                        + "VALUES (" + orderId + ", " + productId + ", 'Thing', 3, 100.00, 250.00)"));
    }

    @Test
    void soldProductCannotBeHardDeleted() throws SQLException {
        long userId = insertUser("fk@example.com");
        long orderId = insertOrder(userId, "ORD-FK", null);
        long productId = firstProductId();
        execute("INSERT INTO order_items (order_id, product_id, product_name_snapshot, quantity, unit_price, line_total) "
                + "VALUES (" + orderId + ", " + productId + ", 'Thing', 2, 100.00, 200.00)");
        assertSqlState(FOREIGN_KEY_VIOLATION, () -> execute("DELETE FROM products WHERE id = " + productId));
    }

    @Test
    void moneyKeepsExactDecimals() throws SQLException {
        long userId = insertUser("money@example.com");
        long orderId = insertOrder(userId, "ORD-MONEY", null);
        execute("INSERT INTO order_items (order_id, product_id, product_name_snapshot, quantity, unit_price, line_total) "
                + "VALUES (" + orderId + ", " + firstProductId() + ", 'Thing', 3, 0.10, 0.30)");
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT line_total FROM order_items WHERE order_id = " + orderId)) {
            rs.next();
            assertThat(rs.getBigDecimal(1)).isEqualByComparingTo("0.30");
        }
    }

    @Test
    void paymentReferenceIsUnique() throws SQLException {
        long userId = insertUser("pay@example.com");
        long orderId = insertOrder(userId, "ORD-PAY", null);
        String sql = "INSERT INTO payments (order_id, payment_method, payment_status, amount, transaction_reference) "
                + "VALUES (" + orderId + ", 'SIMULATED', 'PAID', 10, 'SIM-123')";
        execute(sql);
        assertSqlState(UNIQUE_VIOLATION, () -> execute(sql));
    }

    // -------------------------------------------------------------- helpers

    private long insertUser(String email) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users (full_name, email, password_hash, role) VALUES ('Test User', ?, 'hash', 'CUSTOMER') RETURNING id")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private long insertOrder(long userId, String orderNumber, String idempotencyKey) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO orders (order_number, user_id, subtotal, total_amount, payment_method, idempotency_key) "
                        + "VALUES (?, ?, 100, 100, 'SIMULATED', ?) RETURNING id")) {
            ps.setString(1, orderNumber);
            ps.setLong(2, userId);
            ps.setString(3, idempotencyKey);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private long firstProductId() throws SQLException {
        return count("SELECT min(id) FROM products");
    }

    private long insertReturningId(String insertSql) throws SQLException {
        return count(insertSql + " RETURNING id");
    }

    private long count(String sql) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        }
    }

    private static void assertSqlState(String expectedState, ThrowingRunnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo(expectedState);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws SQLException;
    }
}
