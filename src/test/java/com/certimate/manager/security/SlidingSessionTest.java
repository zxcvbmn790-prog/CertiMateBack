package com.certimate.manager.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

// 사용 중에는 로그인이 풀리지 않아야 한다: 쿠키 유지 기간 = 토큰 만료(7일), 발급 후 하루가 지난 토큰은 요청 시 재발급.
class SlidingSessionTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256!!";
    private static final long SEVEN_DAYS = Duration.ofDays(7).toMillis();
    private final JwtProvider provider = new JwtProvider(SECRET, SEVEN_DAYS);

    private String tokenIssued(Duration ago) {
        long issued = System.currentTimeMillis() - ago.toMillis();
        return Jwts.builder().setSubject("u@test.com").setIssuedAt(new Date(issued))
                .setExpiration(new Date(issued + SEVEN_DAYS))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256).compact();
    }

    @Test
    void cookieLivesAsLongAsToken() {
        assertThat(provider.buildCookie("t").getMaxAge()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void freshTokenIsNotRenewedButDayOldTokenIs() throws Exception {
        assertThat(provider.shouldRenew(provider.generateToken("u@test.com"))).isFalse();
        assertThat(provider.shouldRenew(tokenIssued(Duration.ofDays(2)))).isTrue();
    }

    @Test
    void filterReissuesCookieForDayOldToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/user/dashboard");
        req.setCookies(new Cookie("accessToken", tokenIssued(Duration.ofDays(2))));
        MockHttpServletResponse res = new MockHttpServletResponse();

        new JwtAuthenticationFilter(provider).doFilter(req, res, new MockFilterChain());

        String setCookie = res.getHeader("Set-Cookie");
        assertThat(setCookie).startsWith("accessToken=").contains("Max-Age=604800").contains("HttpOnly");
        String newToken = setCookie.substring("accessToken=".length(), setCookie.indexOf(';'));
        assertThat(provider.shouldRenew(newToken)).isFalse();
        assertThat(provider.getEmailFromToken(newToken)).isEqualTo("u@test.com");
    }

    @Test
    void filterDoesNotTouchCookieForFreshToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/user/dashboard");
        req.setCookies(new Cookie("accessToken", provider.generateToken("u@test.com")));
        MockHttpServletResponse res = new MockHttpServletResponse();

        new JwtAuthenticationFilter(provider).doFilter(req, res, new MockFilterChain());

        assertThat(res.getHeader("Set-Cookie")).isNull();
    }
}
