package com.foodies.recipe.service.impl;

import com.foodies.recipe.client.UserDto;
import com.foodies.recipe.client.UserServiceClient;
import com.foodies.recipe.entite.RecipeEntity;
import com.foodies.recipe.modele.RecipeDto;
import com.foodies.recipe.modele.RecipeRequest;
import com.foodies.recipe.repository.RecipeRepository;
import com.foodies.recipe.service.ForbiddenException;
import com.foodies.recipe.service.RecipeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserServiceClient userServiceClient;

    public RecipeServiceImpl(RecipeRepository recipeRepository, UserServiceClient userServiceClient) {
        this.recipeRepository = recipeRepository;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public List<RecipeDto> findAll() {
        return recipeRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public RecipeDto findById(Long id) {
        return recipeRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Recette %d introuvable".formatted(id)));
    }

    @Override
    public RecipeDto create(RecipeRequest request, String authorizationHeader) {
        UserDto author = userServiceClient.getCurrentUser(authorizationHeader);

        RecipeEntity entity = new RecipeEntity(
                request.title(),
                request.ingredients(),
                request.serving(),
                request.preparationTime(),
                request.preparationStep(),
                request.cookingStep(),
                request.cookingTime(),
                request.sideDish(),
                request.tags(),
                author.id(),
                author.name());

        return toDto(recipeRepository.save(entity));
    }

    @Override
    public RecipeDto update(Long id, RecipeRequest request, String authorizationHeader) {
        UserDto caller = userServiceClient.getCurrentUser(authorizationHeader);

        RecipeEntity entity = recipeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recette %d introuvable".formatted(id)));

        if (!entity.getAuthorId().equals(caller.id())) {
            throw new ForbiddenException("Seul l'auteur de la recette peut la modifier");
        }

        entity.setTitle(request.title());
        entity.setIngredients(request.ingredients());
        entity.setServing(request.serving());
        entity.setPreparationTime(request.preparationTime());
        entity.setPreparationStep(request.preparationStep());
        entity.setCookingStep(request.cookingStep());
        entity.setCookingTime(request.cookingTime());
        entity.setSideDish(request.sideDish());
        entity.setTags(request.tags());
        entity.setAuthorName(caller.name());

        return toDto(recipeRepository.save(entity));
    }

    private RecipeDto toDto(RecipeEntity entity) {
        return new RecipeDto(
                entity.getId(),
                entity.getTitle(),
                entity.getIngredients(),
                entity.getServing(),
                entity.getPreparationTime(),
                entity.getPreparationStep(),
                entity.getCookingStep(),
                entity.getCookingTime(),
                entity.getSideDish(),
                entity.getTags(),
                entity.getAuthorId(),
                entity.getAuthorName());
    }
}
