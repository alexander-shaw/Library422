package org.cpts422.services;

import org.cpts422.exceptions.BookNotFoundException;
import org.cpts422.exceptions.RentalNotAllowedException;
import org.cpts422.exceptions.UserNotFoundException;
import org.cpts422.models.Book;
import org.cpts422.models.BookCategory;
import org.cpts422.models.MembershipType;
import org.cpts422.models.User;
import org.cpts422.repositories.BookRepository;
import org.cpts422.repositories.UserRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RentalService {
    private static final int MAX_LATE_FEE_CENTS = 2000;

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final DateProvider dateProvider;

    public RentalService(BookRepository bookRepository, UserRepository userRepository,
                         PaymentService paymentService, DateProvider dateProvider) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.dateProvider = dateProvider;
    }

    /**
     * Rents a book to a user and returns the due date.
     */
    public LocalDate rentBook(int userId, String bookTitle) {
        User user = userRepository.findUserById(userId);
        if (user == null) {
            throw new UserNotFoundException("User with ID " + userId + " not found");
        }

        Book book = bookRepository.findBookByTitle(bookTitle);
        if (book == null) {
            throw new BookNotFoundException("Book " + bookTitle + " not found");
        }
        if (book.category() == BookCategory.REFERENCE) {
            throw new RentalNotAllowedException("Reference books cannot leave the library");
        }
        if (!book.available()) {
            throw new RentalNotAllowedException("Book " + bookTitle + " is already rented");
        }

        if (paymentService.getOutstandingBalance(user) > 0) {
            throw new RentalNotAllowedException(user.name() + " has unpaid late fees");
        }

        int rentalLimit;
        if (user.membership() == MembershipType.FACULTY) {
            rentalLimit = 10;
        } else if (user.membership() == MembershipType.STUDENT) {
            rentalLimit = 5;
        } else {
            rentalLimit = 2;
        }
        if (bookRepository.countBooksRentedBy(user) >= rentalLimit) {
            throw new RentalNotAllowedException(user.name() + " has reached the rental limit");
        }

        int loanDays;
        if (book.category() == BookCategory.NEW_RELEASE) {
            loanDays = 7;
        } else {
            loanDays = 21;
        }
        if (user.membership() == MembershipType.FACULTY) {
            loanDays = loanDays * 2;
        }

        LocalDate dueDate = dateProvider.today().plusDays(loanDays);
        Book rentedBook = book.rentTo(user, dueDate);
        bookRepository.updateBook(rentedBook);
        bookRepository.addBookToUser(user, rentedBook);
        System.out.println("Book rented successfully to " + user.name() + ", due " + dueDate);
        return dueDate;
    }

    /**
     * Returns a rented book, charges any late fee, and returns the fee in cents.
     */
    public int returnBook(int userId, String bookTitle) {
        User user = userRepository.findUserById(userId);
        if (user == null) {
            throw new UserNotFoundException("User with ID " + userId + " not found");
        }

        Book book = bookRepository.findBookByTitle(bookTitle);
        if (book == null || !user.equals(book.currentUser())) {
            throw new BookNotFoundException("Book " + bookTitle + " was not rented by user " + user.name());
        }

        int lateFee = calculateLateFee(book, user, dateProvider.today());
        if (lateFee > 0) {
            paymentService.chargeLateFee(user, lateFee);
        }

        Book returnedBook = book.returnToShelf();
        bookRepository.updateBook(returnedBook);
        bookRepository.removeBookFromUser(user, returnedBook);
        System.out.println("Book returned successfully by " + user.name() + ", late fee " + lateFee + " cents");
        return lateFee;
    }

    /**
     * Calculates the late fee in cents for returning the given book on returnDate.
     */
    public int calculateLateFee(Book book, User user, LocalDate returnDate) {
        if (book.dueDate() == null || !returnDate.isAfter(book.dueDate())) {
            return 0;
        }

        long daysLate = ChronoUnit.DAYS.between(book.dueDate(), returnDate);

        int dailyRateCents;
        if (book.category() == BookCategory.NEW_RELEASE) {
            if (user.membership() == MembershipType.FACULTY) {
                dailyRateCents = 25;
            } else if (user.membership() == MembershipType.STUDENT) {
                dailyRateCents = 50;
            } else {
                dailyRateCents = 100;
            }
        } else {
            if (user.membership() == MembershipType.FACULTY) {
                dailyRateCents = 10;
            } else if (user.membership() == MembershipType.STUDENT) {
                dailyRateCents = 25;
            } else {
                dailyRateCents = 50;
            }
        }

        int graceDays;
        if (user.membership() == MembershipType.FACULTY) {
            graceDays = 7;
        } else if (user.membership() == MembershipType.STUDENT) {
            graceDays = 2;
        } else {
            graceDays = 0;
        }

        long chargeableDays = daysLate - graceDays;
        if (chargeableDays <= 0) {
            return 0;
        }

        long fee = chargeableDays * dailyRateCents;
        if (fee > MAX_LATE_FEE_CENTS) {
            fee = MAX_LATE_FEE_CENTS;
        }
        return (int) fee;
    }
}
