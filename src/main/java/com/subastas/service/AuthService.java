package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.model.dto.request.LoginRequest;
import com.subastas.model.dto.request.RegistroPaso1Request;
import com.subastas.model.dto.request.RegistroPaso2Request;
import com.subastas.model.dto.response.LoginResponse;
import com.subastas.model.dto.response.RegistroResponse;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoUsuario;
import com.subastas.repository.UsuarioRepository;
import com.subastas.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.subastas.util.FileUtil;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lógica de autenticación y registro en dos pasos.
 *
 * <p>Flujo de registro:
 * <ol>
 *   <li>Paso 1 – se crea el usuario en estado PENDIENTE_VERIFICACION y se dispara
 *       una verificación mock asíncrona que envía el token al email.</li>
 *   <li>Paso 2 – el usuario presenta el token recibido junto con su contraseña;
 *       la cuenta pasa a APROBADO y se devuelve un JWT listo para usar.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final MockVerificacionService mockVerificacionService;

    @Transactional
    public RegistroResponse registroPaso1(RegistroPaso1Request request,
                                          MultipartFile fotoDniFrente,
                                          MultipartFile fotoDniDorso) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCodes.EMAIL_DUPLICADO,
                    "El email ya está registrado", HttpStatus.CONFLICT);
        }

        if (usuarioRepository.existsByNumeroDni(request.getNumeroDni())) {
            throw new BusinessException(ErrorCodes.EMAIL_DUPLICADO,
                    "El DNI ya está registrado", HttpStatus.CONFLICT);
        }

        // Token UUID de un solo uso con vigencia de 24 h para la activación por email
        String token = UUID.randomUUID().toString();

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .numeroDni(request.getNumeroDni())
                .domicilioLegal(request.getDomicilioLegal())
                .paisOrigen(request.getPaisOrigen())
                .fotoDniFrente(fotoDniFrente != null ? "uploads/dni/" + FileUtil.uuidFilename(fotoDniFrente) : null)
                .fotoDniDorso(fotoDniDorso != null ? "uploads/dni/" + FileUtil.uuidFilename(fotoDniDorso) : null)
                .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                .categoria(Categoria.COMUN)
                .tokenEmail(token)
                .tokenExpiracion(LocalDateTime.now().plusHours(24))
                .build();

        usuario = usuarioRepository.save(usuario);

        // Verificación mock asíncrona (3 segundos delay, siempre exitosa) + envío de email
        mockVerificacionService.verificarYEnviarEmail(usuario.getId(), token);

        return RegistroResponse.builder()
                .usuarioId(usuario.getId())
                .estado("pendiente_verificacion")
                .mensaje("Registro iniciado. Recibirás un email con el token para completar el registro.")
                .build();
    }

    @Transactional
    public RegistroResponse registroPaso2(RegistroPaso2Request request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCodes.TOKEN_INVALIDO,
                        "Token o email inválido", HttpStatus.BAD_REQUEST));

        if (!request.getTokenEmail().equals(usuario.getTokenEmail())) {
            throw new BusinessException(ErrorCodes.TOKEN_INVALIDO, "Token inválido");
        }

        if (usuario.getTokenExpiracion() == null || LocalDateTime.now().isAfter(usuario.getTokenExpiracion())) {
            throw new BusinessException(ErrorCodes.TOKEN_INVALIDO, "El token expiró");
        }

        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new BusinessException(ErrorCodes.USUARIO_BLOQUEADO,
                    "La cuenta está bloqueada", HttpStatus.FORBIDDEN);
        }

        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEstado(EstadoUsuario.APROBADO);
        usuario.setTokenEmail(null);
        usuario.setTokenExpiracion(null);
        usuario = usuarioRepository.save(usuario);

        String jwt = jwtUtil.generateToken(usuario.getEmail());

        return RegistroResponse.builder()
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .categoria(usuario.getCategoria().name())
                .tokenAcceso(jwt)
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCodes.CREDENCIALES_INVALIDAS,
                        "Email o contraseña incorrectos", HttpStatus.UNAUTHORIZED));

        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new BusinessException(ErrorCodes.USUARIO_BLOQUEADO,
                    "La cuenta está bloqueada", HttpStatus.FORBIDDEN);
        }

        if (usuario.getEstado() == EstadoUsuario.PENDIENTE_VERIFICACION) {
            throw new BusinessException(ErrorCodes.TOKEN_INVALIDO,
                    "Debés completar el registro primero", HttpStatus.FORBIDDEN);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String jwt = jwtUtil.generateToken(request.getEmail());

        return LoginResponse.builder()
                .tokenAcceso(jwt)
                .tokenRefresh(jwtUtil.generateToken(request.getEmail()))
                .usuario(LoginResponse.UsuarioInfo.builder()
                        .id(usuario.getId())
                        .nombre(usuario.getNombre())
                        .apellido(usuario.getApellido())
                        .email(usuario.getEmail())
                        .categoria(usuario.getCategoria())
                        .estado(usuario.getEstado())
                        .build())
                .build();
    }
}
