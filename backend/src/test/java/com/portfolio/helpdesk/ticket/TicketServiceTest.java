package com.portfolio.helpdesk.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;

import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.portfolio.helpdesk.auth.domain.Role;
import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;
import com.portfolio.helpdesk.category.domain.Category;
import com.portfolio.helpdesk.category.infrastructure.CategoryRepository;
import com.portfolio.helpdesk.ticket.api.TicketDtos.AddCommentRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.ChangeStatusRequest;
import com.portfolio.helpdesk.ticket.application.TicketService;
import com.portfolio.helpdesk.ticket.domain.Ticket;
import com.portfolio.helpdesk.ticket.domain.TicketStatus;
import com.portfolio.helpdesk.ticket.infrastructure.TicketCommentRepository;
import com.portfolio.helpdesk.ticket.infrastructure.TicketHistoryRepository;
import com.portfolio.helpdesk.ticket.infrastructure.TicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock TicketRepository ticketRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserAccountRepository userRepository;
    @Mock TicketCommentRepository commentRepository;
    @Mock TicketHistoryRepository historyRepository;
    @Mock Authentication authentication;

    private TicketService service;
    private UserAccount reporter;
    private UserAccount otherUser;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketRepository, categoryRepository, userRepository, commentRepository, historyRepository);
        reporter = user("reporter@test.local", RoleName.USER);
        otherUser = user("other@test.local", RoleName.USER);
        ticket = new Ticket("Problema", "Descripción", new Category("Software", "Aplicaciones"), reporter);
        lenient().when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
    }

    @Test
    void ownerCanAddCommentAndItCreatesAuditEntry() {
        when(authentication.getName()).thenReturn("reporter@test.local");
        when(userRepository.findByEmailIgnoreCase("reporter@test.local")).thenReturn(Optional.of(reporter));
        when(commentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.addComment(1L, new AddCommentRequest("Ya puedo reproducir el error"), authentication);

        verify(commentRepository).save(any());
        verify(historyRepository).save(any());
    }

    @Test
    void unrelatedUserCannotChangeStatus() {
        when(authentication.getName()).thenReturn("other@test.local");
        when(userRepository.findByEmailIgnoreCase("other@test.local")).thenReturn(Optional.of(otherUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.changeStatus(1L, new ChangeStatusRequest(TicketStatus.IN_PROGRESS), authentication));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void ownerCannotSkipTheTechnicianWorkflow() {
        when(authentication.getName()).thenReturn("reporter@test.local");
        when(userRepository.findByEmailIgnoreCase("reporter@test.local")).thenReturn(Optional.of(reporter));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.changeStatus(1L, new ChangeStatusRequest(TicketStatus.RESOLVED), authentication));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void technicianCanClaimAnUnassignedTicket() {
        UserAccount technician = user("technician@test.local", RoleName.TECHNICIAN);
        when(authentication.getName()).thenReturn("technician@test.local");
        when(userRepository.findByEmailIgnoreCase("technician@test.local")).thenReturn(Optional.of(technician));

        service.claim(1L, authentication);

        assertEquals(technician, ticket.getTechnician());
        verify(historyRepository).save(any());
    }

    @Test
    void technicianCannotAssignAnotherTechnician() {
        UserAccount technician = user("technician@test.local", RoleName.TECHNICIAN);
        when(authentication.getName()).thenReturn("technician@test.local");
        when(userRepository.findByEmailIgnoreCase("technician@test.local")).thenReturn(Optional.of(technician));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.assign(1L, new com.portfolio.helpdesk.ticket.api.TicketDtos.AssignTicketRequest(7L), authentication));

        assertEquals(403, exception.getStatusCode().value());
        verify(userRepository, never()).findById(7L);
    }

    @Test
    void userCannotReadTheTechnicianQueue() {
        when(authentication.getName()).thenReturn("reporter@test.local");
        when(userRepository.findByEmailIgnoreCase("reporter@test.local")).thenReturn(Optional.of(reporter));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.queue(authentication));

        assertEquals(403, exception.getStatusCode().value());
        verify(ticketRepository, never()).findByTechnicianIsNullOrderByCreatedAtAsc();
    }

    @Test
    void technicianCanReadOnlyUnassignedTicketsFromQueue() {
        UserAccount technician = user("technician@test.local", RoleName.TECHNICIAN);
        when(authentication.getName()).thenReturn("technician@test.local");
        when(userRepository.findByEmailIgnoreCase("technician@test.local")).thenReturn(Optional.of(technician));
        when(ticketRepository.findByTechnicianIsNullOrderByCreatedAtAsc()).thenReturn(List.of(ticket));

        assertEquals(1, service.queue(authentication).size());
        verify(ticketRepository).findByTechnicianIsNullOrderByCreatedAtAsc();
    }

    @Test
    void assignedTicketCannotBeClaimedTwice() {
        UserAccount technician = user("technician@test.local", RoleName.TECHNICIAN);
        UserAccount otherTechnician = user("other-tech@test.local", RoleName.TECHNICIAN);
        ticket.assignTo(otherTechnician);
        when(authentication.getName()).thenReturn("technician@test.local");
        when(userRepository.findByEmailIgnoreCase("technician@test.local")).thenReturn(Optional.of(technician));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.claim(1L, authentication));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void assignedTechnicianCanCloseAResolvedTicket() {
        UserAccount technician = user("technician@test.local", RoleName.TECHNICIAN);
        ticket.assignTo(technician);
        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        ticket.changeStatus(TicketStatus.RESOLVED);
        when(authentication.getName()).thenReturn("technician@test.local");
        when(userRepository.findByEmailIgnoreCase("technician@test.local")).thenReturn(Optional.of(technician));

        service.changeStatus(1L, new ChangeStatusRequest(TicketStatus.CLOSED), authentication);

        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
        verify(historyRepository).save(any());
    }

    private UserAccount user(String email, RoleName roleName) {
        UserAccount user = new UserAccount(email, "hash", "Test", "User");
        user.addRole(new Role(roleName));
        return user;
    }
}
