package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoService {
    Producto agregarProducto(Producto producto);
    Optional<Producto> obtenerProductoPorId(Long id);
    Producto actualizarProducto(Long id, Producto producto);
    void eliminarProducto(Long id);
    List<Producto> listarProductos();
    List<Producto> listarProductosPorCategoria(String categoria);
    List<Producto> listarProductosConStockBajo(Integer cantidad);
}
