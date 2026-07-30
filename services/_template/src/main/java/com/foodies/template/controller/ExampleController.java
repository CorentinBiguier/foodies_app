package com.foodies.template.controller;

import com.foodies.template.modele.CreateExampleRequest;
import com.foodies.template.modele.ExampleDto;
import com.foodies.template.service.ExampleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/examples")
public class ExampleController {

    private final ExampleService exampleService;

    public ExampleController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    @GetMapping
    public List<ExampleDto> findAll() {
        return exampleService.findAll();
    }

    @GetMapping("/{id}")
    public ExampleDto findById(@PathVariable Long id) {
        return exampleService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExampleDto create(@RequestBody CreateExampleRequest request) {
        return exampleService.create(request);
    }
}
