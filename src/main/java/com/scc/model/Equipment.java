package com.scc.model;

public record Equipment(
        int equipmentId,
        String name,
        String category,
        int quantity,
        int availableQuantity,
        String condition,
        double replacementCost
) {
}