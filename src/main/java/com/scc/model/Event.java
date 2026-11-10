package com.scc.model;

public record Event(
        int eventId,
        int userId,
        String name,
        String eventDate,
        String location,
        int guests
) {
}