package com.gymflow.ai.application;

import java.time.LocalDate;

/** Lo mínimo que un generador de mensajes necesita para personalizar un aviso — no todo el Member, para no acoplar este puerto a esa entidad. */
public record RiskAlertContext(String firstName, LocalDate lastActivity) {
}
