package org.example.models.dto;

import java.time.LocalDateTime;

public record ReviewDto(
        String username,
        int rating,
        String title,
        String comment,
        LocalDateTime createdAt
) {}
