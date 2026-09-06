package com.gymflow.notification.infrastructure.email;

import com.gymflow.notification.application.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Adapter provisorio: loguea en vez de mandar un email real — ver EmailSender. */
@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("EMAIL a {} — asunto: \"{}\" — cuerpo: {}", to, subject, body);
    }
}
