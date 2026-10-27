package org.cpts422.repositories;

import org.cpts422.models.Book;
import org.cpts422.models.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InMemoryBookRepository implements BookRepository {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<Integer, Set<String>> rentedTitlesByUserId = new HashMap<>();

    @Override
    public Book findBookByTitle(String title) {
        return books.get(title);
    }

    @Override
    public void updateBook(Book book) {
        books.put(book.title(), book);
    }

    @Override
    public void addBookToUser(User user, Book book) {
        rentedTitlesByUserId.computeIfAbsent(user.userId(), id -> new HashSet<>()).add(book.title());
    }

    @Override
    public void removeBookFromUser(User user, Book book) {
        Set<String> titles = rentedTitlesByUserId.get(user.userId());
        if (titles != null) {
            titles.remove(book.title());
            if (titles.isEmpty()) {
                rentedTitlesByUserId.remove(user.userId());
            }
        }
    }

    @Override
    public int countBooksRentedBy(User user) {
        Set<String> titles = rentedTitlesByUserId.get(user.userId());
        return titles == null ? 0 : titles.size();
    }
}
