package com.example.bankcards.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private final String issuer;
    private final String audience;
    private final Key signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.issuer = issuer;
        this.audience = audience;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(ensureBase64(secret)));
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Генерирует подписанный JWT-токен.
     *
     * @param subject субъект токена (обычно логин/email)
     * @param claims дополнительные произвольные клеймы
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .addClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Валидирует подпись/срок действия токена.
     *
     * @param token JWT
     * @param expectedSubject ожидаемый субъект (например, логин)
     * @return true если токен корректен, false в противном случае
     */
    public boolean isTokenValid(String token, String expectedSubject) {
        String subject = getClaim(token, Claims::getSubject);
        Date exp = getClaim(token, Claims::getExpiration);
        return expectedSubject.equals(subject) && exp.after(new Date());
    }

    public <T> T getClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }

    /**
     * Кодировка в Base64.
     */
    private static String ensureBase64(String secret) {
        try {
            Decoders.BASE64.decode(secret);
            return secret;
        } catch (Exception ignored) {
            return java.util.Base64.getEncoder().encodeToString(secret.getBytes());
        }
    }
}
