package com.foodies.user.service;

import com.foodies.user.entite.UserEntity;
import com.foodies.user.modele.AuthResponse;
import com.foodies.user.modele.LoginRequest;
import com.foodies.user.modele.RegisterRequest;
import com.foodies.user.repository.UserRepository;
import com.foodies.user.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_hashesPasswordSavesUserAndReturnsToken() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(new UserEntity("Alice", "alice@test.com", "hashed"));
        when(jwtService.generateToken("alice@test.com")).thenReturn("token-123");

        AuthResponse response = authService.register(new RegisterRequest("Alice", "alice@test.com", "secret123"));

        assertThat(response.token()).isEqualTo("token-123");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void register_whenEmailAlreadyUsed_throwsAndNeverSaves() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Alice", "alice@test.com", "secret123")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        UserEntity user = new UserEntity("Alice", "alice@test.com", "hashed");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("alice@test.com")).thenReturn("token-123");

        AuthResponse response = authService.login(new LoginRequest("alice@test.com", "secret123"));

        assertThat(response.token()).isEqualTo("token-123");
    }

    @Test
    void login_withWrongPassword_throwsBadCredentials() {
        UserEntity user = new UserEntity("Alice", "alice@test.com", "hashed");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withUnknownEmail_throwsBadCredentials() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@test.com", "whatever")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
