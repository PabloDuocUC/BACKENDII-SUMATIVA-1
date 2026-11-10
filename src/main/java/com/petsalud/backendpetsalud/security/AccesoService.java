package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Reglas de PROPIEDAD del dato, usadas desde @PreAuthorize como @acceso.metodo(...).
 * Evita el IDOR (Insecure Direct Object Reference): un CLIENTE autenticado no puede leer la
 * mascota, cita, expediente, venta, etc. de OTRO cliente solo cambiando el id en la URL.
 */
@Component("acceso")
@Transactional(readOnly = true)
public class AccesoService {

    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final ExpedienteMedicoRepository expedienteRepository;
    private final VentaRepository ventaRepository;
    private final RecompensaRepository recompensaRepository;
    private final NotificacionRepository notificacionRepository;

    public AccesoService(MascotaRepository mascotaRepository, CitaRepository citaRepository,
                         ExpedienteMedicoRepository expedienteRepository, VentaRepository ventaRepository,
                         RecompensaRepository recompensaRepository, NotificacionRepository notificacionRepository) {
        this.mascotaRepository = mascotaRepository;
        this.citaRepository = citaRepository;
        this.expedienteRepository = expedienteRepository;
        this.ventaRepository = ventaRepository;
        this.recompensaRepository = recompensaRepository;
        this.notificacionRepository = notificacionRepository;
    }

    /** Id del usuario autenticado, o null si no hay sesión válida. */
    public Long usuarioActualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioDetails ud) {
            return ud.getId();
        }
        return null;
    }

    public boolean esUsuario(Long usuarioId) {
        Long actual = usuarioActualId();
        return actual != null && Objects.equals(actual, usuarioId);
    }

    public boolean esDuenoMascota(Long mascotaId) {
        return mascotaRepository.findById(mascotaId)
                .map(m -> m.getPropietario() != null && esUsuario(m.getPropietario().getId()))
                .orElse(false);
    }

    public boolean esDuenoCita(Long citaId) {
        return citaRepository.findById(citaId)
                .map(c -> c.getMascota() != null && c.getMascota().getPropietario() != null
                        && esUsuario(c.getMascota().getPropietario().getId()))
                .orElse(false);
    }

    public boolean esDuenoExpediente(Long expedienteId) {
        return expedienteRepository.findById(expedienteId)
                .map(e -> e.getMascota() != null && e.getMascota().getPropietario() != null
                        && esUsuario(e.getMascota().getPropietario().getId()))
                .orElse(false);
    }

    public boolean esDuenoVenta(Long ventaId) {
        return ventaRepository.findById(ventaId)
                .map(v -> v.getUsuario() != null && esUsuario(v.getUsuario().getId()))
                .orElse(false);
    }

    public boolean esDuenoRecompensa(Long recompensaId) {
        return recompensaRepository.findById(recompensaId)
                .map(r -> r.getUsuario() != null && esUsuario(r.getUsuario().getId()))
                .orElse(false);
    }

    public boolean esDuenoNotificacion(Long notificacionId) {
        return notificacionRepository.findById(notificacionId)
                .map(n -> n.getUsuario() != null && esUsuario(n.getUsuario().getId()))
                .orElse(false);
    }
}
