package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;

import java.util.List;
import java.util.Optional;

public interface ExpedienteMedicoService {
    ExpedienteMedico agregarRegistro(Long mascotaId, Long veterinarioId, ExpedienteMedico registro);
    Optional<ExpedienteMedico> obtenerRegistroPorId(Long id);
    List<ExpedienteMedico> listarRegistrosPorMascota(Long mascotaId);
    void eliminarRegistro(Long id);
}
