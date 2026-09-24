package com.foodies.recipe.modele;

import com.foodies.recipe.entite.Tag;

import java.util.List;

public record RecipeRequest(
        String title,
        List<String> ingredients,
        Integer serving,
        Integer preparationTime,
        String preparationStep,
        String cookingStep,
        Integer cookingTime,
        String sideDish,
        List<Tag> tags
) {
}
