package com.foodies.user.service;

import com.foodies.user.entite.UserEntity;
import com.foodies.user.modele.CreateUserRequest;
import com.foodies.user.modele.UserDto;
import com.foodies.user.repository.UserRepository;
import com.foodies.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    @Test
    void findAll_returnsAllUsersAsDto() {
        when(userRepository.findAll()).thenReturn(List.of(new UserEntity("Alice")));

        List<UserDto> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Alice");
    }

    @Test
    void findById_whenNotFound_throwsNoSuchElementException() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(42L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_savesEntityAndReturnsDto() {
        when(userRepository.save(any(UserEntity.class))).thenReturn(new UserEntity("Bob"));

        UserDto result = userService.create(new CreateUserRequest("Bob"));

        assertThat(result.name()).isEqualTo("Bob");
        verify(userRepository).save(any(UserEntity.class));
    }
}
