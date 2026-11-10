package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta de usuarios con rol explícito. Solo puede usarlo un GERENTE. */
public record CrearUsuarioRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                 message = "debe contener al menos una letra y un número")
        String password,
        @NotBlank @Size(max = 60) String nombre,
        @Size(max = 60) String apellido,
        @NotNull Rol rol) {
}
