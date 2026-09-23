package com.foodies.user.service.impl;

import com.foodies.user.entite.UserEntity;
import com.foodies.user.modele.AuthResponse;
import com.foodies.user.modele.LoginRequest;
import com.foodies.user.modele.RegisterRequest;
import com.foodies.user.repository.UserRepository;
import com.foodies.user.service.AuthService;
import com.foodies.user.service.EmailAlreadyUsedException;
import com.foodies.user.service.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Email ou mot de passe invalide";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException("Email %s déjà utilisé".formatted(request.email()));
        }
        String hash = passwordEncoder.encode(request.password());
        UserEntity saved = userRepository.save(new UserEntity(request.name(), request.email(), hash));
        return new AuthResponse(jwtService.generateToken(saved.getEmail()));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
        return new AuthResponse(jwtService.generateToken(user.getEmail()));
    }
}
