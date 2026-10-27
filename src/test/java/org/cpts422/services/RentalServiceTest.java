package org.cpts422.services;

import org.cpts422.exceptions.UserNotFoundException;
import org.cpts422.repositories.BookRepository;
import org.cpts422.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
}
