package com.petsalud.backendpetsalud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de seguridad de CLÍNICA PETSALUD (base H2 en memoria, perfil "test").
 * Usuarios de prueba creados por DataInitializer con contraseña "Petsalud2024".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String PASS = "Petsalud2024";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired MascotaRepository mascotaRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}") String secret;

    // ---------- utilidades ----------
    private String login(String email, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = mapper.readTree(r.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private String bearer(String email) throws Exception {
        return "Bearer " + login(email, PASS);
    }

    private Long idDe(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow().getId();
    }

    private Mascota mascotaDe(String email, String nombre) {
        Usuario dueno = usuarioRepository.findByEmail(email).orElseThrow();
        return mascotaRepository.save(Mascota.builder().nombre(nombre).especie("Perro").propietario(dueno).build());
    }

    // =====================================================================
    @Nested
    @DisplayName("1. Autenticación JWT")
    class Autenticacion {

        @Test
        @DisplayName("Login correcto entrega un JWT con 3 partes y el rol")
        void loginCorrecto() throws Exception {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"gerente@petsalud.cl\",\"password\":\"" + PASS + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    .andExpect(jsonPath("$.rol").value("GERENTE"))
                    .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.matchesPattern("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$")));
        }

        @Test
        @DisplayName("Contraseña incorrecta -> 401 con mensaje genérico")
        void loginIncorrecto() throws Exception {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"empleado@petsalud.cl\",\"password\":\"malaClave1\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
        }

        @Test
        @DisplayName("Sin token -> 401")
        void sinToken() throws Exception {
            mvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token alterado (firma inválida) -> 401")
        void tokenAlterado() throws Exception {
            String token = login("cliente@petsalud.cl", PASS);
            String alterado = token.substring(0, token.length() - 4) + "abcd";
            mvc.perform(get("/api/productos").header("Authorization", "Bearer " + alterado))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token con rol falsificado firmado con otra clave -> 401")
        void tokenOtraClave() throws Exception {
            String falso = Jwts.builder().issuer("petsalud-backend").subject("gerente@petsalud.cl")
                    .claim("rol", "GERENTE").expiration(new Date(System.currentTimeMillis() + 60000))
                    .signWith(Keys.hmacShaKeyFor("clave-del-atacante-clave-del-atacante-123".getBytes()))
                    .compact();
            mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + falso))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token expirado -> 401")
        void tokenExpirado() throws Exception {
            String expirado = Jwts.builder().issuer("petsalud-backend").subject("gerente@petsalud.cl")
                    .claim("rol", "GERENTE")
                    .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                    .expiration(new Date(System.currentTimeMillis() - 3600000))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                    .compact();
            mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + expirado))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Contraseñas guardadas con BCrypt, nunca en texto plano ni en la respuesta")
        void passwordConBcrypt() throws Exception {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nuevo@correo.cl\",\"password\":\"Segura123\",\"nombre\":\"Ana\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.password").doesNotExist());
            Usuario u = usuarioRepository.findByEmail("nuevo@correo.cl").orElseThrow();
            assertThat(u.getPassword()).startsWith("$2a$").isNotEqualTo("Segura123");
            assertThat(passwordEncoder.matches("Segura123", u.getPassword())).isTrue();
        }

        @Test
        @DisplayName("Auto-registro no permite elegir rol GERENTE (escalamiento de privilegios)")
        void registroNoEscalaRol() throws Exception {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"hacker@correo.cl\",\"password\":\"Segura123\",\"nombre\":\"H\",\"rol\":\"GERENTE\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rol").value("CLIENTE"));
        }

        @Test
        @DisplayName("Contraseña débil es rechazada -> 400")
        void passwordDebil() throws Exception {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"debil@correo.cl\",\"password\":\"123\",\"nombre\":\"D\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Fuerza bruta: tras 5 intentos fallidos la cuenta se bloquea (429)")
        void bloqueoFuerzaBruta() throws Exception {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"victima@correo.cl\",\"password\":\"Segura123\",\"nombre\":\"V\"}"))
                    .andExpect(status().isCreated());
            for (int i = 0; i < 5; i++) {
                mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"victima@correo.cl\",\"password\":\"Intento" + i + "x\"}"))
                        .andExpect(status().isUnauthorized());
            }
            // Incluso con la contraseña correcta queda bloqueada
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"victima@correo.cl\",\"password\":\"Segura123\"}"))
                    .andExpect(status().isTooManyRequests());
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("2. Autorización por roles")
    class Roles {

        @Test
        @DisplayName("Listar usuarios: CLIENTE 403, EMPLEADO 403, GERENTE 200")
        void listarUsuarios() throws Exception {
            mvc.perform(get("/api/usuarios").header("Authorization", bearer("cliente@petsalud.cl")))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/usuarios").header("Authorization", bearer("empleado@petsalud.cl")))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/usuarios").header("Authorization", bearer("gerente@petsalud.cl")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].password").doesNotExist());
        }

        @Test
        @DisplayName("Productos: CLIENTE puede ver, no crear; EMPLEADO crea; solo GERENTE elimina")
        void productos() throws Exception {
            String body = "{\"nombre\":\"Collar\",\"precio\":5990,\"stock\":10,\"categoria\":\"Accesorios\"}";
            mvc.perform(get("/api/productos").header("Authorization", bearer("cliente@petsalud.cl")))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/productos").header("Authorization", bearer("cliente@petsalud.cl"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
            MvcResult r = mvc.perform(post("/api/productos").header("Authorization", bearer("empleado@petsalud.cl"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andReturn();
            long id = mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
            mvc.perform(delete("/api/productos/" + id).header("Authorization", bearer("empleado@petsalud.cl")))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/productos/" + id).header("Authorization", bearer("gerente@petsalud.cl")))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Reporte de todas las ventas: solo GERENTE")
        void reporteVentas() throws Exception {
            mvc.perform(get("/api/ventas").header("Authorization", bearer("empleado@petsalud.cl")))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/ventas").header("Authorization", bearer("gerente@petsalud.cl")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Expedientes médicos: CLIENTE no puede crear registros clínicos")
        void expedientes() throws Exception {
            Mascota m = mascotaDe("cliente@petsalud.cl", "Firulais");
            mvc.perform(post("/api/expedientes").header("Authorization", bearer("cliente@petsalud.cl"))
                            .param("mascotaId", m.getId().toString())
                            .param("veterinarioId", idDe("empleado@petsalud.cl").toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"diagnostico\":\"Sano\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/expedientes").header("Authorization", bearer("empleado@petsalud.cl"))
                            .param("mascotaId", m.getId().toString())
                            .param("veterinarioId", idDe("empleado@petsalud.cl").toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"diagnostico\":\"Sano\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("IDOR: un CLIENTE no puede ver la mascota ni las citas de OTRO cliente")
        void propiedadDelDato() throws Exception {
            Mascota propia = mascotaDe("cliente@petsalud.cl", "Propia");
            Mascota ajena = mascotaDe("cliente2@petsalud.cl", "Ajena");
            String cliente = bearer("cliente@petsalud.cl");

            mvc.perform(get("/api/mascotas/" + propia.getId()).header("Authorization", cliente))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/mascotas/" + ajena.getId()).header("Authorization", cliente))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/citas/mascota/" + ajena.getId()).header("Authorization", cliente))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/expedientes/mascota/" + ajena.getId()).header("Authorization", cliente))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/usuarios/" + idDe("cliente2@petsalud.cl")).header("Authorization", cliente))
                    .andExpect(status().isForbidden());
            // El empleado sí puede atender a cualquier mascota
            mvc.perform(get("/api/mascotas/" + ajena.getId()).header("Authorization", bearer("empleado@petsalud.cl")))
                    .andExpect(status().isOk());
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("3. Amenazas comunes: SQL Injection, XSS, CSRF")
    class Amenazas {

        @Test
        @DisplayName("SQL Injection en login: ' OR '1'='1 no permite entrar (401)")
        void sqlInjectionLogin() throws Exception {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"' OR '1'='1' -- \",\"password\":\"' OR '1'='1\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("SQL Injection en parámetro de búsqueda: se trata como texto literal, no como SQL")
        void sqlInjectionBusqueda() throws Exception {
            productoRepository.save(Producto.builder().nombre("Alimento").precio(BigDecimal.TEN)
                    .stock(5).categoria("Alimentos").build());
            mvc.perform(get("/api/productos/categoria")
                            .param("categoria", "Alimentos' OR '1'='1")
                            .header("Authorization", bearer("cliente@petsalud.cl")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
            // La tabla sigue existiendo después de un intento de DROP
            mvc.perform(get("/api/productos/categoria")
                            .param("categoria", "x'; DROP TABLE productos; --")
                            .header("Authorization", bearer("cliente@petsalud.cl")))
                    .andExpect(status().isOk());
            assertThat(productoRepository.count()).isGreaterThan(0);
        }

        @Test
        @DisplayName("XSS almacenado: <script> se guarda escapado y no ejecutable")
        void xssAlmacenado() throws Exception {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"xss@correo.cl\",\"password\":\"Segura123\","
                                    + "\"nombre\":\"<script>alert('xss')</script>\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
            assertThat(usuarioRepository.findByEmail("xss@correo.cl").orElseThrow().getNombre())
                    .doesNotContain("<script>");
        }

        @Test
        @DisplayName("Headers de seguridad presentes (CSP, X-Frame-Options, nosniff)")
        void headersSeguridad() throws Exception {
            mvc.perform(get("/api/productos").header("Authorization", bearer("cliente@petsalud.cl")))
                    .andExpect(header().string("Content-Security-Policy",
                            org.hamcrest.Matchers.containsString("default-src 'none'")))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("CSRF: API stateless, sin cookies de sesión; sin el header Bearer la petición es rechazada")
        void csrf() throws Exception {
            // Un sitio malicioso solo puede hacer que el navegador envíe cookies, nunca el header Authorization.
            MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"cliente@petsalud.cl\",\"password\":\"" + PASS + "\"}"))
                    .andExpect(status().isOk()).andReturn();
            assertThat(r.getResponse().getCookies()).isEmpty();          // no se emite JSESSIONID
            assertThat(r.getResponse().getHeader("Set-Cookie")).isNull();
            // Petición "forjada" sin token (lo que lograría un ataque CSRF) -> 401
            mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"forjado\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Errores no exponen stack traces ni detalles internos")
        void sinFugaDeInformacion() throws Exception {
            mvc.perform(get("/api/citas/entre-fechas").param("inicio", "no-es-fecha").param("fin", "x")
                            .header("Authorization", bearer("empleado@petsalud.cl")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(jsonPath("$.error").value("Petición mal formada"));
        }
    }
}
