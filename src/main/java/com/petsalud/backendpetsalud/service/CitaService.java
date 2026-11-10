package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaService {
    Cita agendarCita(Long mascotaId, Long veterinarioId, LocalDateTime fechaHora, String motivo);
    Optional<Cita> obtenerCitaPorId(Long id);
    Cita actualizarEstadoCita(Long id, String nuevoEstado);
    void cancelarCita(Long id);
    List<Cita> listarCitasPorMascota(Long mascotaId);
    List<Cita> listarCitasPorVeterinario(Long veterinarioId);
    List<Cita> listarCitasEntreFechas(LocalDateTime inicio, LocalDateTime fin);
}
