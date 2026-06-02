package com.subastas.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistroPaso1Request {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato válido")
    private String email;

    @NotBlank(message = "El número de DNI es obligatorio")
    private String numeroDni;

    @NotBlank(message = "El domicilio legal es obligatorio")
    private String domicilioLegal;

    @NotBlank(message = "El país de origen es obligatorio")
    private String paisOrigen;
}
