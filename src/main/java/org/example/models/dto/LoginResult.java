package org.example.models.dto;

public record LoginResult(LoginStatus status, String role, Integer userId) {}