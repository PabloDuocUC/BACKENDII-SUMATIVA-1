package com.petsalud.backendpetsalud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Genera y valida tokens JWT firmados con HMAC-SHA256 (HS256).
 * - La clave secreta se lee desde configuración / variable de entorno (nunca hardcodeada en producción).
 * - El token incluye: sub (email), rol, iss, iat, exp y jti (id único).
 * - Se valida firma, emisor y expiración. Un token alterado o vencido es rechazado.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiracionMs;
    private final String emisor;

    public JwtUtil(@Value("${jwt.secret}") String secretBase64,
                   @Value("${jwt.expiration-ms:3600000}") long expiracionMs,
                   @Value("${jwt.issuer:petsalud-backend}") String emisor) {
        byte[] bytes = Decoders.BASE64.decode(secretBase64);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret debe tener al menos 256 bits (32 bytes) en Base64");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiracionMs = expiracionMs;
        this.emisor = emisor;
    }

    public String generarToken(UserDetails usuario) {
        String rol = usuario.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        Date ahora = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(emisor)
                .subject(usuario.getUsername())
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Valida firma, emisor y expiración. Lanza JwtException si el token no es válido. */
    public Claims validarYObtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(emisor)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esValido(String token) {
        try {
            validarYObtenerClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String obtenerEmail(String token) {
        return validarYObtenerClaims(token).getSubject();
    }

    public long getExpiracionSegundos() {
        return expiracionMs / 1000;
    }
}
