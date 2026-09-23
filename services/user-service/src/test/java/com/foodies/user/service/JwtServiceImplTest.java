package com.foodies.user.service;

import com.foodies.user.service.impl.JwtServiceImpl;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceImplTest {

    private static final String SECRET = "cQlhYpQz9gd3XNOH8QliUk0f1+9X0CQsF45yH6IjLWw=";

    @Test
    void generateThenExtract_roundTripsSubject() {
        JwtService jwtService = new JwtServiceImpl(SECRET, 0);

        String token = jwtService.generateToken("alice@test.com");

        assertThat(jwtService.extractSubject(token)).isEqualTo("alice@test.com");
    }

    @Test
    void extractSubject_whenTokenTampered_throws() {
        JwtService jwtService = new JwtServiceImpl(SECRET, 0);
        String token = jwtService.generateToken("alice@test.com");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> jwtService.extractSubject(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void generateToken_whenNoExpirationConfigured_hasNoExpiryClaim() {
        JwtService jwtService = new JwtServiceImpl(SECRET, 0);
        String token = jwtService.generateToken("alice@test.com");

        assertThat(parseExpiration(token)).isNull();
    }

    @Test
    void generateToken_whenExpirationConfigured_hasExpiryClaim() {
        JwtService jwtService = new JwtServiceImpl(SECRET, 60_000);
        String token = jwtService.generateToken("alice@test.com");

        assertThat(parseExpiration(token)).isNotNull();
    }

    private Object parseExpiration(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }
}
