package com.portfolio.helpdesk.user.api;

public final class UserDtos {
    private UserDtos() {}

    public record UserSummary(Long id, String email, String firstName, String lastName) {}
}
