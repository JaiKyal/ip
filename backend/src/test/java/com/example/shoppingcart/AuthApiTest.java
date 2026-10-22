package com.example.shoppingcart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shoppingcart.config.AdminSeeder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Registration, login, token checks and role-based access, all through the real HTTP layer and database. */
class AuthApiTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private AdminSeeder adminSeeder;

    // ------------------------------------------------------------ register

    @Test
    void registerCreatesACustomerAndReturnsAToken() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(postJson("/api/auth/register", Map.of("fullName", "  Asha Rao ", "email", email.toUpperCase(), "password", CUSTOMER_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(7200))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.fullName").value("Asha Rao"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void passwordIsStoredAsABCryptHashNeverAsText() {
        String email = uniqueEmail();
        try {
            registerCustomerToken(email);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        String stored = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
        assertThat(stored).startsWith("$2").hasSize(60).doesNotContain(CUSTOMER_PASSWORD);
    }

    @Test
    void nobodyCanRegisterThemselvesAsAdmin() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(postJson("/api/auth/register",
                        Map.of("fullName", "Sneaky", "email", email, "password", CUSTOMER_PASSWORD, "role", "ADMIN", "enabled", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE email = ?", String.class, email)).isEqualTo("CUSTOMER");
    }

    @Test
    void registeringTheSameEmailAgainIsA409EvenWithDifferentCase() throws Exception {
        String email = uniqueEmail();
        registerCustomerToken(email);
        mockMvc.perform(postJson("/api/auth/register", Map.of("fullName", "Again", "email", email.toUpperCase(), "password", CUSTOMER_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void registrationInputIsValidatedAndRejectedValuesAreNotEchoed() throws Exception {
        mockMvc.perform(postJson("/api/auth/register", Map.of("fullName", "", "email", "not-an-email", "password", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fullName")))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("password")))
                .andExpect(jsonPath("$.message", not(containsString("not-an-email"))));
        mockMvc.perform(postJson("/api/auth/register", Map.of("fullName", "Ok", "email", uniqueEmail(), "password", "x".repeat(73))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingOrBrokenJsonBodyIsA400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    // --------------------------------------------------------------- login

    @Test
    void loginWithCorrectPasswordWorksAndIsCaseInsensitiveOnEmail() throws Exception {
        String email = uniqueEmail();
        registerCustomerToken(email);
        mockMvc.perform(postJson("/api/auth/login", Map.of("email", email.toUpperCase(), "password", CUSTOMER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        String email = uniqueEmail();
        registerCustomerToken(email);
        mockMvc.perform(postJson("/api/auth/login", Map.of("email", email, "password", "WrongPassword-1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        mockMvc.perform(postJson("/api/auth/login", Map.of("email", uniqueEmail(), "password", "WrongPassword-1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void sqlInjectionInTheLoginFormIsJustAWrongLogin() throws Exception {
        mockMvc.perform(postJson("/api/auth/login", Map.of("email", "' OR '1'='1' --", "password", "' OR '1'='1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAccountsCannotLogIn() throws Exception {
        String email = uniqueEmail();
        registerCustomerToken(email);
        jdbc.update("UPDATE users SET enabled = FALSE WHERE email = ?", email);
        mockMvc.perform(postJson("/api/auth/login", Map.of("email", email, "password", CUSTOMER_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------- /me and tokens

    @Test
    void meReturnsTheLoggedInUserFromTheToken() throws Exception {
        String email = uniqueEmail();
        String token = registerCustomerToken(email);
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void tokenCarriesUserIdEmailRoleAndIssuer() throws Exception {
        String email = uniqueEmail();
        Jwt jwt = jwtDecoder.decode(registerCustomerToken(email));
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(id));
        assertThat(jwt.getClaimAsString("email")).isEqualTo(email);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CUSTOMER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("shoppingcart");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now().plusSeconds(7000));
    }

    @Test
    void noTokenIsA401WithJsonAndWwwAuthenticate() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void garbageExpiredForgedAndUnsignedTokensAreAllRejected() throws Exception {
        String valid = registerCustomerToken(uniqueEmail());

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer("not.a.token"))).andExpect(status().isUnauthorized());

        String expired = encode(jwtEncoder, "1", "CUSTOMER", Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));

        JwtEncoder foreign = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec("a-completely-different-secret-of-32-chars!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        String signedWithWrongKey = encode(foreign, "1", "ADMIN", Instant.now(), Instant.now().plusSeconds(3600));
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(signedWithWrongKey))).andExpect(status().isUnauthorized());

        String unsigned = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + b64("{\"sub\":\"1\",\"role\":\"ADMIN\",\"iss\":\"shoppingcart\"}") + ".";
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(unsigned))).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(valid))).andExpect(status().isOk());
    }

    @Test
    void aCustomerCannotEditTheirOwnTokenToBecomeAdmin() throws Exception {
        String token = registerCustomerToken(uniqueEmail());
        String[] parts = token.split("\\.");
        String adminPayload = b64("{\"sub\":\"1\",\"role\":\"ADMIN\",\"iss\":\"shoppingcart\",\"exp\":" + (Instant.now().getEpochSecond() + 3600) + "}");
        String tampered = parts[0] + "." + adminPayload + "." + parts[2];
        mockMvc.perform(get("/api/admin/test-probe").header("Authorization", bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------- roles and admin area

    @Test
    void adminAreaIs401ForVisitors403ForCustomersAnd200ForAdmins() throws Exception {
        mockMvc.perform(get("/api/admin/test-probe")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/test-probe").header("Authorization", bearer(registerCustomerToken(uniqueEmail()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to do this"));

        mockMvc.perform(get("/api/admin/test-probe").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.area").value("admin"));
    }

    @Test
    void theSeededAdminExistsOnceAndSeedingTwiceDoesNotDuplicateIt() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value(TEST_ADMIN_EMAIL));

        adminSeeder.run(null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE role = 'ADMIN'", Long.class)).isEqualTo(1L);
        String stored = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, TEST_ADMIN_EMAIL);
        assertThat(stored).startsWith("$2").doesNotContain(TEST_ADMIN_PASSWORD);
    }

    // ------------------------------------------------- everything else is still sensible

    @Test
    void publicEndpointsNeedNoLoginButAnythingElseDoes() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/does-not-exist")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/products/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void loggedInUsersGetNormalJson404And405() throws Exception {
        String token = registerCustomerToken(uniqueEmail());
        mockMvc.perform(get("/api/does-not-exist").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(delete("/api/products/1").header("Authorization", bearer(token)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    // -------------------------------------------------------------- helpers

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postJson(String url, Map<String, ?> body) throws Exception {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    private static String encode(JwtEncoder encoder, String subject, String role, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("shoppingcart").subject(subject).claim("role", role).issuedAt(issuedAt).expiresAt(expiresAt).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
