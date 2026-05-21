package com.subastas.service;

import com.subastas.model.entity.Usuario;
import com.subastas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simula el proceso de verificación de identidad que en producción
 * realizaría un servicio externo (ej. validación de DNI).
 * Ejecuta de forma asíncrona para no bloquear la respuesta del registro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockVerificacionService {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    /**
     * Espera 3 segundos para simular latencia de verificación externa,
     * luego envía el token de activación al email del usuario.
     */
    @Async
    @Transactional
    public void verificarYEnviarEmail(Long usuarioId, String token) {
        try {
            log.debug("Iniciando verificación mock para usuario {}", usuarioId);
            Thread.sleep(3000);

            // En producción aquí se consultaría el resultado del servicio externo
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario == null) {
                log.warn("Usuario {} no encontrado tras verificación mock", usuarioId);
                return;
            }

            // La empresa asigna la categoría manualmente; aquí solo se envía el email de activación
            log.debug("Verificación mock completada para {}. Enviando email.", usuario.getEmail());
            emailService.enviarTokenRegistro(usuario.getEmail(), usuario.getNombre(), token);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Verificación mock interrumpida para usuario {}", usuarioId);
        }
    }
}
