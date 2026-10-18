package com.portfolio.helpdesk.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.category.domain.Category;
import com.portfolio.helpdesk.ticket.domain.Ticket;
import com.portfolio.helpdesk.ticket.domain.TicketPriority;
import com.portfolio.helpdesk.ticket.domain.TicketStatus;

class TicketDomainTest {
    @Test
    void newTicketStartsOpenWithMediumPriority() {
        Ticket ticket = new Ticket(
            "No funciona el correo",
            "El usuario recibe un error al iniciar sesión",
            new Category("Correo", "Correo corporativo"),
            new UserAccount("user@test.local", "hash", "Test", "User")
        );

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertEquals(TicketPriority.MEDIUM, ticket.getPriority());
    }

    @Test
    void ticketCanChangeStatusAndPriority() {
        Ticket ticket = new Ticket(
            "Problema",
            "Descripción",
            new Category("Software", "Aplicaciones"),
            new UserAccount("user@test.local", "hash", "Test", "User")
        );

        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        ticket.changePriority(TicketPriority.HIGH);

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(TicketPriority.HIGH, ticket.getPriority());
    }
}
