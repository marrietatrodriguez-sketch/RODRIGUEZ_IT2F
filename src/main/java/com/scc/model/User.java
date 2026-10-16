package com.scc.model;

public record User(int id, String username, String email, String password, String roles, String status) {
    @Override
    public String toString(){
        return String.format("[%d] %s (%s) - Role: %s - Status: %s", id, username, email, roles, status);
    }
}