package com.portfolio.helpdesk.category.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.portfolio.helpdesk.category.api.CategoryDtos.CategoryResponse;
import com.portfolio.helpdesk.category.api.CategoryDtos.CreateCategoryRequest;
import com.portfolio.helpdesk.category.application.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
@Validated
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.active();
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CategoryResponse> manage() {
        return categoryService.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryDtos.UpdateCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse changeStatus(@PathVariable Long id,
        @Valid @RequestBody CategoryDtos.ChangeCategoryStatusRequest request) {
        return categoryService.changeStatus(id, request);
    }
}
