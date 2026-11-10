package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Recompensa;

import java.util.List;
import java.util.Optional;

public interface RecompensaService {
    Recompensa agregarRecompensa(Long usuarioId, Recompensa recompensa);
    Optional<Recompensa> obtenerRecompensaPorId(Long id);
    List<Recompensa> listarRecompensasPorUsuario(Long usuarioId);
    void eliminarRecompensa(Long id);
}
