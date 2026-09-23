package com.foodies.user.service;

public interface JwtService {

    String generateToken(String subject);

    String extractSubject(String token);
}
