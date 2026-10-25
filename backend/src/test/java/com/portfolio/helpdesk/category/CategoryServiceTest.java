package com.portfolio.helpdesk.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.portfolio.helpdesk.category.api.CategoryDtos;
import com.portfolio.helpdesk.category.domain.Category;
import com.portfolio.helpdesk.category.infrastructure.CategoryRepository;
import com.portfolio.helpdesk.category.application.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository categoryRepository;
    private CategoryService service;

    @BeforeEach
    void setUp() { service = new CategoryService(categoryRepository); }

    @Test
    void rejectsDuplicateNamesIgnoringCase() {
        when(categoryRepository.findByNameIgnoreCase("Correo")).thenReturn(Optional.of(new Category("Correo", "")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.create(new CategoryDtos.CreateCategoryRequest(" Correo ", "Correo")));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void updatesAndDeactivatesExistingCategoryWithoutDeletingIt() {
        Category category = new Category("Vieja", "Descripción");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Nueva", 1L)).thenReturn(false);

        service.update(1L, new CategoryDtos.UpdateCategoryRequest("Nueva", "Actualizada"));
        service.changeStatus(1L, new CategoryDtos.ChangeCategoryStatusRequest(false));

        assertEquals("Nueva", category.getName());
        assertEquals(false, category.isActive());
    }
}
