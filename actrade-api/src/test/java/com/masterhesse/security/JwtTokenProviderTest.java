package com.masterhesse.security;

import com.masterhesse.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-must-be-at-least-32-characters-long");
        jwtProperties.setAccessTokenExpirationMs(3600000); // 1 hour
        jwtProperties.setRefreshTokenExpirationMs(604800000); // 7 days

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    private UserDetails createTestUser(String username, String... roles) {
        var authorities = Stream.of(roles)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return new User(username, "password", authorities);
    }

    @Nested
    @DisplayName("Token 生成测试")
    class TokenGenerationTests {

        @Test
        @DisplayName("应成功生成 Access Token")
        void shouldGenerateAccessToken() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String token = jwtTokenProvider.generateAccessToken(authentication);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
        }

        @Test
        @DisplayName("应成功生成 Refresh Token")
        void shouldGenerateRefreshToken() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String token = jwtTokenProvider.generateRefreshToken(authentication);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("应成功生成 Refresh Token（通过用户名）")
        void shouldGenerateRefreshTokenByUsername() {
            String token = jwtTokenProvider.generateRefreshToken("testuser");

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("不同用户应生成不同的 Token")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            UserDetails user1 = createTestUser("user1", "ROLE_USER");
            UserDetails user2 = createTestUser("user2", "ROLE_USER");

            Authentication auth1 = new UsernamePasswordAuthenticationToken(user1, null, user1.getAuthorities());
            Authentication auth2 = new UsernamePasswordAuthenticationToken(user2, null, user2.getAuthorities());

            String token1 = jwtTokenProvider.generateAccessToken(auth1);
            String token2 = jwtTokenProvider.generateAccessToken(auth2);

            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("Token 解析测试")
    class TokenParsingTests {

        @Test
        @DisplayName("应从 Token 中正确提取用户名")
        void shouldExtractUsernameFromToken() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String token = jwtTokenProvider.generateAccessToken(authentication);
            String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

            assertEquals("testuser", extractedUsername);
        }

        @Test
        @DisplayName("应从 Token 中正确提取角色")
        void shouldExtractRolesFromToken() {
            UserDetails user = createTestUser("testuser", "ROLE_USER", "ROLE_ADMIN");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String token = jwtTokenProvider.generateAccessToken(authentication);
            String roles = jwtTokenProvider.getRolesFromToken(token);

            assertTrue(roles.contains("ROLE_USER"));
            assertTrue(roles.contains("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("应正确获取 Token 类型")
        void shouldGetTokenType() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            assertEquals("access", jwtTokenProvider.getTokenType(accessToken));
            assertEquals("refresh", jwtTokenProvider.getTokenType(refreshToken));
        }

        @Test
        @DisplayName("应正确解析 Claims")
        void shouldParseClaimsCorrectly() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String token = jwtTokenProvider.generateAccessToken(authentication);
            Claims claims = jwtTokenProvider.parseToken(token);

            assertEquals("testuser", claims.getSubject());
            assertNotNull(claims.getId());
            assertNotNull(claims.getIssuedAt());
            assertNotNull(claims.getExpiration());
        }
    }

    @Nested
    @DisplayName("Token 验证测试")
    class TokenValidationTests {

        @Test
        @DisplayName("应验证有效的 Token")
        void shouldValidateValidToken() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String token = jwtTokenProvider.generateAccessToken(authentication);

            assertTrue(jwtTokenProvider.validateToken(token));
        }

        @Test
        @DisplayName("应拒绝无效的 Token")
        void shouldRejectInvalidToken() {
            String invalidToken = "invalid.token.here";

            assertFalse(jwtTokenProvider.validateToken(invalidToken));
        }

        @Test
        @DisplayName("应拒绝伪造的 Token")
        void shouldRejectForgedToken() {
            String forgedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciJ9.wrong_signature";

            assertFalse(jwtTokenProvider.validateToken(forgedToken));
        }

        @Test
        @DisplayName("应正确判断 Access Token")
        void shouldIdentifyAccessToken() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            assertTrue(jwtTokenProvider.isAccessToken(accessToken));
            assertFalse(jwtTokenProvider.isAccessToken(refreshToken));
        }

        @Test
        @DisplayName("应正确判断 Refresh Token")
        void shouldIdentifyRefreshToken() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            assertFalse(jwtTokenProvider.isRefreshToken(accessToken));
            assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
        }
    }

    @Nested
    @DisplayName("Token 过期测试")
    class TokenExpirationTests {

        @Test
        @DisplayName("新生成的 Token 不应过期")
        void shouldNotBeExpiredWhenNewlyGenerated() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String token = jwtTokenProvider.generateAccessToken(authentication);

            assertFalse(jwtTokenProvider.isTokenExpired(token));
        }

        @Test
        @DisplayName("应拒绝空 Token")
        void shouldRejectEmptyToken() {
            assertFalse(jwtTokenProvider.validateToken(""));
            assertFalse(jwtTokenProvider.validateToken(null));
        }
    }

    @Nested
    @DisplayName("Token 类型检查测试")
    class TokenTypeCheckTests {

        @Test
        @DisplayName("Access Token 验证应通过")
        void accessTokenShouldBeValid() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);

            assertTrue(jwtTokenProvider.validateToken(accessToken));
            assertTrue(jwtTokenProvider.isAccessToken(accessToken));
            assertFalse(jwtTokenProvider.isRefreshToken(accessToken));
        }

        @Test
        @DisplayName("Refresh Token 验证应通过")
        void refreshTokenShouldBeValid() {
            UserDetails user = createTestUser("testuser", "ROLE_USER");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            assertTrue(jwtTokenProvider.validateToken(refreshToken));
            assertFalse(jwtTokenProvider.isAccessToken(refreshToken));
            assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
        }
    }
}
