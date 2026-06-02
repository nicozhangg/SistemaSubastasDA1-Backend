package com.subastas.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción para violaciones de reglas de negocio.
 * Incluye un código de error (ver {@link ErrorCodes}) y el HTTP status
 * correspondiente para que el GlobalExceptionHandler construya la respuesta.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String codigo;
    private final HttpStatus httpStatus;

    public BusinessException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String codigo, String mensaje, HttpStatus httpStatus) {
        super(mensaje);
        this.codigo = codigo;
        this.httpStatus = httpStatus;
    }
}
