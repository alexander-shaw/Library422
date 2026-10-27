package org.cpts422.models;

import java.time.LocalDate;

public record Book(String title, String author, BookCategory category,
                   boolean available, User currentUser, LocalDate dueDate) {
    public Book {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (author == null || author.isEmpty()) {
            throw new IllegalArgumentException("Author cannot be null or empty");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
    }

    // Convenience constructor for a book sitting on the shelf
    public Book(String title, String author, BookCategory category) {
        this(title, author, category, true, null, null);
    }

    public Book rentTo(User user, LocalDate newDueDate) {
        return new Book(title, author, category, false, user, newDueDate);
    }

    public Book returnToShelf() {
        return new Book(title, author, category, true, null, null);
    }
}
