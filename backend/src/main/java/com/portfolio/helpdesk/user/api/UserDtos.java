package com.portfolio.helpdesk.user.api;

import java.time.Instant;
import java.util.Set;

import com.portfolio.helpdesk.auth.domain.RoleName;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class UserDtos {
    private UserDtos() {}

    public record UserSummary(Long id, String email, String firstName, String lastName) {}

    public record UserResponse(Long id, String email, String firstName, String lastName,
        Set<RoleName> roles, boolean enabled, Instant createdAt) {}

    public record UpdateUserRequest(@NotEmpty Set<RoleName> roles, @NotNull Boolean enabled) {}
}
