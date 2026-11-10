package com.petsalud.backendpetsalud.config;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea un usuario por rol para pruebas (solo si no existen).
 * Se desactiva en producción con app.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final String passwordDemo;

    public DataInitializer(UsuarioRepository repo, PasswordEncoder encoder,
                           @Value("${app.seed.password:Petsalud2024}") String passwordDemo) {
        this.repo = repo;
        this.encoder = encoder;
        this.passwordDemo = passwordDemo;
    }

    @Override
    public void run(String... args) {
        crear("gerente@petsalud.cl", "Gerencia", Rol.GERENTE);
        crear("empleado@petsalud.cl", "Veterinario", Rol.EMPLEADO);
        crear("cliente@petsalud.cl", "Cliente", Rol.CLIENTE);
        crear("cliente2@petsalud.cl", "Cliente Dos", Rol.CLIENTE);
    }

    private void crear(String email, String nombre, Rol rol) {
        if (repo.findByEmail(email).isEmpty()) {
            repo.save(Usuario.builder()
                    .email(email)
                    .password(encoder.encode(passwordDemo))
                    .nombre(nombre)
                    .rol(rol)
                    .build());
            log.info("Usuario de prueba creado: {} ({})", email, rol);
        }
    }
}
