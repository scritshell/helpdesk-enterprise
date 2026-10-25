package com.portfolio.helpdesk.ticket.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.helpdesk.ticket.domain.TicketHistoryEntry;

public interface TicketHistoryRepository extends JpaRepository<TicketHistoryEntry, Long> {
    List<TicketHistoryEntry> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    @org.springframework.data.jpa.repository.Query(value = "select to_char(date_trunc('day', created_at), 'YYYY-MM-DD') as label, count(*) as value from ticket_history where created_at >= :from and action = 'STATUS_CHANGED' and new_value = :status group by date_trunc('day', created_at) order by label", nativeQuery = true)
    List<TicketRepository.CountRow> countStatusChangesByDay(java.time.Instant from, String status);
}
