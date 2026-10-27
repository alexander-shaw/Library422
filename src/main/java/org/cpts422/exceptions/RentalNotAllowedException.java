package org.cpts422.exceptions;

public class RentalNotAllowedException extends RuntimeException {
    public RentalNotAllowedException(String message) {
        super(message);
    }
}
