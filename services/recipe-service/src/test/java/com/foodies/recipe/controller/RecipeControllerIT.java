package com.foodies.recipe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodies.recipe.TestcontainersConfig;
import com.foodies.recipe.client.UserDto;
import com.foodies.recipe.client.UserServiceClient;
import com.foodies.recipe.entite.Tag;
import com.foodies.recipe.modele.RecipeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class RecipeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceClient userServiceClient;

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

            === Préparation

            . Mélanger le bœuf, l'œuf, la chapelure, le sel et le poivre.

            === Cuisson

            . Faire revenir l'oignon dans une poêle.

            === Accompagnement

            * Riz
            * Pâtes
            * Semoule
            """;

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
    void findAll_withoutAuthorizationHeader_returnsPublicly() throws Exception {
        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk());

        verifyNoInteractions(userServiceClient);
    }

    @Test
    void findById_whenMissing_returns404() throws Exception {
        mockMvc.perform(get("/api/recipes/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void create_withoutAuthorizationHeader_returns401AndNeverCallsUserService() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userServiceClient);
    }

    @Test
    void create_withValidAuthorization_persistsRecipeWithResolvedAuthor() throws Exception {
        when(userServiceClient.getCurrentUser(anyString())).thenReturn(new UserDto(1L, "Alice", "alice@test.com"));

        mockMvc.perform(post("/api/recipes")
                        .header("Authorization", "Bearer token-alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(1))
                .andExpect(jsonPath("$.authorName").value("Alice"));
    }

    @Test
    void update_byNonAuthor_returns403() throws Exception {
        when(userServiceClient.getCurrentUser(anyString())).thenReturn(new UserDto(1L, "Alice", "alice@test.com"));

        String created = mockMvc.perform(post("/api/recipes")
                        .header("Authorization", "Bearer token-alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        when(userServiceClient.getCurrentUser(anyString())).thenReturn(new UserDto(2L, "Bob", "bob@test.com"));

        mockMvc.perform(put("/api/recipes/{id}", id)
                        .header("Authorization", "Bearer token-bob")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void update_byAuthor_returns200() throws Exception {
        when(userServiceClient.getCurrentUser(anyString())).thenReturn(new UserDto(1L, "Alice", "alice@test.com"));

        String created = mockMvc.perform(post("/api/recipes")
                        .header("Authorization", "Bearer token-alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        RecipeRequest updated = new RecipeRequest(
                "Curry de légumes (v2)",
                List.of("Riz", "Curry"),
                4, 15, "prep", "cook", 20, "Riz", List.of(Tag.VEGETARIEN));

        mockMvc.perform(put("/api/recipes/{id}", id)
                        .header("Authorization", "Bearer token-alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Curry de légumes (v2)"));
    }

    @Test
    void importFromAdoc_withValidFileAndAuthorization_returns201WithMappedFields() throws Exception {
        when(userServiceClient.getCurrentUser(anyString())).thenReturn(new UserDto(1L, "Alice", "alice@test.com"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "recipe.adoc", "text/plain", VALID_ADOC.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/recipes/import")
                        .file(file)
                        .param("tags", "VEGETARIEN")
                        .header("Authorization", "Bearer token-alice"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Boulettes de bœuf sauce tomate"))
                .andExpect(jsonPath("$.sideDish").value("Riz,Pâtes,Semoule"))
                .andExpect(jsonPath("$.tags[0]").value("VEGETARIEN"))
                .andExpect(jsonPath("$.authorId").value(1));
    }

    @Test
    void importFromAdoc_withMalformedFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "broken.adoc", "text/plain", "not a valid adoc recipe".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/recipes/import")
                        .file(file)
                        .header("Authorization", "Bearer token-alice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void importFromAdoc_withoutAuthorizationHeader_returns401AndNeverCallsUserService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "recipe.adoc", "text/plain", VALID_ADOC.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/recipes/import").file(file))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userServiceClient);
    }
}
