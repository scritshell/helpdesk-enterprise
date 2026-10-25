package com.portfolio.helpdesk.category.application;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.portfolio.helpdesk.category.api.CategoryDtos;
import com.portfolio.helpdesk.category.domain.Category;
import com.portfolio.helpdesk.category.infrastructure.CategoryRepository;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> active() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> all() {
        return categoryRepository.findAll(org.springframework.data.domain.Sort.by("name")).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CreateCategoryRequest request) {
        String name = request.name().trim();
        ensureUnique(name, null);
        return toResponse(categoryRepository.save(new Category(name, normalizeDescription(request.description()))));
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.UpdateCategoryRequest request) {
        Category category = find(id);
        String name = request.name().trim();
        ensureUnique(name, id);
        category.update(name, normalizeDescription(request.description()));
        return toResponse(category);
    }

    @Transactional
    public CategoryDtos.CategoryResponse changeStatus(Long id, CategoryDtos.ChangeCategoryStatusRequest request) {
        Category category = find(id);
        category.setActive(request.active());
        return toResponse(category);
    }

    private void ensureUnique(String name, Long id) {
        boolean duplicate = id == null ? categoryRepository.findByNameIgnoreCase(name).isPresent()
            : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicate) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private CategoryDtos.CategoryResponse toResponse(Category category) {
        return new CategoryDtos.CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.isActive());
    }
}
