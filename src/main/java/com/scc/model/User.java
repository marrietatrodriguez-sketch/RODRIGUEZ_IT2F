package com.scc.model;

public record User(
        int userId,
        String username,
        String email,
        String password,
        String role,
        String status
) {

    @Override
    public String toString() {
        return String.format(
                "[%d] %s (%s) - Role: %s - Status: %s",
                userId,
                username,
                email,
                role,
                status
        );
    }
}