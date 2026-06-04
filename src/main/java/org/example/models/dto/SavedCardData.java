package org.example.models.dto;

public record SavedCardData(
        int id,
        String holder,
        String number,
        String expiry,
        String type
) {}