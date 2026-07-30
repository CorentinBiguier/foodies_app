package com.foodies.user.service;

import com.foodies.user.modele.CreateUserRequest;
import com.foodies.user.modele.UserDto;

import java.util.List;

public interface UserService {

    List<UserDto> findAll();

    UserDto findById(Long id);

    UserDto create(CreateUserRequest request);
}
