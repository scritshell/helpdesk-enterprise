package com.portfolio.helpdesk.statistics.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.helpdesk.statistics.api.StatisticsDtos;
import com.portfolio.helpdesk.ticket.domain.TicketStatus;
import com.portfolio.helpdesk.ticket.infrastructure.TicketHistoryRepository;
import com.portfolio.helpdesk.ticket.infrastructure.TicketRepository;

@Service
public class StatisticsService {
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;

    public StatisticsService(TicketRepository ticketRepository, TicketHistoryRepository historyRepository) {
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public StatisticsDtos.StatisticsResponse summary(int requestedDays) {
        int days = Math.max(1, Math.min(requestedDays, 365));
        long open = ticketRepository.countByStatus(TicketStatus.OPEN) + ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long pending = ticketRepository.countByStatus(TicketStatus.WAITING_USER);
        long resolved = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long closed = ticketRepository.countByStatus(TicketStatus.CLOSED);
        List<StatisticsDtos.Count> priorities = ticketRepository.countGroupedByPriority().stream().map(row -> new StatisticsDtos.Count(row.getLabel(), row.getValue())).toList();
        List<StatisticsDtos.Count> categories = ticketRepository.countGroupedByCategory().stream().map(row -> new StatisticsDtos.Count(row.getLabel(), row.getValue())).toList();
        Instant from = Instant.now().minus(days - 1L, ChronoUnit.DAYS);
        Map<String, Long> created = toMap(ticketRepository.countCreatedByDay(from));
        Map<String, Long> resolvedByDay = toMap(historyRepository.countStatusChangesByDay(from, "RESOLVED"));
        Map<String, Long> closedByDay = toMap(historyRepository.countStatusChangesByDay(from, "CLOSED"));
        List<StatisticsDtos.DailyCount> evolution = new ArrayList<>();
        java.time.LocalDate start = java.time.LocalDate.now().minusDays(days - 1L);
        for (int index = 0; index < days; index++) {
            String date = start.plusDays(index).toString();
            evolution.add(new StatisticsDtos.DailyCount(date, created.getOrDefault(date, 0L), resolvedByDay.getOrDefault(date, 0L), closedByDay.getOrDefault(date, 0L)));
        }
        return new StatisticsDtos.StatisticsResponse(open + pending + resolved + closed, open, pending, resolved, closed, priorities, categories, evolution, days);
    }

    private Map<String, Long> toMap(List<TicketRepository.CountRow> rows) {
        return rows.stream().collect(Collectors.toMap(TicketRepository.CountRow::getLabel, TicketRepository.CountRow::getValue, Long::sum));
    }
}
