# CLÍNICA PETSALUD — Backend seguro (Spring Security + JWT)

Desarrollo Backend II (PBY2202) — Experiencia 1, Semana 3: *Integrando seguridad en aplicaciones Backend*.

Backend REST de la clínica veterinaria PetSalud (citas, expedientes médicos, inventario, ventas,
fidelización y notificaciones) asegurado con **Spring Security 6 + JWT (stateless)**, control de acceso
por **roles (CLIENTE, EMPLEADO, GERENTE)** y por **propiedad del dato**.

## Tecnologías
Java 17 · Spring Boot 3.3.6 · Spring Security 6 · JJWT 0.12.6 · Spring Data JPA · MySQL 8 · H2 (tests) · Lombok · Bean Validation

## Estructura
```
src/main/java/com/petsalud/backendpetsalud
├── config/        DataInitializer (usuarios demo), XssSanitizerConfig (escape HTML de entradas)
├── controller/    AuthController (login/registro) + controladores con @PreAuthorize
├── dto/           LoginRequest, RegistroRequest, CrearUsuarioRequest, AuthResponse
├── entity/        Entidades JPA (Usuario, Mascota, Cita, ExpedienteMedico, Producto, Venta, ...)
├── exception/     GlobalExceptionHandler (errores sin stack traces)
├── repository/    Repositorios Spring Data (consultas parametrizadas)
├── security/      SecurityConfig, JwtUtil, JwtAuthenticationFilter, UsuarioDetailsService,
│                  AccesoService (propiedad del dato), LoginAttemptService (anti fuerza bruta),
│                  RestSecurityHandlers (401/403 JSON + auditoría)
└── service/       Lógica de negocio
src/test/java/...  SecurityIntegrationTest (pruebas JWT, roles, IDOR, SQLi, XSS, CSRF)
```

## Ejecutar
1. Tener MySQL corriendo (la BD `petsalud_db` se crea sola).
2. Variables de entorno (opcionales en desarrollo):

| Variable | Uso | Default (solo dev) |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Conexión MySQL | `localhost:3306/petsalud_db`, `root`/`root` |
| `JWT_SECRET` | Clave HMAC Base64 ≥ 256 bits (`openssl rand -base64 32`) | clave de desarrollo |
| `JWT_EXPIRATION_MS` | Vigencia del token | `3600000` (1 h) |
| `APP_SEED` | Crear usuarios de prueba | `true` |

3. `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)

### Pruebas automatizadas (no requieren MySQL, usan H2)
```
./mvnw test
```

## Usuarios de prueba (contraseña `Petsalud2024`)
| Email | Rol |
|---|---|
| gerente@petsalud.cl | GERENTE |
| empleado@petsalud.cl | EMPLEADO |
| cliente@petsalud.cl | CLIENTE |
| cliente2@petsalud.cl | CLIENTE |

## Uso de la API
```bash
# 1. Login -> obtiene token
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"gerente@petsalud.cl","password":"Petsalud2024"}'
# 2. Usar el token
curl http://localhost:8080/api/usuarios -H "Authorization: Bearer <TOKEN>"
```
También se incluye la colección `postman/PetSalud-Seguridad.postman_collection.json`.

## Matriz de permisos
"Dueño" = el CLIENTE autenticado es propietario del recurso (validado por `AccesoService`).

| Recurso | CLIENTE | EMPLEADO | GERENTE |
|---|---|---|---|
| `POST /api/auth/login`, `/api/auth/registro` | público | público | público |
| `GET /api/usuarios` (listar), `POST /api/usuarios` (crear con rol), `DELETE /api/usuarios/{id}` | ✗ | ✗ | ✓ |
| `GET /api/usuarios/{id}` | solo propio | ✓ | ✓ |
| `PUT /api/usuarios/{id}` | solo propio | ✗ | ✓ |
| Mascotas: crear / ver / editar / listar por propietario | solo propias | ✓ | ✓ |
| `DELETE /api/mascotas/{id}` | ✗ | ✗ | ✓ |
| Citas: agendar, ver, cancelar, listar por mascota | solo propias | ✓ | ✓ |
| Citas: cambiar estado, por veterinario, entre fechas | ✗ | ✓ | ✓ |
| Expedientes: crear | ✗ | ✓ | ✓ |
| Expedientes: ver / listar por mascota | solo propios | ✓ | ✓ |
| `DELETE /api/expedientes/{id}` | ✗ | ✗ | ✓ |
| Productos: ver catálogo | ✓ | ✓ | ✓ |
| Productos: crear / editar / stock bajo | ✗ | ✓ | ✓ |
| `DELETE /api/productos/{id}` | ✗ | ✗ | ✓ |
| Ventas: realizar / ver / listar por usuario | solo propias | ✓ | ✓ |
| `GET /api/ventas` (reporte total) | ✗ | ✗ | ✓ |
| Recompensas: ver / listar | solo propias | ✓ | ✓ |
| Recompensas: asignar | ✗ | ✓ | ✓ |
| `DELETE /api/recompensas/{id}` | ✗ | ✗ | ✓ |
| Notificaciones: ver / listar / marcar leída | solo propias | ✓ | ✓ |
| Notificaciones: enviar | ✗ | ✓ | ✓ |
| `DELETE /api/notificaciones/{id}` | solo propias | ✗ | ✓ |

## Medidas de seguridad implementadas
- **JWT HS256** firmado, con `iss`, `iat`, `exp` (1 h) y `jti`; se valida firma, emisor y expiración en cada petición.
- **BCrypt (costo 12)** para contraseñas; nunca se devuelven en JSON (`WRITE_ONLY`).
- **Sesión STATELESS**, sin cookies → CSRF no aplicable (justificado en `SecurityConfig`).
- **Doble barrera de autorización**: reglas por URL + `@PreAuthorize` por método, con verificación de propiedad (anti‑IDOR).
- **Auto‑registro siempre como CLIENTE** (sin escalamiento de privilegios).
- **SQL Injection**: solo consultas JPA parametrizadas (sin concatenación de SQL).
- **XSS**: escape HTML de toda entrada de texto + `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`.
- **Monitoreo**: logger `SECURITY_AUDIT` (login OK/fallido, bloqueos, token inválido/expirado, 401/403) en `logs/petsalud.log`.
- **Anti fuerza bruta**: 5 intentos fallidos → bloqueo 15 min (HTTP 429).
- **Validación de entradas** (Bean Validation) y política de contraseña mínima.
- **Errores controlados** sin stack traces; secretos y credenciales por variables de entorno.
