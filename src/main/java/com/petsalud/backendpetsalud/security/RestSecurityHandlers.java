package com.petsalud.backendpetsalud.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuestas JSON uniformes para 401 (no autenticado) y 403 (sin permiso),
 * sin exponer detalles internos, y registro de cada intento en el log de auditoría.
 */
@Component
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        AUDIT.warn("ACCESO_NO_AUTENTICADO ip={} metodo={} uri={}",
                request.getRemoteAddr(), request.getMethod(), request.getRequestURI());
        escribir(response, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado o token inválido/expirado", request);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AUDIT.warn("ACCESO_DENEGADO usuario={} ip={} metodo={} uri={}",
                auth != null ? auth.getName() : "anonimo", request.getRemoteAddr(),
                request.getMethod(), request.getRequestURI());
        escribir(response, HttpServletResponse.SC_FORBIDDEN, "No tiene permisos para este recurso", request);
    }

    private void escribir(HttpServletResponse response, int status, String mensaje,
                          HttpServletRequest request) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", mensaje);
        body.put("path", request.getRequestURI());
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), body);
    }
}
