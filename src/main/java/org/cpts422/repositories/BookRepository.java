package org.cpts422.repositories;

import org.cpts422.models.Book;
import org.cpts422.models.User;

public interface BookRepository {
    Book findBookByTitle(String title);
    void updateBook(Book book);
    void addBookToUser(User user, Book book);
    void removeBookFromUser(User user, Book book);
    int countBooksRentedBy(User user);
}
