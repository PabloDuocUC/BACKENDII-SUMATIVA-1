package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Notificacion;

import java.util.List;
import java.util.Optional;

public interface NotificacionService {
    Notificacion enviarNotificacion(Long usuarioId, Notificacion notificacion);
    Optional<Notificacion> obtenerNotificacionPorId(Long id);
    List<Notificacion> listarNotificacionesPorUsuario(Long usuarioId);
    List<Notificacion> listarNotificacionesNoLeidas(Long usuarioId);
    void marcarComoLeida(Long id);
    void eliminarNotificacion(Long id);
}
