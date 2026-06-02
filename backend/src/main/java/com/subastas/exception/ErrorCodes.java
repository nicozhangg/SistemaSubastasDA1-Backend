package com.subastas.exception;

/** Constantes de códigos de error devueltos en el campo "codigo" del ErrorResponse. */
public final class ErrorCodes {

    private ErrorCodes() {}

    public static final String CATEGORIA_INSUFICIENTE     = "CATEGORIA_INSUFICIENTE";
    public static final String SIN_MEDIO_PAGO_VERIFICADO  = "SIN_MEDIO_PAGO_VERIFICADO";
    public static final String USUARIO_YA_CONECTADO       = "USUARIO_YA_CONECTADO";
    public static final String USUARIO_NO_CONECTADO       = "USUARIO_NO_CONECTADO";
    public static final String MONTO_FUERA_DE_RANGO       = "MONTO_FUERA_DE_RANGO";
    public static final String PUJA_EN_PROCESO            = "PUJA_EN_PROCESO";
    public static final String MULTA_PENDIENTE            = "MULTA_PENDIENTE";
    public static final String EMAIL_DUPLICADO            = "EMAIL_DUPLICADO";
    public static final String DNI_DUPLICADO              = "DNI_DUPLICADO";
    public static final String TOKEN_INVALIDO             = "TOKEN_INVALIDO";
    public static final String USUARIO_BLOQUEADO          = "USUARIO_BLOQUEADO";
    public static final String RECURSO_NO_ENCONTRADO      = "RECURSO_NO_ENCONTRADO";
    public static final String ACCESO_DENEGADO            = "ACCESO_DENEGADO";
    public static final String SUBASTA_NO_ABIERTA         = "SUBASTA_NO_ABIERTA";
    public static final String ESTADO_INVALIDO            = "ESTADO_INVALIDO";
    public static final String MEDIO_PAGO_EN_USO          = "MEDIO_PAGO_EN_USO";
    public static final String FOTOS_INSUFICIENTES        = "FOTOS_INSUFICIENTES";
    public static final String PERTENENCIA_NO_DECLARADA   = "PERTENENCIA_NO_DECLARADA";
    public static final String CREDENCIALES_INVALIDAS     = "CREDENCIALES_INVALIDAS";
    public static final String REGISTRO_INCOMPLETO        = "REGISTRO_INCOMPLETO";
    public static final String ENTREGA_YA_CONFIRMADA      = "ENTREGA_YA_CONFIRMADA";
    public static final String DIRECCION_REQUERIDA        = "DIRECCION_REQUERIDA";
    public static final String LIMITE_CHEQUE_EXCEDIDO     = "LIMITE_CHEQUE_EXCEDIDO";
    public static final String MONEDA_NO_COINCIDE         = "MONEDA_NO_COINCIDE";
    public static final String ARCHIVO_INVALIDO           = "ARCHIVO_INVALIDO";
    public static final String ARCHIVO_MUY_GRANDE         = "ARCHIVO_MUY_GRANDE";
}
