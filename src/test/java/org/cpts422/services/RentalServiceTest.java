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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2024, 6, 15);
    private static final int USER_ID = 1;
    private static final String BOOK_TITLE = "Clean Code";
    private static final String BOOK_AUTHOR = "Robert Martin";

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private DateProvider dateProvider;

    @InjectMocks
    private RentalService rentalService;

    @BeforeEach
    void setUp() {
        lenient().when(dateProvider.today()).thenReturn(TODAY);
    }

    @Test
    @DisplayName("Renting a book with an unknown user throws UserNotFoundException")
    void rentBookWithUnknownUserThrowsUserNotFoundException() {
        int unknownUserId = 999;
        when(userRepository.findUserById(unknownUserId)).thenReturn(null);

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> rentalService.rentBook(unknownUserId, BOOK_TITLE)
        );

        assertEquals("User with ID " + unknownUserId + " not found", exception.getMessage());
        verify(userRepository).findUserById(unknownUserId);
        verifyNoInteractions(bookRepository, paymentService);
        verify(dateProvider, never()).today();
        verifyNoRentalPersistence();
    }

    @Test
    @DisplayName("Renting an unknown book throws BookNotFoundException")
    void rentBookWithUnknownBookThrowsBookNotFoundException() {
        User user = user(MembershipType.STUDENT);
        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(null);

        BookNotFoundException exception = assertThrows(
                BookNotFoundException.class,
                () -> rentalService.rentBook(USER_ID, BOOK_TITLE)
        );

        assertEquals("Book " + BOOK_TITLE + " not found", exception.getMessage());
        verify(userRepository).findUserById(USER_ID);
        verify(bookRepository).findBookByTitle(BOOK_TITLE);
        verify(paymentService, never()).getOutstandingBalance(any());
        verify(bookRepository, never()).countBooksRentedBy(any());
        verify(dateProvider, never()).today();
        verifyNoRentalPersistence();
    }

    @Test
    @DisplayName("Renting a reference book throws RentalNotAllowedException")
    void rentBookWithReferenceBookThrowsRentalNotAllowedException() {
        User user = user(MembershipType.STUDENT);
        Book referenceBook = new Book(BOOK_TITLE, BOOK_AUTHOR, BookCategory.REFERENCE);
        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(referenceBook);

        RentalNotAllowedException exception = assertThrows(
                RentalNotAllowedException.class,
                () -> rentalService.rentBook(USER_ID, BOOK_TITLE)
        );

        assertEquals("Reference books cannot leave the library", exception.getMessage());
        verify(userRepository).findUserById(USER_ID);
        verify(bookRepository).findBookByTitle(BOOK_TITLE);
        verify(paymentService, never()).getOutstandingBalance(any());
        verify(bookRepository, never()).countBooksRentedBy(any());
        verify(dateProvider, never()).today();
        verifyNoRentalPersistence();
    }

    @Test
    @DisplayName("Renting an already rented book throws RentalNotAllowedException")
    void rentBookWhenAlreadyRentedThrowsRentalNotAllowedException() {
        User user = user(MembershipType.STUDENT);
        User otherUser = new User("Bob", 2, MembershipType.FACULTY);
        Book rentedBook = new Book(BOOK_TITLE, BOOK_AUTHOR, BookCategory.REGULAR)
                .rentTo(otherUser, TODAY.plusDays(7));
        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(rentedBook);

        RentalNotAllowedException exception = assertThrows(
                RentalNotAllowedException.class,
                () -> rentalService.rentBook(USER_ID, BOOK_TITLE)
        );

        assertEquals("Book " + BOOK_TITLE + " is already rented", exception.getMessage());
        verify(userRepository).findUserById(USER_ID);
        verify(bookRepository).findBookByTitle(BOOK_TITLE);
        verify(paymentService, never()).getOutstandingBalance(any());
        verify(bookRepository, never()).countBooksRentedBy(any());
        verify(dateProvider, never()).today();
        verifyNoRentalPersistence();
    }

    @Test
    @DisplayName("Renting with an outstanding late-fee balance throws RentalNotAllowedException")
    void rentBookWithOutstandingLateFeeBalanceThrowsRentalNotAllowedException() {
        User user = user(MembershipType.STUDENT);
        Book availableBook = availableBook(BookCategory.REGULAR);
        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(availableBook);
        when(paymentService.getOutstandingBalance(user)).thenReturn(500);

        RentalNotAllowedException exception = assertThrows(
                RentalNotAllowedException.class,
                () -> rentalService.rentBook(USER_ID, BOOK_TITLE)
        );

        assertEquals(user.name() + " has unpaid late fees", exception.getMessage());
        verify(userRepository).findUserById(USER_ID);
        verify(bookRepository).findBookByTitle(BOOK_TITLE);
        verify(paymentService).getOutstandingBalance(user);
        verify(bookRepository, never()).countBooksRentedBy(any());
        verify(dateProvider, never()).today();
        verifyNoRentalPersistence();
    }

    @ParameterizedTest(name = "{0} at rental limit of {1} is rejected")
    @MethodSource("rentalLimitCases")
    @DisplayName("Renting at membership rental limit throws RentalNotAllowedException")
    void rentBookAtRentalLimitThrowsRentalNotAllowedException(MembershipType membership, int rentalLimit) {
        User user = user(membership);
        Book availableBook = availableBook(BookCategory.REGULAR);
        stubEligibleUserAndBook(user, availableBook);
        when(bookRepository.countBooksRentedBy(user)).thenReturn(rentalLimit);

        RentalNotAllowedException exception = assertThrows(
                RentalNotAllowedException.class,
                () -> rentalService.rentBook(USER_ID, BOOK_TITLE)
        );

        assertEquals(user.name() + " has reached the rental limit", exception.getMessage());
        verify(userRepository).findUserById(USER_ID);
        verify(bookRepository).findBookByTitle(BOOK_TITLE);
        verify(paymentService).getOutstandingBalance(user);
        verify(bookRepository).countBooksRentedBy(user);
        verify(dateProvider, never()).today();
        verifyNoRentalPersistence();
    }

    @ParameterizedTest(name = "{0} with {1} rented books (limit {2}) is eligible")
    @MethodSource("justBelowRentalLimitCases")
    @DisplayName("Renting just below membership rental limit succeeds")
    void rentBookJustBelowRentalLimitSucceeds(
            MembershipType membership, int booksAlreadyRented, int rentalLimit) {
        User user = user(membership);
        Book availableBook = availableBook(BookCategory.REGULAR);
        LocalDate expectedDueDate = TODAY.plusDays(membership == MembershipType.FACULTY ? 42 : 21);
        stubEligibleUserAndBook(user, availableBook);
        when(bookRepository.countBooksRentedBy(user)).thenReturn(booksAlreadyRented);

        LocalDate actualDueDate = rentalService.rentBook(USER_ID, BOOK_TITLE);

        assertEquals(expectedDueDate, actualDueDate);
        assertEquals(rentalLimit - 1, booksAlreadyRented);

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).updateBook(bookCaptor.capture());
        Book rentedBook = bookCaptor.getValue();
        assertFalse(rentedBook.available());
        assertEquals(user, rentedBook.currentUser());
        assertEquals(expectedDueDate, rentedBook.dueDate());

        verify(bookRepository).addBookToUser(user, rentedBook);
        verify(bookRepository).countBooksRentedBy(user);
        verify(dateProvider).today();
    }

    @ParameterizedTest(name = "{0} renting {1} book is due in {2} days")
    @MethodSource("successfulRentBookCases")
    @DisplayName("Successful rentBook returns correct due date for membership and category")
    void rentBookSucceedsWithCorrectDueDateForMembershipAndCategory(
            MembershipType membership, BookCategory category, int expectedLoanDays) {
        User user = user(membership);
        Book availableBook = availableBook(category);
        LocalDate expectedDueDate = TODAY.plusDays(expectedLoanDays);

        stubEligibleUserAndBook(user, availableBook);
        when(bookRepository.countBooksRentedBy(user)).thenReturn(0);

        LocalDate actualDueDate = rentalService.rentBook(USER_ID, BOOK_TITLE);

        assertEquals(expectedDueDate, actualDueDate);

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).updateBook(bookCaptor.capture());
        Book rentedBook = bookCaptor.getValue();
        assertFalse(rentedBook.available());
        assertEquals(user, rentedBook.currentUser());
        assertEquals(expectedDueDate, rentedBook.dueDate());
        assertEquals(category, rentedBook.category());
        assertEquals(BOOK_TITLE, rentedBook.title());

        verify(userRepository).findUserById(USER_ID);
        verify(bookRepository).findBookByTitle(BOOK_TITLE);
        verify(paymentService).getOutstandingBalance(user);
        verify(bookRepository).countBooksRentedBy(user);
        verify(bookRepository).addBookToUser(user, rentedBook);
        verify(dateProvider).today();
        verifyNoMoreInteractions(bookRepository, userRepository, paymentService);
    }

    @Test
    @DisplayName("Returning a book with an unknown user throws UserNotFoundException")
    void returnBookWithUnknownUserThrowsUserNotFoundException() {
        int unknownUserId = 999;
        when(userRepository.findUserById(unknownUserId)).thenReturn(null);

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> rentalService.returnBook(unknownUserId, BOOK_TITLE)
        );

        assertEquals("User with ID " + unknownUserId + " not found", exception.getMessage());
        verify(userRepository).findUserById(unknownUserId);
        verifyNoInteractions(bookRepository, paymentService);
        verify(dateProvider, never()).today();
        verifyNoReturnPersistence();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("returnBookNotRentedByUserCases")
    @DisplayName("Returning a book not rented by the user throws BookNotFoundException")
    void returnBookWhenNotRentedByUserThrowsBookNotFoundException(String scenario, Book book) {
        User user = user(MembershipType.STUDENT);
        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(book);

        BookNotFoundException exception = assertThrows(
                BookNotFoundException.class,
                () -> rentalService.returnBook(USER_ID, BOOK_TITLE)
        );

        assertEquals(
                "Book " + BOOK_TITLE + " was not rented by user " + user.name(),
                exception.getMessage());
        verify(userRepository).findUserById(USER_ID);
        verify(bookRepository).findBookByTitle(BOOK_TITLE);
        verify(dateProvider, never()).today();
        verify(paymentService, never()).chargeLateFee(any(), anyInt());
        verifyNoReturnPersistence();
    }

    @ParameterizedTest(name = "due {0} days from today yields zero fee")
    @MethodSource("onTimeReturnCases")
    @DisplayName("Returning a book on or before the due date returns zero fee and does not charge")
    void returnBookOnTimeReturnsZeroFeeAndDoesNotCharge(int daysUntilDue) {
        User user = user(MembershipType.STUDENT);
        LocalDate dueDate = TODAY.plusDays(daysUntilDue);
        Book rentedBook = rentedBook(user, BookCategory.REGULAR, dueDate);
        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(rentedBook);

        int lateFee = rentalService.returnBook(USER_ID, BOOK_TITLE);

        assertEquals(0, lateFee);
        verifySuccessfulReturn(user, false, 0);
        verify(dateProvider).today();
        verify(paymentService, never()).chargeLateFee(any(), anyInt());
    }

    @Test
    @DisplayName("Returning a book late charges a positive late fee and updates the repository")
    void returnBookLateChargesPositiveLateFeeAndUpdatesRepository() {
        User user = user(MembershipType.COMMUNITY);
        LocalDate dueDate = TODAY.minusDays(3);
        Book rentedBook = rentedBook(user, BookCategory.REGULAR, dueDate);
        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(rentedBook);

        int lateFee = rentalService.returnBook(USER_ID, BOOK_TITLE);

        // COMMUNITY regular: 3 days late, 0 grace, 50 cents/day → 150
        assertEquals(150, lateFee);
        verifySuccessfulReturn(user, true, 150);
        verify(dateProvider).today();
        verify(paymentService).chargeLateFee(user, 150);
        verify(paymentService, never()).getOutstandingBalance(any());
    }

    static Stream<Arguments> successfulRentBookCases() {
        return Stream.of(
                Arguments.of(MembershipType.STUDENT, BookCategory.REGULAR, 21),
                Arguments.of(MembershipType.STUDENT, BookCategory.NEW_RELEASE, 7),
                Arguments.of(MembershipType.FACULTY, BookCategory.REGULAR, 42),
                Arguments.of(MembershipType.FACULTY, BookCategory.NEW_RELEASE, 14),
                Arguments.of(MembershipType.COMMUNITY, BookCategory.REGULAR, 21),
                Arguments.of(MembershipType.COMMUNITY, BookCategory.NEW_RELEASE, 7)
        );
    }

    static Stream<Arguments> rentalLimitCases() {
        return Stream.of(
                Arguments.of(MembershipType.STUDENT, 5),
                Arguments.of(MembershipType.FACULTY, 10),
                Arguments.of(MembershipType.COMMUNITY, 2)
        );
    }

    static Stream<Arguments> justBelowRentalLimitCases() {
        return Stream.of(
                Arguments.of(MembershipType.STUDENT, 4, 5),
                Arguments.of(MembershipType.FACULTY, 9, 10),
                Arguments.of(MembershipType.COMMUNITY, 1, 2)
        );
    }

    static Stream<Arguments> returnBookNotRentedByUserCases() {
        User otherUser = new User("Bob", 2, MembershipType.FACULTY);
        return Stream.of(
                Arguments.of("unknown book", null),
                Arguments.of("book not currently rented", new Book(BOOK_TITLE, BOOK_AUTHOR, BookCategory.REGULAR)),
                Arguments.of(
                        "book rented by another user",
                        new Book(BOOK_TITLE, BOOK_AUTHOR, BookCategory.REGULAR)
                                .rentTo(otherUser, TODAY.plusDays(7)))
        );
    }

    static Stream<Arguments> onTimeReturnCases() {
        return Stream.of(
                Arguments.of(5),  // before due date
                Arguments.of(0)   // exactly on due date
        );
    }

    private User user(MembershipType membership) {
        return new User("Alice", USER_ID, membership);
    }

    private Book availableBook(BookCategory category) {
        return new Book(BOOK_TITLE, BOOK_AUTHOR, category);
    }

    private Book rentedBook(User renter, BookCategory category, LocalDate dueDate) {
        return new Book(BOOK_TITLE, BOOK_AUTHOR, category).rentTo(renter, dueDate);
    }

    private void stubEligibleUserAndBook(User user, Book book) {
        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(bookRepository.findBookByTitle(BOOK_TITLE)).thenReturn(book);
        when(paymentService.getOutstandingBalance(user)).thenReturn(0);
    }

    private void verifyNoRentalPersistence() {
        verify(bookRepository, never()).updateBook(any());
        verify(bookRepository, never()).addBookToUser(any(), any());
    }

    private void verifyNoReturnPersistence() {
        verify(bookRepository, never()).updateBook(any());
        verify(bookRepository, never()).removeBookFromUser(any(), any());
        verify(paymentService, never()).chargeLateFee(any(), anyInt());
    }

    private void verifySuccessfulReturn(User user, boolean expectCharge, int expectedFee) {
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).updateBook(bookCaptor.capture());
        Book returnedBook = bookCaptor.getValue();
        assertTrue(returnedBook.available());
        assertNull(returnedBook.currentUser());
        assertNull(returnedBook.dueDate());
        assertEquals(BOOK_TITLE, returnedBook.title());
        verify(bookRepository).removeBookFromUser(user, returnedBook);
        if (expectCharge) {
            verify(paymentService).chargeLateFee(user, expectedFee);
        } else {
            verify(paymentService, never()).chargeLateFee(any(), anyInt());
        }
    }
}
