package org.cpts422.models;

public record User(String name, int userId, MembershipType membership) {
    public User {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (membership == null) {
            throw new IllegalArgumentException("Membership type cannot be null");
        }
    }
}
