package com.scc.model;

public record BorrowingTransaction(
        int borrowingId,
        int eventId,
        int userId,
        int equipmentId,
        int quantity,
        String requestDate,
        String dueDate,
        String returnDate,
        String requestStatus,
        String returnStatus
) {
}