package com.subastas.service;

import com.subastas.event.RegistroCompletadoEvent;
import com.subastas.model.entity.Usuario;
import com.subastas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Simula el proceso de verificación de identidad que en producción
 * realizaría un servicio externo (ej. validación de DNI).
 * Se ejecuta de forma asíncrona DESPUÉS del commit de la transacción de registro
 * para garantizar que el usuario ya esté persistido antes de enviar el email.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockVerificacionService {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void verificarYEnviarEmail(RegistroCompletadoEvent event) {
        try {
            log.debug("Iniciando verificación mock para usuario {}", event.getUsuarioId());
            Thread.sleep(3000);

            Usuario usuario = usuarioRepository.findById(event.getUsuarioId()).orElse(null);
            if (usuario == null) {
                log.warn("Usuario {} no encontrado tras verificación mock", event.getUsuarioId());
                return;
            }

            log.debug("Verificación mock completada para {}. Enviando email.", usuario.getEmail());
            emailService.enviarTokenRegistro(usuario.getEmail(), usuario.getNombre(), event.getToken());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Verificación mock interrumpida para usuario {}", event.getUsuarioId());
        }
    }
}
