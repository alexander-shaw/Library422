package org.cpts422;

import org.cpts422.exceptions.BookNotFoundException;
import org.cpts422.exceptions.RentalNotAllowedException;
import org.cpts422.exceptions.UserNotFoundException;
import org.cpts422.models.Book;
import org.cpts422.models.BookCategory;
import org.cpts422.models.MembershipType;
import org.cpts422.models.User;
import org.cpts422.repositories.BookRepository;
import org.cpts422.repositories.InMemoryBookRepository;
import org.cpts422.repositories.InMemoryUserRepository;
import org.cpts422.repositories.UserRepository;
import org.cpts422.services.InMemoryPaymentService;
import org.cpts422.services.PaymentService;
import org.cpts422.services.RentalService;
import org.cpts422.services.SystemDateProvider;

public class Main {
    public static void main(String[] args) {
        BookRepository bookRepository = new InMemoryBookRepository();
        UserRepository userRepository = new InMemoryUserRepository();
        PaymentService paymentService = new InMemoryPaymentService();

        RentalService rentalService = new RentalService(
                bookRepository, userRepository, paymentService, new SystemDateProvider());

        bookRepository.updateBook(new Book("Effective Java", "Joshua Bloch", BookCategory.REGULAR));
        bookRepository.updateBook(new Book("Clean Code", "Robert C. Martin", BookCategory.NEW_RELEASE));
        bookRepository.updateBook(new Book("Java Language Specification", "James Gosling", BookCategory.REFERENCE));
        userRepository.addUser(new User("Alice", 1, MembershipType.STUDENT));
        userRepository.addUser(new User("Bob", 2, MembershipType.FACULTY));

        try {
            rentalService.rentBook(1, "Effective Java");              // rented, due in 21 days
            rentalService.rentBook(2, "Clean Code");                  // rented, due in 14 days (faculty)
            rentalService.rentBook(1, "Java Language Specification"); // reference book, not allowed
        } catch (BookNotFoundException | UserNotFoundException | RentalNotAllowedException e) {
            System.out.println(e.getMessage());
        }

        try {
            rentalService.returnBook(1, "Effective Java");            // returned on time, no fee
        } catch (BookNotFoundException | UserNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}
