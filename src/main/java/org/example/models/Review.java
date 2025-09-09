package org.example.models;

import java.time.LocalDateTime;

public class Review {
    private int userId;
    private String username;
    private int rating;
    private String title;
    private String comment;
    private LocalDateTime createdAt;

    public Review() {}

    public Review(int userId, String username, int rating, String title, String comment, LocalDateTime createdAt) {
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

    public void setUserId(int userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setRating(int rating) { this.rating = rating; }
    public void setTitle(String title) { this.title = title; }
    public void setComment(String comment) { this.comment = comment; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}