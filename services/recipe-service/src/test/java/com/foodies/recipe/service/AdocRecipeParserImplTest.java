package com.foodies.recipe.service;

import com.foodies.recipe.modele.RecipeRequest;
import com.foodies.recipe.service.impl.AdocRecipeParserImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdocRecipeParserImplTest {

    private static final String VALID_ADOC = """
            =  Boulettes de bœuf sauce tomate
            :toc:
            :toclevels: 2

            == Boulettes de bœuf sauce tomate

            Temps de préparation:: 20 min
            Temps de cuisson:: 20 min
            Portions:: 4

            === Ingrédients

            * 500 g de bœuf haché
            * 1 œuf
            * 2 c. à soupe de chapelure
            * 1 oignon
            * 1 boîte de tomates concassées
            * 1 c. à soupe d'huile d'olive
            * Sel
            * Poivre
            * Origan

            === Préparation

            . Mélanger le bœuf, l'œuf, la chapelure, le sel et le poivre.
            . Former des boulettes.
            . Émincer l'oignon.

            === Cuisson

            . Faire revenir l'oignon dans une poêle.
            . Ajouter les boulettes et les faire dorer.
            . Verser les tomates concassées.
            . Ajouter l'origan.
            . Laisser mijoter 15 minutes.

            === Accompagnement

            * Riz
            * Pâtes
            * Semoule
            """;

    private final AdocRecipeParser parser = new AdocRecipeParserImpl();

    @Test
    void parse_validAdoc_mapsAllFields() {
        RecipeRequest result = parser.parse(VALID_ADOC);

        assertThat(result.title()).isEqualTo("Boulettes de bœuf sauce tomate");
        assertThat(result.ingredients()).containsExactly(
                "500 g de bœuf haché",
                "1 œuf",
                "2 c. à soupe de chapelure",
                "1 oignon",
                "1 boîte de tomates concassées",
                "1 c. à soupe d'huile d'olive",
                "Sel",
                "Poivre",
                "Origan");
        assertThat(result.serving()).isEqualTo(4);
        assertThat(result.preparationTime()).isEqualTo(20);
        assertThat(result.cookingTime()).isEqualTo(20);
        assertThat(result.preparationStep())
                .contains("Mélanger le bœuf")
                .contains("Former des boulettes")
                .contains("Émincer l'oignon");
        assertThat(result.cookingStep())
                .contains("Faire revenir l'oignon")
                .contains("Laisser mijoter 15 minutes");
        assertThat(result.sideDish()).isEqualTo("Riz,Pâtes,Semoule");
        assertThat(result.tags()).isEmpty();
    }

    @Test
    void parse_missingTitle_throwsAdocParsingException() {
        String withoutTitle = VALID_ADOC.replaceFirst("(?m)^=\\s+Boulettes.*$\n", "");

        assertThatThrownBy(() -> parser.parse(withoutTitle))
                .isInstanceOf(AdocParsingException.class)
                .hasMessage("Ligne de titre (\"= <titre>\") manquante");
    }

    @Test
    void parse_missingCuissonSection_throwsAdocParsingException() {
        String withoutCuisson = VALID_ADOC.substring(0, VALID_ADOC.indexOf("=== Cuisson"))
                + VALID_ADOC.substring(VALID_ADOC.indexOf("=== Accompagnement"));

        assertThatThrownBy(() -> parser.parse(withoutCuisson))
                .isInstanceOf(AdocParsingException.class)
                .hasMessage("Section '=== Cuisson' manquante");
    }

    @Test
    void parse_unparseablePreparationTime_throwsAdocParsingException() {
        String withBadTime = VALID_ADOC.replace("Temps de préparation:: 20 min", "Temps de préparation:: vingt min");

        assertThatThrownBy(() -> parser.parse(withBadTime))
                .isInstanceOf(AdocParsingException.class)
                .hasMessage("Impossible d'extraire un nombre entier depuis 'Temps de préparation:: vingt min'");
    }
}
