package com.subastas.event;

import org.springframework.context.ApplicationEvent;

public class RegistroCompletadoEvent extends ApplicationEvent {

    private final Long usuarioId;
    private final String token;

    public RegistroCompletadoEvent(Object source, Long usuarioId, String token) {
        super(source);
        this.usuarioId = usuarioId;
        this.token = token;
    }

    public Long getUsuarioId() { return usuarioId; }
    public String getToken()   { return token; }
}
