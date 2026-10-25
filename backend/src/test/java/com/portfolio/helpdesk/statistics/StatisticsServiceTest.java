package com.portfolio.helpdesk.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.portfolio.helpdesk.statistics.application.StatisticsService;
import com.portfolio.helpdesk.ticket.domain.TicketStatus;
import com.portfolio.helpdesk.ticket.infrastructure.TicketHistoryRepository;
import com.portfolio.helpdesk.ticket.infrastructure.TicketRepository;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {
    @Mock TicketRepository ticketRepository;
    @Mock TicketHistoryRepository historyRepository;

    @Test
    void aggregatesStatusCountsAndBoundsThePeriod() {
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.WAITING_USER)).thenReturn(3L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(4L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(5L);
        when(ticketRepository.countGroupedByPriority()).thenReturn(List.of());
        when(ticketRepository.countGroupedByCategory()).thenReturn(List.of());
        when(ticketRepository.countCreatedByDay(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(historyRepository.countStatusChangesByDay(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());

        var result = new StatisticsService(ticketRepository, historyRepository).summary(999);

        assertEquals(15L, result.total());
        assertEquals(3L, result.open());
        assertEquals(3L, result.pending());
        assertEquals(365, result.days());
        assertEquals(365, result.evolution().size());
    }
}
