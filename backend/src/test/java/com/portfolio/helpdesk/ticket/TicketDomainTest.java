package com.portfolio.helpdesk.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.category.domain.Category;
import com.portfolio.helpdesk.ticket.domain.Ticket;
import com.portfolio.helpdesk.ticket.domain.TicketPriority;
import com.portfolio.helpdesk.ticket.domain.TicketStatus;

class TicketDomainTest {
    @Test
    void newTicketStartsOpenWithMediumPriority() {
        Ticket ticket = ticket();

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertEquals(TicketPriority.MEDIUM, ticket.getPriority());
    }

    @Test
    void ticketCanChangeStatusAndPriority() {
        Ticket ticket = ticket();

        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        ticket.changePriority(TicketPriority.HIGH);

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(TicketPriority.HIGH, ticket.getPriority());
    }

    @Test
    void invalidStatusTransitionIsRejected() {
        Ticket ticket = ticket();

        assertThrows(IllegalStateException.class, () -> ticket.changeStatus(TicketStatus.CLOSED));
    }

    @Test
    void resolvedTicketCanBeReopenedForAdditionalWork() {
        Ticket ticket = ticket();
        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        ticket.changeStatus(TicketStatus.RESOLVED);
        ticket.changeStatus(TicketStatus.IN_PROGRESS);

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
    }

    private Ticket ticket() {
        return new Ticket(
            "No funciona el correo",
            "El usuario recibe un error al iniciar sesión",
            new Category("Correo", "Correo corporativo"),
            new UserAccount("user@test.local", "hash", "Test", "User")
        );
    }
}
