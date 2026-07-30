package com.foodies.template.service;

import com.foodies.template.entite.ExampleEntity;
import com.foodies.template.modele.CreateExampleRequest;
import com.foodies.template.modele.ExampleDto;
import com.foodies.template.repository.ExampleRepository;
import com.foodies.template.service.impl.ExampleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExampleServiceImplTest {

    @Mock
    private ExampleRepository exampleRepository;

    private ExampleService exampleService;

    @BeforeEach
    void setUp() {
        exampleService = new ExampleServiceImpl(exampleRepository);
    }

    @Test
    void findAll_returnsAllExamplesAsDto() {
        when(exampleRepository.findAll()).thenReturn(List.of(new ExampleEntity("Tomate")));

        List<ExampleDto> result = exampleService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Tomate");
    }

    @Test
    void findById_whenNotFound_throwsNoSuchElementException() {
        when(exampleRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exampleService.findById(42L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_savesEntityAndReturnsDto() {
        when(exampleRepository.save(any(ExampleEntity.class))).thenReturn(new ExampleEntity("Basilic"));

        ExampleDto result = exampleService.create(new CreateExampleRequest("Basilic"));

        assertThat(result.name()).isEqualTo("Basilic");
        verify(exampleRepository).save(any(ExampleEntity.class));
    }
}
