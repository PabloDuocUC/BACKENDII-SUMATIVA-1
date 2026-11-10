package com.petsalud.backendpetsalud.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitoreo básico de actividad sospechosa: protección contra fuerza bruta.
 * Tras N intentos fallidos de login para un mismo email, la cuenta queda bloqueada
 * temporalmente (por defecto 5 intentos / 15 minutos). Cada evento queda en el log SECURITY_AUDIT.
 */
@Service
public class LoginAttemptService {

    private static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final int maxIntentos;
    private final long bloqueoSegundos;
    private final Map<String, Intento> intentos = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${security.login.max-intentos:5}") int maxIntentos,
                               @Value("${security.login.bloqueo-segundos:900}") long bloqueoSegundos) {
        this.maxIntentos = maxIntentos;
        this.bloqueoSegundos = bloqueoSegundos;
    }

    public boolean estaBloqueado(String email) {
        Intento i = intentos.get(clave(email));
        if (i == null || i.bloqueadoHasta == null) {
            return false;
        }
        if (Instant.now().isAfter(i.bloqueadoHasta)) {
            intentos.remove(clave(email));
            return false;
        }
        return true;
    }

    public void loginFallido(String email, String ip) {
        Intento i = intentos.computeIfAbsent(clave(email), k -> new Intento());
        synchronized (i) {
            i.fallidos++;
            AUDIT.warn("LOGIN_FALLIDO email={} ip={} intento={}", email, ip, i.fallidos);
            if (i.fallidos >= maxIntentos) {
                i.bloqueadoHasta = Instant.now().plusSeconds(bloqueoSegundos);
                AUDIT.error("CUENTA_BLOQUEADA email={} ip={} hasta={}", email, ip, i.bloqueadoHasta);
            }
        }
    }

    public void loginExitoso(String email, String ip) {
        intentos.remove(clave(email));
        AUDIT.info("LOGIN_OK email={} ip={}", email, ip);
    }

    private String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static class Intento {
        int fallidos;
        Instant bloqueadoHasta;
    }
}
