package com.certimate.manager.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtProvider {

    // 사용 중에는 로그인이 풀리지 않도록, 발급 후 하루가 지난 토큰은 요청 때마다 새로 발급한다(슬라이딩 세션).
    // 그래서 실제로는 '마지막 접속 후 expirationMs 동안' 로그인이 유지된다.
    private static final long RENEW_AFTER_MS = Duration.ofDays(1).toMillis();

    private final Key key;
    private final long expirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    // 1. 토큰 생성
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 3. 토큰에서 이메일 꺼내기
    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 4. 발급된 지 하루가 지났으면 갱신 대상
    public boolean shouldRenew(String token) {
        Date issuedAt = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getIssuedAt();
        return issuedAt == null || System.currentTimeMillis() - issuedAt.getTime() > RENEW_AFTER_MS;
    }

    // 5. 토큰 쿠키. 쿠키 유지 기간을 토큰 만료와 같게 맞춘다(따로 두면 한쪽이 먼저 끝나 로그인이 풀린다).
    public ResponseCookie buildCookie(String token) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(false) // ponytail: Cloudflare가 HTTPS를 처리하지만 오리진은 HTTP라 false. 오리진 HTTPS 전환 시 true로
                .path("/")
                .maxAge(Duration.ofMillis(expirationMs))
                .sameSite("Lax")
                .build();
    }
}
