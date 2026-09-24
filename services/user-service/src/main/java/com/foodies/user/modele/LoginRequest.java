package com.foodies.user.modele;

public record LoginRequest(
        String email,
        String password) {
}
