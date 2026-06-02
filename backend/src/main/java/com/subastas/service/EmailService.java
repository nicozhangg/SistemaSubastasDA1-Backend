package com.subastas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@subastas.com}")
    private String from;

    public void enviarTokenRegistro(String emailDestino, String nombre, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(emailDestino);
        message.setSubject("Completa tu registro en Sistema de Subastas");
        message.setText(String.format(
            "Hola %s,\n\n" +
            "Tu token de registro es: %s\n\n" +
            "Usá este token junto con tu email para completar el registro y definir tu contraseña.\n\n" +
            "El token expira en 24 horas.\n\n" +
            "Sistema de Subastas",
            nombre, token
        ));
        mailSender.send(message);
        log.debug("Email de registro enviado a {}", emailDestino);
    }

    @Async
    public void enviarNotificacionGanador(String emailDestino, String nombre, String descripcionItem,
                                          String desglose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(emailDestino);
            message.setSubject("¡Ganaste la subasta!");
            message.setText(String.format(
                "Hola %s,\n\n" +
                "¡Felicitaciones! Ganaste el ítem: %s\n\n" +
                "%s\n\n" +
                "Sistema de Subastas",
                nombre, descripcionItem, desglose
            ));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error enviando notificación de ganador a {}: {}", emailDestino, e.getMessage());
        }
    }
}
