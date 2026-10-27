package org.cpts422.services;

import org.cpts422.exceptions.RentalNotAllowedException;
import org.cpts422.models.Book;
import org.cpts422.models.BookCategory;
import org.cpts422.models.MembershipType;
import org.cpts422.models.User;
import org.cpts422.repositories.BookRepository;
import org.cpts422.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServicePaymentSpyTest {

    private static final LocalDate TODAY = LocalDate.of(2024, 6, 15);
    private static final int USER_ID = 1;
    private static final String BOOK_TITLE = "Clean Code";
    private static final String BOOK_AUTHOR = "Robert Martin";

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DateProvider dateProvider;

    @Spy
    private InMemoryPaymentService paymentService = new InMemoryPaymentService();

    @InjectMocks
    private RentalService rentalService;

    @BeforeEach
    void setUp() {
        when(dateProvider.today()).thenReturn(TODAY);
    }

    @Test
    @DisplayName("Late return records a real outstanding balance that blocks the next rental")
    void lateReturnRecordsOutstandingBalanceAndBlocksSubsequentRental() {
        User user = new User("Alice", USER_ID, MembershipType.COMMUNITY);
        LocalDate dueDate = TODAY.minusDays(3);
        Book overdueBook = new Book(BOOK_TITLE, BOOK_AUTHOR, BookCategory.REGULAR)
                .rentTo(user, dueDate);
        // COMMUNITY regular: 3 days late, 0 grace, 50 cents/day → 150
        int expectedLateFeeCents = 150;

        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(overdueBook);

        int chargedFee = rentalService.returnBook(USER_ID, BOOK_TITLE);

        assertEquals(expectedLateFeeCents, chargedFee);
        verify(paymentService).chargeLateFee(user, expectedLateFeeCents);
        assertEquals(expectedLateFeeCents, paymentService.getOutstandingBalance(user));

        clearInvocations(paymentService, bookRepository);

        Book availableBook = new Book(BOOK_TITLE, BOOK_AUTHOR, BookCategory.REGULAR);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(availableBook);

        RentalNotAllowedException exception = assertThrows(
                RentalNotAllowedException.class,
                () -> rentalService.rentBook(USER_ID, BOOK_TITLE)
        );

        assertEquals(user.name() + " has unpaid late fees", exception.getMessage());
        verify(paymentService).getOutstandingBalance(user);
        assertEquals(expectedLateFeeCents, paymentService.getOutstandingBalance(user));
        verify(bookRepository, never()).updateBook(any());
        verify(bookRepository, never()).addBookToUser(any(), any());
        verify(bookRepository, never()).countBooksRentedBy(any());
    }
}
