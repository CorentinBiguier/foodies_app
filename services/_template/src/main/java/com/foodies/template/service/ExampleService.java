package com.foodies.template.service;

import com.foodies.template.modele.CreateExampleRequest;
import com.foodies.template.modele.ExampleDto;

import java.util.List;

public interface ExampleService {

    List<ExampleDto> findAll();

    ExampleDto findById(Long id);

    ExampleDto create(CreateExampleRequest request);
}
