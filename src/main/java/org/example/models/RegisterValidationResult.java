package org.example.models;

public enum RegisterValidationResult {
    SUCCESS, EMPTY_FIELDS, PASSWORD_MISMATCH, INVALID_EMAIL,
    INVALID_PHONE, USERNAME_TAKEN, EMAIL_TAKEN, DATABASE_ERROR
}

