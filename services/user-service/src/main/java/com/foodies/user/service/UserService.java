package com.foodies.user.service;

import com.foodies.user.modele.UserDto;

import java.util.List;

public interface UserService {

    List<UserDto> findAll();

    UserDto findById(Long id);

    UserDto findByEmail(String email);
}
