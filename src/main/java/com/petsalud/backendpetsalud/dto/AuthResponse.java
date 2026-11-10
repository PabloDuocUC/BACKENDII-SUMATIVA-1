package com.petsalud.backendpetsalud.dto;

/** Respuesta del login: token JWT y datos mínimos del usuario. */
public record AuthResponse(String token, String tipo, long expiraEnSegundos, String email, String rol) {
}
