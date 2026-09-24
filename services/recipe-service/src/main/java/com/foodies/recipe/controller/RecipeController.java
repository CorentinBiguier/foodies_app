package com.foodies.recipe.controller;

import com.foodies.recipe.client.UserAuthenticationException;
import com.foodies.recipe.entite.Tag;
import com.foodies.recipe.modele.RecipeDto;
import com.foodies.recipe.modele.RecipeRequest;
import com.foodies.recipe.service.AdocParsingException;
import com.foodies.recipe.service.RecipeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<RecipeDto> findAll() {
        return recipeService.findAll();
    }

    @GetMapping("/{id}")
    public RecipeDto findById(@PathVariable Long id) {
        return recipeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDto create(@RequestBody RecipeRequest request,
                             @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        requireAuthorizationHeader(authorizationHeader);
        return recipeService.create(request, authorizationHeader);
    }

    @PutMapping("/{id}")
    public RecipeDto update(@PathVariable Long id,
                             @RequestBody RecipeRequest request,
                             @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        requireAuthorizationHeader(authorizationHeader);
        return recipeService.update(id, request, authorizationHeader);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDto importFromAdoc(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "tags", required = false) List<Tag> tags,
                                     @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        requireAuthorizationHeader(authorizationHeader);
        return recipeService.createFromAdoc(readFileContent(file), tags, authorizationHeader);
    }

    private String readFileContent(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AdocParsingException("Fichier illisible");
        }
    }

    private void requireAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UserAuthenticationException("En-tête Authorization manquant");
        }
    }
}
