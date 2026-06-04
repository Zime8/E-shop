package org.example.models.entity;

import java.time.LocalDateTime;

public class Review {
    private final int userId;
    private final String username;
    private int rating;
    private String title;
    private String comment;
    private final LocalDateTime createdAt;

    public Review(int userId, String username, int rating, String title, String comment, LocalDateTime createdAt) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }

        this.userId = userId;
        this.username = username;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getRating() { return rating; }
    public String getTitle() { return title; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.rating = rating;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }
}