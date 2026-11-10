package com.scc.model;

public record ActivityLog(
        int logId,
        Integer userId,
        String action,
        String description,
        String dateTime
) {
}
