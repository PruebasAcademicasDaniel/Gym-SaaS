package com.gymflow.dashboard.infrastructure.web;

import com.gymflow.dashboard.application.DashboardSummary;
import java.math.BigDecimal;

public record DashboardResponse(long activeMembers, long membershipsExpiringSoon, BigDecimal revenueThisMonth) {

    public static DashboardResponse from(DashboardSummary summary) {
        return new DashboardResponse(summary.activeMembers(), summary.membershipsExpiringSoon(), summary.revenueThisMonth());
    }
}
