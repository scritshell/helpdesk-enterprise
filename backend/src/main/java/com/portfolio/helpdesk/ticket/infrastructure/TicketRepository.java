package com.portfolio.helpdesk.ticket.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.helpdesk.ticket.domain.Ticket;
import com.portfolio.helpdesk.ticket.domain.TicketPriority;
import com.portfolio.helpdesk.ticket.domain.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    List<Ticket> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);
    List<Ticket> findByTechnicianIsNullOrderByCreatedAtAsc();

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);

    interface CountRow {
        String getLabel();
        long getValue();
    }

    @org.springframework.data.jpa.repository.Query(value = "select priority as label, count(*) as value from tickets group by priority order by priority", nativeQuery = true)
    List<CountRow> countGroupedByPriority();

    @org.springframework.data.jpa.repository.Query(value = "select c.name as label, count(t.id) as value from categories c left join tickets t on t.category_id = c.id group by c.id, c.name order by c.name", nativeQuery = true)
    List<CountRow> countGroupedByCategory();

    @org.springframework.data.jpa.repository.Query(value = "select to_char(date_trunc('day', created_at), 'YYYY-MM-DD') as label, count(*) as value from tickets where created_at >= :from group by date_trunc('day', created_at) order by label", nativeQuery = true)
    List<CountRow> countCreatedByDay(java.time.Instant from);
}
