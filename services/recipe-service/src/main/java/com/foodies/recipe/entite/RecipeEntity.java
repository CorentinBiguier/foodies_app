package com.foodies.recipe.entite;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
public class RecipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ElementCollection
    @CollectionTable(name = "recipe_ingredients", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "ingredient", nullable = false)
    private List<String> ingredients = new ArrayList<>();

    private Integer serving;

    @Column(name = "preparation_time")
    private Integer preparationTime;

    @Column(name = "preparation_step", columnDefinition = "TEXT")
    private String preparationStep;

    @Column(name = "cooking_step", columnDefinition = "TEXT")
    private String cookingStep;

    @Column(name = "cooking_time")
    private Integer cookingTime;

    @Column(name = "side_dish")
    private String sideDish;

    @ElementCollection
    @CollectionTable(name = "recipe_tags", joinColumns = @JoinColumn(name = "recipe_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false)
    private List<Tag> tags = new ArrayList<>();

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    protected RecipeEntity() {
    }

    public RecipeEntity(String title, List<String> ingredients, Integer serving, Integer preparationTime,
                         String preparationStep, String cookingStep, Integer cookingTime, String sideDish,
                         List<Tag> tags, Long authorId, String authorName) {
        this.title = title;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.serving = serving;
        this.preparationTime = preparationTime;
        this.preparationStep = preparationStep;
        this.cookingStep = cookingStep;
        this.cookingTime = cookingTime;
        this.sideDish = sideDish;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.authorId = authorId;
        this.authorName = authorName;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public Integer getServing() {
        return serving;
    }

    public void setServing(Integer serving) {
        this.serving = serving;
    }

    public Integer getPreparationTime() {
        return preparationTime;
    }

    public void setPreparationTime(Integer preparationTime) {
        this.preparationTime = preparationTime;
    }

    public String getPreparationStep() {
        return preparationStep;
    }

    public void setPreparationStep(String preparationStep) {
        this.preparationStep = preparationStep;
    }

    public String getCookingStep() {
        return cookingStep;
    }

    public void setCookingStep(String cookingStep) {
        this.cookingStep = cookingStep;
    }

    public Integer getCookingTime() {
        return cookingTime;
    }

    public void setCookingTime(Integer cookingTime) {
        this.cookingTime = cookingTime;
    }

    public String getSideDish() {
        return sideDish;
    }

    public void setSideDish(String sideDish) {
        this.sideDish = sideDish;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
}
