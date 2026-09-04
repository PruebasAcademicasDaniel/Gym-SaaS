package com.gymflow.auth.application;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("El refresh token no es válido, venció o ya fue usado.");
    }
}
