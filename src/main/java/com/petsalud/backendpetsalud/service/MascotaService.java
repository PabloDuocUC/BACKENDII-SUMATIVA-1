package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Mascota;

import java.util.List;
import java.util.Optional;

public interface MascotaService {
    Mascota registrarMascota(Mascota mascota, Long propietarioId);
    Optional<Mascota> obtenerMascotaPorId(Long id);
    Mascota actualizarMascota(Long id, Mascota mascotaActualizada);
    void eliminarMascota(Long id);
    List<Mascota> listarMascotasPorPropietario(Long propietarioId);
}
