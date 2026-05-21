package com.subastas.config;

import com.subastas.security.UserDetailsServiceImpl;
import com.subastas.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Interceptor STOMP que autentica la conexión WebSocket a partir del header
 * Authorization enviado en el frame CONNECT. Sin este interceptor, el Principal
 * sería nulo y no se podrían enviar mensajes privados por usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String email = jwtUtil.extractEmail(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if (jwtUtil.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(auth);
                    }
                } catch (Exception e) {
                    log.warn("Token WebSocket inválido: {}", e.getMessage());
                }
            }
        }

        return message;
    }
}
