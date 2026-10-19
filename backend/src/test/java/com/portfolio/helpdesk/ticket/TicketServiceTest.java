package com.portfolio.helpdesk.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
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

    private UserAccount user(String email, RoleName roleName) {
        UserAccount user = new UserAccount(email, "hash", "Test", "User");
        user.addRole(new Role(roleName));
        return user;
    }
}
