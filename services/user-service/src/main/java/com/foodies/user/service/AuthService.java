package com.foodies.user.service;

import com.foodies.user.modele.AuthResponse;
import com.foodies.user.modele.LoginRequest;
import com.foodies.user.modele.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
