package com.gymflow.auth.application;

/** Deliberadamente genérica: no distingue "usuario no existe" de "contraseña incorrecta" (evita enumeración de usuarios). */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email o contraseña incorrectos.");
    }
}
