package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioId(Long usuarioId);
    List<Notificacion> findByLeidaFalseAndUsuarioId(Long usuarioId);
}
