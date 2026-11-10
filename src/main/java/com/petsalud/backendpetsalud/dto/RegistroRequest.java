package com.petsalud.backendpetsalud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de auto-registro de un CLIENTE.
 * No incluye "rol": un usuario NO puede elegir su propio rol (evita escalamiento de privilegios).
 */
public record RegistroRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                 message = "debe contener al menos una letra y un número")
        String password,
        @NotBlank @Size(max = 60) String nombre,
        @Size(max = 60) String apellido,
        @Pattern(regexp = "^[+0-9 ]{0,15}$", message = "teléfono inválido") String telefono,
        @Size(max = 150) String direccion) {
}
