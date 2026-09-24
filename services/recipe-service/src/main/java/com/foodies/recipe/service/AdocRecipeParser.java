package com.foodies.recipe.service;

import com.foodies.recipe.modele.RecipeRequest;

public interface AdocRecipeParser {

    /**
     * Parses a fixed-format .adoc recipe document into a RecipeRequest.
     * Tags are always empty (the .adoc format has no notion of tags) — the
     * caller merges in any user-supplied tags afterward.
     */
    RecipeRequest parse(String adocContent);
}
