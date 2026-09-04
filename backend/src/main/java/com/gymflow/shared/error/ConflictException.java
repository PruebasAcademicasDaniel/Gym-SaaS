package com.gymflow.shared.error;

/** Base para "esto ya existe / viola una regla de unicidad" — GlobalExceptionHandler la mapea a 409 una sola vez. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
