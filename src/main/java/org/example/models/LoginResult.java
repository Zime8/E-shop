package org.example.models;

public record LoginResult(LoginStatus status, String role, Integer userId) {}