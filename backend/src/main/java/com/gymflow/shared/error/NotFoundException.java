package com.gymflow.shared.error;

/** Base para "no existe" en cualquier módulo — GlobalExceptionHandler la mapea a 404 una sola vez. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
