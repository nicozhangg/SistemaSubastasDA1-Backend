package com.subastas.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String recurso, Long id) {
        super(ErrorCodes.RECURSO_NO_ENCONTRADO,
              recurso + " con id " + id + " no encontrado",
              HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String mensaje) {
        super(ErrorCodes.RECURSO_NO_ENCONTRADO, mensaje, HttpStatus.NOT_FOUND);
    }
}
