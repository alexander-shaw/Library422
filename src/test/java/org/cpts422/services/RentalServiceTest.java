package org.cpts422.services;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2024, 6, 15);

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
                () -> rentalService.rentBook(unknownUserId, "Clean Code")
        );

        assertEquals("User with ID " + unknownUserId + " not found", exception.getMessage());
        verify(userRepository).findUserById(unknownUserId);
        verifyNoInteractions(bookRepository, paymentService);
        verify(dateProvider, never()).today();
    }

    @ParameterizedTest(name = "{0} renting {1} book is due in {2} days")
    @MethodSource("successfulRentBookCases")
    @DisplayName("Successful rentBook returns correct due date for membership and category")
    void rentBookSucceedsWithCorrectDueDateForMembershipAndCategory(
            MembershipType membership, BookCategory category, int expectedLoanDays) {
        int userId = 1;
        String bookTitle = "Clean Code";
        User user = new User("Alice", userId, membership);
        Book availableBook = new Book(bookTitle, "Robert Martin", category);
        LocalDate expectedDueDate = TODAY.plusDays(expectedLoanDays);

        when(userRepository.findUserById(userId)).thenReturn(user);
        when(bookRepository.findBookByTitle(bookTitle)).thenReturn(availableBook);
        when(paymentService.getOutstandingBalance(user)).thenReturn(0);
        when(bookRepository.countBooksRentedBy(user)).thenReturn(0);

        LocalDate actualDueDate = rentalService.rentBook(userId, bookTitle);

        assertEquals(expectedDueDate, actualDueDate);

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).updateBook(bookCaptor.capture());
        Book rentedBook = bookCaptor.getValue();
        assertFalse(rentedBook.available());
        assertEquals(user, rentedBook.currentUser());
        assertEquals(expectedDueDate, rentedBook.dueDate());
        assertEquals(category, rentedBook.category());
        assertEquals(bookTitle, rentedBook.title());

        verify(userRepository).findUserById(userId);
        verify(bookRepository).findBookByTitle(bookTitle);
        verify(paymentService).getOutstandingBalance(user);
        verify(bookRepository).countBooksRentedBy(user);
        verify(bookRepository).addBookToUser(user, rentedBook);
        verify(dateProvider).today();
        verifyNoMoreInteractions(bookRepository, userRepository, paymentService);
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
}
