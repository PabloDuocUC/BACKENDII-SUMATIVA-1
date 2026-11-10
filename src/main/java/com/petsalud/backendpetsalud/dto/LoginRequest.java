package com.petsalud.backendpetsalud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credenciales de inicio de sesión. */
public record LoginRequest(
        @NotBlank @Size(max = 100) String email,
        @NotBlank @Size(max = 100) String password) {
}
