package com.foodies.recipe.service;

import com.foodies.recipe.entite.Tag;
import com.foodies.recipe.modele.RecipeDto;
import com.foodies.recipe.modele.RecipeRequest;

import java.util.List;

public interface RecipeService {

    List<RecipeDto> findAll();

    RecipeDto findById(Long id);

    RecipeDto create(RecipeRequest request, String authorizationHeader);

    RecipeDto update(Long id, RecipeRequest request, String authorizationHeader);

    RecipeDto createFromAdoc(String adocContent, List<Tag> tags, String authorizationHeader);

    RecipeDto addTag(Long id, Tag tag, String authorizationHeader);

    RecipeDto removeTag(Long id, Tag tag, String authorizationHeader);
}
