package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import com.petsalud.backendpetsalud.service.ExpedienteMedicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/expedientes")
public class ExpedienteMedicoController {

    @Autowired
    private ExpedienteMedicoService expedienteMedicoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    public ResponseEntity<ExpedienteMedico> agregarRegistro(
            @RequestParam Long mascotaId,
            @RequestParam Long veterinarioId,
            @RequestBody ExpedienteMedico registro) {
        registro.setId(null); // evita sobrescribir registros existentes (mass assignment)
        ExpedienteMedico nuevoRegistro = expedienteMedicoService.agregarRegistro(mascotaId, veterinarioId, registro);
        return ResponseEntity.ok(nuevoRegistro);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @acceso.esDuenoExpediente(#id)")
    public ResponseEntity<ExpedienteMedico> obtenerRegistroPorId(@PathVariable Long id) {
        return expedienteMedicoService.obtenerRegistroPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mascota/{mascotaId}")
    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @acceso.esDuenoMascota(#mascotaId)")
    public ResponseEntity<List<ExpedienteMedico>> listarRegistrosPorMascota(@PathVariable Long mascotaId) {
        List<ExpedienteMedico> registros = expedienteMedicoService.listarRegistrosPorMascota(mascotaId);
        return ResponseEntity.ok(registros);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> eliminarRegistro(@PathVariable Long id) {
        expedienteMedicoService.eliminarRegistro(id);
        return ResponseEntity.noContent().build();
    }
}
