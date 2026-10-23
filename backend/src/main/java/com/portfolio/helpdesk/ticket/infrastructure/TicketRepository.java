package com.portfolio.helpdesk.ticket.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.helpdesk.ticket.domain.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    List<Ticket> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);
    List<Ticket> findByTechnicianIsNullOrderByCreatedAtAsc();
}
