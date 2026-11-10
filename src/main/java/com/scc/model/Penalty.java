package com.scc.model;

public record Penalty(
        int penaltyId,
        int borrowingId,
        String type,
        String description,
        double amount,
        String status
) {
}