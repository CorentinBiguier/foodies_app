package com.foodies.user.modele;

public record ErrorResponse(
        String error,
        String details) {
}
