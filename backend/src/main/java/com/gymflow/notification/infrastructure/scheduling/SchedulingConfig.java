package com.gymflow.notification.infrastructure.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Vive acá, no en la clase principal, porque hoy es el único módulo que usa @Scheduled — si notification se fuera, esto se va con él. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
