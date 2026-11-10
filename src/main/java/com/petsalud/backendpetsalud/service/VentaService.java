package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Venta;

import java.util.List;
import java.util.Optional;

public interface VentaService {
    Venta realizarVenta(Long usuarioId, List<Long> productosIds);
    Optional<Venta> obtenerVentaPorId(Long id);
    List<Venta> listarVentasPorUsuario(Long usuarioId);
    List<Venta> listarTodasLasVentas();
}
