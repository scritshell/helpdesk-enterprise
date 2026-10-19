package com.portfolio.helpdesk.ticket.api;

import java.time.Instant;
import java.util.List;

import com.portfolio.helpdesk.ticket.domain.TicketPriority;
import com.portfolio.helpdesk.ticket.domain.TicketStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TicketDtos {
    private TicketDtos() {}

    public record CreateTicketRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank String description,
        @NotNull Long categoryId,
        TicketPriority priority
    ) {}

    public record ChangeStatusRequest(@NotNull TicketStatus status) {}
    public record ChangePriorityRequest(@NotNull TicketPriority priority) {}
    public record AssignTicketRequest(@NotNull Long technicianId) {}
    public record AddCommentRequest(@NotBlank @Size(max = 5000) String body) {}

    public record UserSummary(Long id, String email, String firstName, String lastName) {}

    public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        CategorySummary category,
        UserSummary reporter,
        UserSummary technician,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record CategorySummary(Long id, String name) {}
    public record CommentResponse(Long id, String body, UserSummary author, Instant createdAt) {}
    public record HistoryResponse(Long id, String action, String oldValue, String newValue, UserSummary actor, Instant createdAt) {}
    public record TicketListResponse(List<TicketResponse> tickets) {}
}
