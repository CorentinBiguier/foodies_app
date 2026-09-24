package com.foodies.recipe.service;

import com.foodies.recipe.client.UserDto;
import com.foodies.recipe.client.UserServiceClient;
import com.foodies.recipe.entite.RecipeEntity;
import com.foodies.recipe.entite.Tag;
import com.foodies.recipe.modele.RecipeDto;
import com.foodies.recipe.modele.RecipeRequest;
import com.foodies.recipe.repository.RecipeRepository;
import com.foodies.recipe.service.impl.RecipeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    private static final String AUTH_HEADER = "Bearer some-token";

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private AdocRecipeParser adocRecipeParser;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeServiceImpl(recipeRepository, userServiceClient, adocRecipeParser);
    }

    private RecipeRequest sampleRequest() {
        return new RecipeRequest(
                "Curry de légumes",
                List.of("Riz", "Curry", "Lait de coco"),
                4,
                15,
                "Couper les légumes",
                "Mijoter 20 minutes",
                20,
                "Riz basmati",
                List.of(Tag.VEGETARIEN));
    }

    @Test
    void findAll_returnsAllRecipesAsDto() {
        RecipeEntity entity = new RecipeEntity("Curry", List.of("Riz"), 2, 10, "prep", "cook", 15,
                "Riz", List.of(Tag.VEGETARIEN), 1L, "Alice");
        when(recipeRepository.findAll()).thenReturn(List.of(entity));

        List<RecipeDto> result = recipeService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Curry");
    }

    @Test
    void findById_whenNotFound_throwsNoSuchElementException() {
        when(recipeRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.findById(42L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_resolvesAuthorAndSavesEntity() {
        when(userServiceClient.getCurrentUser(AUTH_HEADER)).thenReturn(new UserDto(1L, "Alice", "alice@test.com"));
        when(recipeRepository.save(any(RecipeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeDto result = recipeService.create(sampleRequest(), AUTH_HEADER);

        assertThat(result.authorId()).isEqualTo(1L);
        assertThat(result.authorName()).isEqualTo("Alice");
        assertThat(result.title()).isEqualTo("Curry de légumes");
        verify(recipeRepository).save(any(RecipeEntity.class));
    }

    @Test
    void update_byAuthor_appliesChanges() {
        RecipeEntity existing = new RecipeEntity("Ancien titre", List.of("x"), 1, 5, "p", "c", 5,
                "s", List.of(Tag.AIR_FRYER), 1L, "Alice");
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userServiceClient.getCurrentUser(AUTH_HEADER)).thenReturn(new UserDto(1L, "Alice", "alice@test.com"));
        when(recipeRepository.save(any(RecipeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeDto result = recipeService.update(10L, sampleRequest(), AUTH_HEADER);

        assertThat(result.title()).isEqualTo("Curry de légumes");
        verify(recipeRepository).save(existing);
    }

    @Test
    void update_byNonAuthor_throwsForbiddenAndNeverSaves() {
        RecipeEntity existing = new RecipeEntity("Titre", List.of("x"), 1, 5, "p", "c", 5,
                "s", List.of(Tag.AIR_FRYER), 1L, "Alice");
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userServiceClient.getCurrentUser(AUTH_HEADER)).thenReturn(new UserDto(2L, "Bob", "bob@test.com"));

        assertThatThrownBy(() -> recipeService.update(10L, sampleRequest(), AUTH_HEADER))
                .isInstanceOf(ForbiddenException.class);

        verify(recipeRepository, never()).save(any(RecipeEntity.class));
    }

    @Test
    void createFromAdoc_mergesCallerSuppliedTagsAndDelegatesToCreate() {
        RecipeRequest parsed = new RecipeRequest(
                "Boulettes", List.of("Bœuf haché"), 4, 20, "prep", "cook", 20, "Riz", List.of());
        when(adocRecipeParser.parse("adoc-content")).thenReturn(parsed);
        when(userServiceClient.getCurrentUser(AUTH_HEADER)).thenReturn(new UserDto(1L, "Alice", "alice@test.com"));
        when(recipeRepository.save(any(RecipeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeDto result = recipeService.createFromAdoc("adoc-content", List.of(Tag.AIR_FRYER), AUTH_HEADER);

        assertThat(result.tags()).containsExactly(Tag.AIR_FRYER);
        assertThat(result.title()).isEqualTo("Boulettes");

        ArgumentCaptor<RecipeEntity> captor = ArgumentCaptor.forClass(RecipeEntity.class);
        verify(recipeRepository).save(captor.capture());
        assertThat(captor.getValue().getTags()).containsExactly(Tag.AIR_FRYER);
    }
}
