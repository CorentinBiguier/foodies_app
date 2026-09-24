package com.foodies.recipe.service.impl;

import com.foodies.recipe.modele.RecipeRequest;
import com.foodies.recipe.service.AdocParsingException;
import com.foodies.recipe.service.AdocRecipeParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AdocRecipeParserImpl implements AdocRecipeParser {

    private static final Pattern TITLE_PATTERN = Pattern.compile("^=\\s+(\\S.*)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\*\\s+(.+)$");
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\.\\s+(.+)$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("(\\d+)");

    private static final String SECTION_INGREDIENTS = "=== Ingrédients";
    private static final String SECTION_PREPARATION = "=== Préparation";
    private static final String SECTION_CUISSON = "=== Cuisson";
    private static final String SECTION_ACCOMPAGNEMENT = "=== Accompagnement";

    @Override
    public RecipeRequest parse(String adocContent) {
        List<String> lines = normalizeLines(adocContent);

        String title = extractTitle(lines);
        Integer preparationTime = extractInt(lines, "Temps de préparation");
        Integer cookingTime = extractInt(lines, "Temps de cuisson");
        Integer serving = extractInt(lines, "Portions");

        List<String> ingredients = extractSectionItems(lines, SECTION_INGREDIENTS, BULLET_PATTERN);
        List<String> preparationItems = extractSectionItems(lines, SECTION_PREPARATION, NUMBERED_PATTERN);
        List<String> cookingItems = extractSectionItems(lines, SECTION_CUISSON, NUMBERED_PATTERN);
        List<String> sideDishItems = extractSectionItems(lines, SECTION_ACCOMPAGNEMENT, BULLET_PATTERN);

        String preparationStep = String.join("\n", preparationItems);
        String cookingStep = String.join("\n", cookingItems);
        String sideDish = String.join(",", sideDishItems);

        return new RecipeRequest(title, ingredients, serving, preparationTime, preparationStep,
                cookingStep, cookingTime, sideDish, List.of());
    }

    private List<String> normalizeLines(String content) {
        return List.of(content.replace("\r\n", "\n").split("\n", -1));
    }

    private String extractTitle(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = TITLE_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                return matcher.group(1).trim();
            }
        }
        throw new AdocParsingException("Ligne de titre (\"= <titre>\") manquante");
    }

    private Integer extractInt(List<String> lines, String label) {
        String prefix = label + "::";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                String value = trimmed.substring(prefix.length()).trim();
                Matcher matcher = INTEGER_PATTERN.matcher(value);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
                throw new AdocParsingException(
                        "Impossible d'extraire un nombre entier depuis '%s:: %s'".formatted(label, value));
            }
        }
        throw new AdocParsingException("Ligne '%s::' manquante".formatted(label));
    }

    private List<String> extractSectionItems(List<String> lines, String sectionHeader, Pattern itemPattern) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals(sectionHeader)) {
                start = i;
                break;
            }
        }
        if (start == -1) {
            throw new AdocParsingException("Section '%s' manquante".formatted(sectionHeader));
        }

        List<String> items = new ArrayList<>();
        for (int i = start + 1; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("=== ")) {
                break;
            }
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher matcher = itemPattern.matcher(trimmed);
            if (matcher.matches()) {
                items.add(matcher.group(1).trim());
            }
        }

        if (items.isEmpty()) {
            throw new AdocParsingException(
                    "Section '%s' vide (aucun élément de liste trouvé)".formatted(sectionHeader));
        }
        return items;
    }
}
