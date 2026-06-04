package org.example.models.dto;

public record ProfileDataDto(
        String username,
        String maskedPassword,
        String email,
        String phone
) {}