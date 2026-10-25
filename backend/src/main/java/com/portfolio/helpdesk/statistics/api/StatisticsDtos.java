package com.portfolio.helpdesk.statistics.api;

import java.util.List;

public final class StatisticsDtos {
    private StatisticsDtos() {}

    public record Count(String label, long value) {}
    public record DailyCount(String date, long created, long resolved, long closed) {}
    public record StatisticsResponse(long total, long open, long pending, long resolved, long closed,
        List<Count> priorities, List<Count> categories, List<DailyCount> evolution, int days) {}
}
