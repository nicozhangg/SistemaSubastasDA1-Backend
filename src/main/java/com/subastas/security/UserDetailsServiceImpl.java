package com.subastas.security;

import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoUsuario;
import com.subastas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de UserDetailsService que carga el usuario desde la base de datos
 * por email. Mapea el estado BLOQUEADO de la entidad a las flags de Spring Security
 * (disabled/accountLocked) para que el AuthenticationManager rechace el login.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        boolean activo = usuario.getEstado() != EstadoUsuario.BLOQUEADO;

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword() != null ? usuario.getPassword() : "")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .disabled(!activo)
                .accountLocked(!activo)
                .build();
    }
}
