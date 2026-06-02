package com.subastas.service;

import com.subastas.model.entity.Usuario;
import com.subastas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Simula el proceso de verificación de identidad que en producción
 * realizaría un servicio externo (ej. validación de DNI).
 * Se ejecuta de forma asíncrona para no bloquear la respuesta del registro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockVerificacionService {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    @Async
    public void verificarYEnviarEmail(Long usuarioId, String token) {
        try {
            log.debug("Iniciando verificación mock para usuario {}", usuarioId);
            Thread.sleep(3000);

            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario == null) {
                log.warn("Usuario {} no encontrado tras verificación mock", usuarioId);
                return;
            }

            log.debug("Verificación mock completada para {}. Enviando email.", usuario.getEmail());
            emailService.enviarTokenRegistro(usuario.getEmail(), usuario.getNombre(), token);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Verificación mock interrumpida para usuario {}", usuarioId);
        } catch (Exception e) {
            log.error("Error enviando email de registro para usuario {}: {}", usuarioId, e.getMessage());
        }
    }
}
