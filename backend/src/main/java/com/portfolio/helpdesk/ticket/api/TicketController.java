package com.portfolio.helpdesk.ticket.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.helpdesk.ticket.api.TicketDtos.AddCommentRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.AssignTicketRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.ChangePriorityRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.ChangeStatusRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.CommentResponse;
import com.portfolio.helpdesk.ticket.api.TicketDtos.CreateTicketRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.HistoryResponse;
import com.portfolio.helpdesk.ticket.api.TicketDtos.TicketResponse;
import com.portfolio.helpdesk.ticket.application.TicketService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tickets")
@Validated
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request, Authentication authentication) {
        return ticketService.create(request, authentication);
    }

    @GetMapping
    public List<TicketResponse> list(Authentication authentication) {
        return ticketService.list(authentication);
    }

    @GetMapping("/queue")
    public List<TicketResponse> queue(Authentication authentication) {
        return ticketService.queue(authentication);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, Authentication authentication) {
        return ticketService.get(id, authentication);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request, Authentication authentication) {
        return ticketService.changeStatus(id, request, authentication);
    }

    @PatchMapping("/{id}/priority")
    public TicketResponse changePriority(@PathVariable Long id, @Valid @RequestBody ChangePriorityRequest request, Authentication authentication) {
        return ticketService.changePriority(id, request, authentication);
    }

    @PatchMapping("/{id}/assignment")
    public TicketResponse assign(@PathVariable Long id, @Valid @RequestBody AssignTicketRequest request, Authentication authentication) {
        return ticketService.assign(id, request, authentication);
    }

    @PostMapping("/{id}/claim")
    public TicketResponse claim(@PathVariable Long id, Authentication authentication) {
        return ticketService.claim(id, authentication);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable Long id, @Valid @RequestBody AddCommentRequest request, Authentication authentication) {
        return ticketService.addComment(id, request, authentication);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id, Authentication authentication) {
        return ticketService.comments(id, authentication);
    }

    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@PathVariable Long id, Authentication authentication) {
        return ticketService.history(id, authentication);
    }
}
