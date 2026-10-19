package com.portfolio.helpdesk.ticket.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.helpdesk.ticket.domain.TicketHistoryEntry;

public interface TicketHistoryRepository extends JpaRepository<TicketHistoryEntry, Long> {
    List<TicketHistoryEntry> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
