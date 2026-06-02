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
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.subastas.util.FileUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${app.uploads.base-path:uploads}")
    private String uploadsBasePath;

    @Transactional
    public RegistroResponse registroPaso1(RegistroPaso1Request request,
                                          MultipartFile fotoDniFrente,
                                          MultipartFile fotoDniDorso) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCodes.EMAIL_DUPLICADO,
                    "El email ya está registrado", HttpStatus.CONFLICT);
        }

        if (usuarioRepository.existsByNumeroDni(request.getNumeroDni())) {
            throw new BusinessException(ErrorCodes.DNI_DUPLICADO,
                    "El DNI ya está registrado", HttpStatus.CONFLICT);
        }

        // Token UUID de un solo uso con vigencia de 24 h para la activación por email
        String token = UUID.randomUUID().toString();

        String rutaFrente = guardarArchivoDni(fotoDniFrente);
        String rutaDorso  = guardarArchivoDni(fotoDniDorso);

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .numeroDni(request.getNumeroDni())
                .domicilioLegal(request.getDomicilioLegal())
                .paisOrigen(request.getPaisOrigen())
                .fotoDniFrente(rutaFrente)
                .fotoDniDorso(rutaDorso)
                .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                .categoria(Categoria.COMUN)
                .tokenEmail(token)
                .tokenExpiracion(LocalDateTime.now().plusHours(24))
                .build();

        usuario = usuarioRepository.save(usuario);

        // Se dispara de forma asíncrona: duerme 3s (mock) y luego envía el email con el token
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

    private String guardarArchivoDni(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                    "Las fotos del DNI son obligatorias para completar el registro",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        try {
            String contentType = archivo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                        "Solo se permiten imágenes para el DNI");
            }
            String nombreArchivo = uploadsBasePath + "/dni/" + FileUtil.uuidFilename(archivo);
            Path destino = Paths.get(nombreArchivo);
            Files.createDirectories(destino.getParent());
            archivo.transferTo(destino.toFile());
            return nombreArchivo;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo de DNI", e);
        }
    }

    public LoginResponse login(LoginRequest request) {
        // Autenticar primero para evitar revelar el estado de la cuenta con contraseña incorrecta
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCodes.CREDENCIALES_INVALIDAS,
                        "Email o contraseña incorrectos", HttpStatus.UNAUTHORIZED));

        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new BusinessException(ErrorCodes.USUARIO_BLOQUEADO,
                    "La cuenta está bloqueada", HttpStatus.FORBIDDEN);
        }

        if (usuario.getEstado() == EstadoUsuario.PENDIENTE_VERIFICACION) {
            throw new BusinessException(ErrorCodes.REGISTRO_INCOMPLETO,
                    "Debés completar el registro primero", HttpStatus.FORBIDDEN);
        }

        String jwt = jwtUtil.generateToken(request.getEmail());

        return LoginResponse.builder()
                .tokenAcceso(jwt)
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
