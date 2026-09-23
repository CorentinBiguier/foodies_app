package com.foodies.user.service.impl;

import com.foodies.user.service.JwtService;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtServiceImpl(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(String subject) {
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .signWith(key);
        if (expirationMs > 0) {
            builder.expiration(new Date(System.currentTimeMillis() + expirationMs));
        }
        return builder.compact();
    }

    @Override
    public String extractSubject(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
