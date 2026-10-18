package com.portfolio.helpdesk.category.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {
    private CategoryDtos() {}

    public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
    ) {}

    public record CategoryResponse(
        Long id,
        String name,
        String description,
        boolean active
    ) {}
}
