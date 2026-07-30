package com.foodies.template.service.impl;

import com.foodies.template.entite.ExampleEntity;
import com.foodies.template.modele.CreateExampleRequest;
import com.foodies.template.modele.ExampleDto;
import com.foodies.template.repository.ExampleRepository;
import com.foodies.template.service.ExampleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ExampleServiceImpl implements ExampleService {

    private final ExampleRepository exampleRepository;

    public ExampleServiceImpl(ExampleRepository exampleRepository) {
        this.exampleRepository = exampleRepository;
    }

    @Override
    public List<ExampleDto> findAll() {
        return exampleRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ExampleDto findById(Long id) {
        return exampleRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Example %d introuvable".formatted(id)));
    }

    @Override
    public ExampleDto create(CreateExampleRequest request) {
        ExampleEntity saved = exampleRepository.save(new ExampleEntity(request.name()));
        return toDto(saved);
    }

    private ExampleDto toDto(ExampleEntity entity) {
        return new ExampleDto(entity.getId(), entity.getName());
    }
}
